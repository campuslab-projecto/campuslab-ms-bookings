
package cl.duoc.campuslab.service;

import cl.duoc.campuslab.messaging.kafka.BookingKafkaEvent;
import cl.duoc.campuslab.messaging.kafka.BookingKafkaProducer;
import cl.duoc.campuslab.messaging.kafka.OccupancyKafkaEvent;
import cl.duoc.campuslab.messaging.kafka.OccupancyKafkaProducer;
import cl.duoc.campuslab.messaging.producer.BookingMessageProducer;
import cl.duoc.campuslab.model.Booking;
import cl.duoc.campuslab.model.BookingStatus;
import cl.duoc.campuslab.repository.BookingRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingMessageProducer bookingMessageProducer;

    @Mock
    private ObjectProvider<BookingKafkaProducer> kafkaProducerProvider;

    @Mock
    private ObjectProvider<OccupancyKafkaProducer> occupancyProducerProvider;

    @Mock
    private BookingKafkaProducer kafkaProducer;

    @Mock
    private OccupancyKafkaProducer occupancyProducer;

    private BookingService bookingService;

    private Booking booking;

    @BeforeEach
    void setUp() {

        bookingService = new BookingService(
                bookingRepository,
                bookingMessageProducer,
                kafkaProducerProvider,
                occupancyProducerProvider
        );

        booking = new Booking();
        booking.setId(1L);
        booking.setUserEmail("test@campuslab.cl");
        booking.setResourceId("RECURSO-EP6-001");
        booking.setStatus(BookingStatus.SOLICITADA);
    }

    @Test
    void createBookingShouldPreserveResourceIdAndPublishKafkaEvent() {

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(kafkaProducerProvider.getIfAvailable())
                .thenReturn(kafkaProducer);

        Booking result = bookingService.createBooking(booking);

        assertEquals("RECURSO-EP6-001", result.getResourceId());
        assertEquals(BookingStatus.SOLICITADA, result.getStatus());
        assertNotNull(result.getBookingDate());

        ArgumentCaptor<BookingKafkaEvent> captor =
                ArgumentCaptor.forClass(BookingKafkaEvent.class);

        verify(kafkaProducer).publish(captor.capture());

        BookingKafkaEvent event = captor.getValue();

        assertEquals(1L, event.bookingId());
        assertEquals("RECURSO-EP6-001", event.resourceId());
        assertEquals("BOOKING_CREATED", event.eventType());
        assertEquals("SOLICITADA", event.status());

        verifyNoInteractions(occupancyProducer);
    }

    @Test
    void createBookingShouldWorkWhenKafkaIsDisabled() {

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(kafkaProducerProvider.getIfAvailable())
                .thenReturn(null);

        Booking result = bookingService.createBooking(booking);

        assertNotNull(result);
        assertEquals("RECURSO-EP6-001", result.getResourceId());

        verify(kafkaProducer, never()).publish(any(BookingKafkaEvent.class));
        verifyNoInteractions(occupancyProducer);
    }

    @Test
    void updateStatusShouldPreserveResourceIdAndPublishKafkaEvent() {

        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(booking));

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(kafkaProducerProvider.getIfAvailable())
                .thenReturn(kafkaProducer);

        Booking result = bookingService.updateStatus(
                1L,
                BookingStatus.APROBADA
        );

        assertEquals(BookingStatus.APROBADA, result.getStatus());
        assertEquals("RECURSO-EP6-001", result.getResourceId());

        ArgumentCaptor<BookingKafkaEvent> captor =
                ArgumentCaptor.forClass(BookingKafkaEvent.class);

        verify(kafkaProducer).publish(captor.capture());

        assertEquals(
                "RECURSO-EP6-001",
                captor.getValue().resourceId()
        );

        assertEquals(
                "BOOKING_STATUS_CHANGED",
                captor.getValue().eventType()
        );

        verifyNoInteractions(occupancyProducer);
    }

    @Test
    void updateStatusToInUseShouldPublishOccupancyEvent() {

        booking.setStatus(BookingStatus.EN_PREPARACION);

        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(booking));

        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(kafkaProducerProvider.getIfAvailable())
                .thenReturn(null);

        when(occupancyProducerProvider.getIfAvailable())
                .thenReturn(occupancyProducer);

        bookingService.updateStatus(1L, BookingStatus.EN_USO);

        ArgumentCaptor<OccupancyKafkaEvent> captor =
                ArgumentCaptor.forClass(OccupancyKafkaEvent.class);

        verify(occupancyProducer).publish(captor.capture());

        OccupancyKafkaEvent event = captor.getValue();

        assertEquals(1L, event.bookingId());
        assertEquals("RECURSO-EP6-001", event.resourceId());
        assertEquals("OCCUPANCY_STARTED", event.eventType());
        assertEquals("EN_USO", event.status());

        verifyNoInteractions(kafkaProducer);
    }

    @Test
    void updateStatusShouldRejectInvalidTransition() {

        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(booking));

        assertThrows(
                ResponseStatusException.class,
                () -> bookingService.updateStatus(
                        1L,
                        BookingStatus.DEVUELTA
                )
        );

        verify(bookingRepository, never()).save(any(Booking.class));
        verifyNoInteractions(kafkaProducer);
        verifyNoInteractions(occupancyProducer);
    }

    @Test
    void getBookingByIdShouldReturnBooking() {

        when(bookingRepository.findById(1L))
                .thenReturn(Optional.of(booking));

        Booking result = bookingService.getBookingById(1L);

        assertEquals(1L, result.getId());
        assertEquals("RECURSO-EP6-001", result.getResourceId());
    }

    @Test
    void getBookingByIdShouldReturnNotFoundWhenMissing() {

        when(bookingRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResponseStatusException.class,
                () -> bookingService.getBookingById(99L)
        );
    }
}
