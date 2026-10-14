package cl.duoc.campuslab.controller;

import cl.duoc.campuslab.model.Booking;
import cl.duoc.campuslab.model.BookingStatus;
import cl.duoc.campuslab.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<Booking> createBooking(
            @RequestBody Booking booking
    ) {
        Booking createdBooking = bookingService.createBooking(booking);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdBooking);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Booking> getBookingById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                bookingService.getBookingById(id)
        );
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Booking> updateStatus(
            @PathVariable Long id,
            @RequestBody Booking statusUpdate
    ) {
        Booking updatedBooking = bookingService.updateStatus(
                id,
                statusUpdate.getStatus()
        );

        return ResponseEntity.ok(updatedBooking);
    }

    @GetMapping
    public ResponseEntity<List<Booking>> getAllBookings(
            @RequestParam(required = false) BookingStatus status
    ) {
        return ResponseEntity.ok(
                bookingService.getAllBookings(status)
        );
    }
}