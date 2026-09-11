package cl.duoc.campuslab.repository;

import cl.duoc.campuslab.model.Booking;
import cl.duoc.campuslab.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByStatus(BookingStatus status);
}
