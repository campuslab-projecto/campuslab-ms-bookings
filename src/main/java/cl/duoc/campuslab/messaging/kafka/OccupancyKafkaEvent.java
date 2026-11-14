
package cl.duoc.campuslab.messaging.kafka;

import java.time.LocalDateTime;

public record OccupancyKafkaEvent(
        String eventId,
        String eventType,
        Long bookingId,
        String resourceId,
        String status,
        LocalDateTime timestamp
) {
}
