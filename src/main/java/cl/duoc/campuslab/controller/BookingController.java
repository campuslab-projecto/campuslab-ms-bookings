package cl.duoc.campuslab.controller;

import cl.duoc.campuslab.model.Booking;
import cl.duoc.campuslab.model.BookingStatus;
import cl.duoc.campuslab.repository.BookingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingRepository bookingRepository;

    public BookingController(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @PostMapping
    public ResponseEntity<Booking> createBooking(@RequestBody Booking booking) {
        booking.setStatus(BookingStatus.SOLICITADA);
        booking.setBookingDate(LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingRepository.save(booking));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Booking> getBookingById(@PathVariable Long id) {
        return bookingRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Booking> updateStatus(@PathVariable Long id, @RequestBody Booking statusUpdate) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));

        BookingStatus newStatus = statusUpdate.getStatus();
        if (newStatus == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe informar un estado válido");
        }

        validateStatusTransition(booking.getStatus(), newStatus);
        booking.setStatus(newStatus);
        return ResponseEntity.ok(bookingRepository.save(booking));
    }

    @GetMapping
    public List<Booking> getAllBookings(@RequestParam(required = false) BookingStatus status) {
        if (status != null) {
            return bookingRepository.findByStatus(status);
        }
        return bookingRepository.findAll();
    }

    private void validateStatusTransition(BookingStatus currentStatus, BookingStatus newStatus) {
        if (newStatus == BookingStatus.EN_USO && currentStatus != BookingStatus.APROBADA) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No se puede pasar a EN_USO sin estar APROBADA"
            );
        }

        if (currentStatus == BookingStatus.DEVUELTA || currentStatus == BookingStatus.CANCELADA) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No se puede modificar una reserva finalizada o cancelada"
            );
        }
    }
}
