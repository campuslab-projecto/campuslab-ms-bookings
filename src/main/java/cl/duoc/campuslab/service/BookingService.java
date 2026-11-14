
package cl.duoc.campuslab.service;

import cl.duoc.campuslab.messaging.dto.BookingEventMessage;
import cl.duoc.campuslab.messaging.kafka.BookingKafkaEvent;
import cl.duoc.campuslab.messaging.kafka.BookingKafkaProducer;
import cl.duoc.campuslab.messaging.kafka.OccupancyKafkaEvent;
import cl.duoc.campuslab.messaging.kafka.OccupancyKafkaProducer;
import cl.duoc.campuslab.messaging.producer.BookingMessageProducer;
import cl.duoc.campuslab.model.Booking;
import cl.duoc.campuslab.model.BookingStatus;
import cl.duoc.campuslab.repository.BookingRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private static final Logger log =
            LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final BookingMessageProducer bookingMessageProducer;

    private final ObjectProvider<BookingKafkaProducer>
            kafkaProducerProvider;

    private final ObjectProvider<OccupancyKafkaProducer>
            occupancyProducerProvider;

    public BookingService(
            BookingRepository bookingRepository,
            BookingMessageProducer bookingMessageProducer,
            ObjectProvider<BookingKafkaProducer> kafkaProducerProvider,
            ObjectProvider<OccupancyKafkaProducer> occupancyProducerProvider
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingMessageProducer = bookingMessageProducer;
        this.kafkaProducerProvider = kafkaProducerProvider;
        this.occupancyProducerProvider = occupancyProducerProvider;
    }

    // ============================================
    // 1. CREAR RESERVA
    // ============================================

    public Booking createBooking(Booking booking) {

        booking.setStatus(BookingStatus.SOLICITADA);
        booking.setBookingDate(LocalDateTime.now());

        Booking savedBooking = bookingRepository.save(booking);

        // RabbitMQ: notificacion de creacion.
        BookingEventMessage message = createEventMessage(
                savedBooking,
                "BOOKING_CREATED",
                "Reserva creada correctamente"
        );

        bookingMessageProducer.sendEmailMessage(message);

        // Kafka: auditoria y reportes.
        publishKafkaEvent(
                savedBooking,
                "BOOKING_CREATED"
        );

        return savedBooking;
    }

    // ============================================
    // 2. CONSULTAR RESERVA POR ID
    // ============================================

    public Booking getBookingById(Long id) {

        return bookingRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Reserva no encontrada"
                        )
                );
    }

    // ============================================
    // 3. ACTUALIZAR ESTADO DE RESERVA
    // ============================================

    public Booking updateStatus(
            Long id,
            BookingStatus newStatus
    ) {

        Booking booking = getBookingById(id);

        if (newStatus == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe informar un estado valido"
            );
        }

        // Validar antes de modificar y guardar.
        validateStatusTransition(
                booking.getStatus(),
                newStatus
        );

        booking.setStatus(newStatus);

        Booking savedBooking = bookingRepository.save(booking);

        // RabbitMQ: mantener notificaciones.
        publishStatusEvent(savedBooking);

        // Kafka: historial de cambios.
        publishKafkaEvent(
                savedBooking,
                "BOOKING_STATUS_CHANGED"
        );

        // Kafka: cambios de ocupacion.
        // Solo publicar al entrar o salir de EN_USO.
        if (newStatus == BookingStatus.EN_USO) {

            publishOccupancyEvent(
                    savedBooking,
                    "OCCUPANCY_STARTED"
            );

        } else if (newStatus == BookingStatus.DEVUELTA) {

            publishOccupancyEvent(
                    savedBooking,
                    "OCCUPANCY_ENDED"
            );
        }

        return savedBooking;
    }

    // ============================================
    // 4. LISTAR RESERVAS
    // ============================================

    public List<Booking> getAllBookings(
            BookingStatus status
    ) {

        if (status != null) {
            return bookingRepository.findByStatus(status);
        }

        return bookingRepository.findAll();
    }

    // ============================================
    // 5. PUBLICAR EVENTOS EN RABBITMQ
    // ============================================

    private void publishStatusEvent(Booking booking) {

        BookingEventMessage message = createEventMessage(
                booking,
                "BOOKING_STATUS_CHANGED",
                "La reserva cambio al estado "
                        + booking.getStatus().name()
        );

        switch (booking.getStatus()) {

            case APROBADA -> {

                // Notificacion de aprobacion.
                bookingMessageProducer.sendEmailMessage(message);

                // Ticket de preparacion.
                bookingMessageProducer.sendPrepMessage(message);
            }

            case EN_PREPARACION -> {

                bookingMessageProducer.sendPrepMessage(message);
            }

            case DEVUELTA -> {

                // Notificacion de devolucion.
                bookingMessageProducer.sendEmailMessage(message);

                // Generacion del voucher.
                bookingMessageProducer.sendVoucherMessage(message);
            }

            case CANCELADA -> {

                bookingMessageProducer.sendEmailMessage(message);
            }

            default -> {

                // Otros estados sin mensajes RabbitMQ.
            }
        }
    }

    // ============================================
    // 6. CREAR MENSAJE PARA RABBITMQ
    // ============================================

    private BookingEventMessage createEventMessage(
            Booking booking,
            String type,
            String message
    ) {

        String eventId = UUID.randomUUID().toString();
        String traceId = UUID.randomUUID().toString();

        String correlationId = "booking-" + booking.getId();

        return new BookingEventMessage(
                booking.getId(),
                booking.getStatus().name(),
                type,
                message,
                eventId,
                traceId,
                correlationId,
                LocalDateTime.now()
        );
    }

    // ============================================
    // 7. PUBLICAR EVENTOS DE RESERVAS EN KAFKA
    // ============================================

    private void publishKafkaEvent(
            Booking booking,
            String eventType
    ) {

        BookingKafkaProducer producer =
                kafkaProducerProvider.getIfAvailable();

        if (producer == null) {

            log.debug(
                    "Kafka desactivado. No se publica evento para reserva {}",
                    booking.getId()
            );

            return;
        }

        BookingKafkaEvent event = new BookingKafkaEvent(
                UUID.randomUUID().toString(),
                eventType,
                booking.getId(),
                booking.getStatus().name(),
                LocalDateTime.now()
        );

        try {

            producer.publish(event);

            log.info(
                    "Evento Kafka solicitado: {} | Reserva: {}",
                    eventType,
                    booking.getId()
            );

        } catch (RuntimeException exception) {

            log.error(
                    "No se pudo solicitar publicacion Kafka para reserva {}",
                    booking.getId(),
                    exception
            );
        }
    }

    // ============================================
    // 8. PUBLICAR EVENTOS DE OCUPACION EN KAFKA
    // ============================================

    private void publishOccupancyEvent(
            Booking booking,
            String eventType
    ) {

        OccupancyKafkaProducer producer =
                occupancyProducerProvider.getIfAvailable();

        if (producer == null) {

            log.debug(
                    "Kafka de ocupacion desactivado para reserva {}",
                    booking.getId()
            );

            return;
        }

        OccupancyKafkaEvent event = new OccupancyKafkaEvent(
                UUID.randomUUID().toString(),
                eventType,
                booking.getId(),
                booking.getResourceId(),
                booking.getStatus().name(),
                LocalDateTime.now()
        );

        try {

            producer.publish(event);

            log.info(
                    "Evento de ocupacion solicitado: {} | Recurso: {} | Reserva: {}",
                    eventType,
                    booking.getResourceId(),
                    booking.getId()
            );

        } catch (RuntimeException exception) {

            log.error(
                    "Error al solicitar evento de ocupacion para reserva {}",
                    booking.getId(),
                    exception
            );
        }
    }

    // ============================================
    // 9. VALIDAR TRANSICIONES DE ESTADO
    // ============================================

    private void validateStatusTransition(
            BookingStatus currentStatus,
            BookingStatus newStatus
    ) {

        if (currentStatus == null || newStatus == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe informar estados validos"
            );
        }

        boolean allowed = switch (currentStatus) {

            case SOLICITADA ->
                    newStatus == BookingStatus.APROBADA ||
                            newStatus == BookingStatus.CANCELADA;

            case APROBADA ->
                    newStatus == BookingStatus.EN_PREPARACION ||
                            newStatus == BookingStatus.CANCELADA;

            case EN_PREPARACION ->
                    newStatus == BookingStatus.EN_USO ||
                            newStatus == BookingStatus.CANCELADA;

            case EN_USO ->
                    newStatus == BookingStatus.DEVUELTA;

            case DEVUELTA, CANCELADA -> false;
        };

        if (!allowed) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Transicion de estado no permitida: "
                            + currentStatus.name()
                            + " -> "
                            + newStatus.name()
            );
        }
    }
}
