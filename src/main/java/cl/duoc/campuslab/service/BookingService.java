package cl.duoc.campuslab.service;

import cl.duoc.campuslab.messaging.dto.BookingEventMessage;
import cl.duoc.campuslab.messaging.producer.BookingMessageProducer;
import cl.duoc.campuslab.model.Booking;
import cl.duoc.campuslab.model.BookingStatus;
import cl.duoc.campuslab.repository.BookingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingMessageProducer bookingMessageProducer;

    public BookingService(
            BookingRepository bookingRepository,
            BookingMessageProducer bookingMessageProducer
    ) {
        this.bookingRepository = bookingRepository;
        this.bookingMessageProducer = bookingMessageProducer;
    }

    public Booking createBooking(Booking booking) {

        booking.setStatus(BookingStatus.SOLICITADA);
        booking.setBookingDate(LocalDateTime.now());

        Booking savedBooking = bookingRepository.save(booking);

        BookingEventMessage message = createEventMessage(
                savedBooking,
                "BOOKING_CREATED",
                "Reserva creada correctamente"
        );

        bookingMessageProducer.sendEmailMessage(message);

        return savedBooking;
    }

    public Booking getBookingById(Long id) {

        return bookingRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Reserva no encontrada"
                        )
                );
    }

    public Booking updateStatus(Long id, BookingStatus newStatus) {

        Booking booking = getBookingById(id);

        if (newStatus == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debe informar un estado válido"
            );
        }

        validateStatusTransition(
                booking.getStatus(),
                newStatus
        );

        booking.setStatus(newStatus);

        Booking savedBooking =
                bookingRepository.save(booking);

        publishStatusEvent(savedBooking);

        return savedBooking;
    }

    public List<Booking> getAllBookings(BookingStatus status) {

        if (status != null) {
            return bookingRepository.findByStatus(status);
        }

        return bookingRepository.findAll();
    }

    private void publishStatusEvent(Booking booking) {

        BookingEventMessage message = createEventMessage(
                booking,
                "BOOKING_STATUS_CHANGED",
                "La reserva cambió al estado "
                        + booking.getStatus().name()
        );

        switch (booking.getStatus()) {

            case APROBADA -> {
                bookingMessageProducer.sendEmailMessage(message);
                bookingMessageProducer.sendPrepMessage(message);
            }

            case EN_PREPARACION -> {
                bookingMessageProducer.sendPrepMessage(message);
            }

            case DEVUELTA -> {
                bookingMessageProducer.sendEmailMessage(message);
                bookingMessageProducer.sendVoucherMessage(message);
            }

            case CANCELADA -> {
                bookingMessageProducer.sendEmailMessage(message);
            }

            default -> {
                // No se requiere mensajería adicional
                // para este estado
            }
        }
    }

    private BookingEventMessage createEventMessage(
            Booking booking,
            String type,
            String message
    ) {

        String eventId = UUID.randomUUID().toString();
        String traceId = UUID.randomUUID().toString();
        String correlationId =
                "booking-" + booking.getId();

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

    private void validateStatusTransition(
            BookingStatus currentStatus,
            BookingStatus newStatus
    ) {

        if (newStatus == BookingStatus.EN_USO
                && currentStatus != BookingStatus.APROBADA
                && currentStatus != BookingStatus.EN_PREPARACION) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No se puede pasar a EN_USO sin estar APROBADA o EN_PREPARACION"
            );
        }

        if (currentStatus == BookingStatus.DEVUELTA
                || currentStatus == BookingStatus.CANCELADA) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No se puede modificar una reserva finalizada o cancelada"
            );
        }
    }
}