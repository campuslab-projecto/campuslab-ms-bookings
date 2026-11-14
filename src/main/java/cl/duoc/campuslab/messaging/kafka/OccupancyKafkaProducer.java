
package cl.duoc.campuslab.messaging.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.kafka.clients.producer.RecordMetadata;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@ConditionalOnProperty(
        name = "campuslab.kafka.enabled",
        havingValue = "true"
)
public class OccupancyKafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String occupancyTopic;

    public OccupancyKafkaProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${campuslab.kafka.topics.occupancy-events}")
            String occupancyTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.occupancyTopic = occupancyTopic;
    }

    public void publish(OccupancyKafkaEvent event) {

        try {

            String json = objectMapper.writeValueAsString(event);

            // Usamos el recurso como clave para mantener
            // sus eventos en la misma particion.
            String key = event.resourceId();

            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(occupancyTopic, key, json);

            future.whenComplete((result, exception) -> {

                if (exception != null) {

                    System.err.println(
                            "ERROR KAFKA OCUPACION: "
                                    + exception.getMessage()
                    );

                    return;
                }

                RecordMetadata metadata =
                        result.getRecordMetadata();

                System.out.println(
                        "EVENTO OCUPACION PUBLICADO: "
                                + event.eventType()
                                + " | Recurso: "
                                + event.resourceId()
                                + " | Reserva: "
                                + event.bookingId()
                                + " | Particion: "
                                + metadata.partition()
                                + " | Offset: "
                                + metadata.offset()
                );
            });

        } catch (JsonProcessingException exception) {

            System.err.println(
                    "ERROR AL CONVERTIR EVENTO DE OCUPACION: "
                            + exception.getMessage()
            );
        }
    }
}
