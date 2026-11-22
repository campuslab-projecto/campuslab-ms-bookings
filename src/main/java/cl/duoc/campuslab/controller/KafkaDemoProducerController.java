
package cl.duoc.campuslab.controller;

import cl.duoc.campuslab.messaging.kafka.BookingKafkaEvent;
import cl.duoc.campuslab.messaging.kafka.OccupancyKafkaEvent;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.kafka.clients.producer.RecordMetadata;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/internal/kafka/demo")
@ConditionalOnProperty(
        name = "campuslab.kafka.demo.enabled",
        havingValue = "true"
)
public class KafkaDemoProducerController {

    private static final String BOOKING_TOPIC =
            "campuslab.demo.ep5.booking.events";

    private static final String OCCUPANCY_TOPIC =
            "campuslab.demo.ep5.occupancy.events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String internalKey;

    public KafkaDemoProducerController(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${CAMPUSLAB_KAFKA_DEMO_API_KEY}")
            String internalKey
    ) {
        if (internalKey.isBlank()) {
            throw new IllegalStateException(
                    "La clave interna Kafka Demo es obligatoria"
            );
        }

        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.internalKey = internalKey;
    }

    public record DemoEventRequest(
            String eventType,
            Long bookingId,
            String resourceId
    ) {}

    @PostMapping("/events")
    public ResponseEntity<?> publishEvent(
            @RequestHeader(
                    value = "X-Internal-Demo-Key",
                    required = false
            )
            String suppliedKey,
            @RequestBody DemoEventRequest request
    ) {

        validateInternalKey(suppliedKey);

        if (request == null ||
                request.bookingId() == null ||
                request.bookingId() <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe indicar un ID de reserva positivo"
            );
        }

        String eventType = request.eventType();

        if (eventType == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe indicar el tipo de evento"
            );
        }

        String eventId = UUID.randomUUID().toString();
        LocalDateTime timestamp = LocalDateTime.now();

        String topic;
        String key;
        Object event;

        switch (eventType) {

            case "BOOKING_CREATED" -> {

                topic = BOOKING_TOPIC;
                key = request.bookingId().toString();

                event = new BookingKafkaEvent(
                        eventId,
                        eventType,
                        request.bookingId(),
                        request.resourceId(),
                        "SOLICITADA",
                        timestamp
                );
            }

            case "BOOKING_STATUS_CHANGED" -> {

                topic = BOOKING_TOPIC;
                key = request.bookingId().toString();

                event = new BookingKafkaEvent(
                        eventId,
                        eventType,
                        request.bookingId(),
                        request.resourceId(),
                        "APROBADA",
                        timestamp
                );
            }

            case "OCCUPANCY_STARTED",
                 "OCCUPANCY_ENDED" -> {

                String resourceId =
                        request.resourceId() == null
                                ? ""
                                : request.resourceId().trim();

                if (resourceId.isBlank()) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Debe indicar el recurso"
                    );
                }

                topic = OCCUPANCY_TOPIC;
                key = resourceId;

                String status =
                        eventType.equals("OCCUPANCY_STARTED")
                                ? "EN_USO"
                                : "DEVUELTA";

                event = new OccupancyKafkaEvent(
                        eventId,
                        eventType,
                        request.bookingId(),
                        resourceId,
                        status,
                        timestamp
                );
            }

            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tipo de evento no permitido"
            );
        }

        try {

            String json =
                    objectMapper.writeValueAsString(event);

            SendResult<String, String> result =
                    kafkaTemplate.send(topic, key, json)
                            .get(10, TimeUnit.SECONDS);

            RecordMetadata metadata =
                    result.getRecordMetadata();

            return ResponseEntity.ok(
                    Map.of(
                            "published", true,
                            "eventId", eventId,
                            "eventType", eventType,
                            "topic", topic,
                            "partition", metadata.partition(),
                            "offset", metadata.offset(),
                            "payload", event
                    )
            );

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Publicación interrumpida",
                    exception
            );

        } catch (Exception exception) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo confirmar la publicación Kafka",
                    exception
            );
        }
    }

    private void validateInternalKey(String suppliedKey) {

        if (suppliedKey == null ||
                !MessageDigest.isEqual(
                        internalKey.getBytes(StandardCharsets.UTF_8),
                        suppliedKey.getBytes(StandardCharsets.UTF_8)
                )) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Acceso interno no autorizado"
            );
        }
    }
}
