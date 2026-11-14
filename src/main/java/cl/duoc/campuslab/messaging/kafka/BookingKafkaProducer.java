
package cl.duoc.campuslab.messaging.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.support.SendResult;

@Service
@ConditionalOnProperty(
        name = "campuslab.kafka.enabled",
        havingValue = "true"
)
public class BookingKafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String bookingTopic;

    public BookingKafkaProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${campuslab.kafka.topics.booking-events}")
            String bookingTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.bookingTopic = bookingTopic;
    }

    public void publish(BookingKafkaEvent event) {

        try {
            String json = objectMapper.writeValueAsString(event);

            String key = String.valueOf(event.bookingId());

            CompletableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(bookingTopic, key, json);

            future.whenComplete((result, exception) -> {

                if (exception != null) {
                    System.err.println(
                            "ERROR KAFKA: No se pudo publicar el evento "
                                    + event.eventId()
                                    + " - "
                                    + exception.getMessage()
                    );

                    return;
                }

                RecordMetadata metadata =
                        result.getRecordMetadata();

                System.out.println(
                        "EVENTO KAFKA PUBLICADO: "
                                + event.eventType()
                                + " | Reserva: "
                                + event.bookingId()
                                + " | Partición: "
                                + metadata.partition()
                                + " | Offset: "
                                + metadata.offset()
                );
            });

        } catch (JsonProcessingException exception) {

            System.err.println(
                    "ERROR KAFKA: No se pudo convertir el evento a JSON: "
                            + exception.getMessage()
            );
        }
    }
}
