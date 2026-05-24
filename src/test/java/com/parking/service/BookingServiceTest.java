package com.parking.service;

import com.parking.dto.booking.BookingRequest;
import com.parking.entity.*;
import com.parking.exception.BadRequestException;
import com.parking.exception.ConflictException;
import com.parking.exception.ForbiddenException;
import com.parking.exception.ResourceNotFoundException;
import com.parking.mapper.BookingMapper;
import com.parking.mapper.BookingMapperImpl;
import com.parking.repository.BookingRepository;
import com.parking.repository.ParkingSpotRepository;
import com.parking.repository.SubscriptionRepository;
import com.parking.repository.VehicleRepository;
import com.parking.websocket.OccupancyWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock ParkingSpotRepository spotRepository;
    @Mock VehicleRepository vehicleRepository;
    @Mock SubscriptionRepository subscriptionRepository;
    @Mock UserService userService;
    @Mock PricingService pricingService;
    @Mock OccupancyWebSocketHandler webSocketHandler;
    @Spy  BookingMapper bookingMapper = new BookingMapperImpl();

    @InjectMocks BookingService bookingService;

    private User driver;
    private Parking parking;
    private ParkingSpot spot;

    @BeforeEach
    void setUp() {
        driver = User.builder().id(1L).email("a@b.io").role(Role.DRIVER).enabled(true).build();
        parking = Parking.builder().id(1L).name("Test").build();
        spot = ParkingSpot.builder().id(10L).parking(parking).spotNumber("A1")
                .type(SpotType.REGULAR).status(SpotStatus.FREE).build();
    }

    @Test
    void createBooking_persistsAndReservesSpot() {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        when(spotRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(spot));
        when(bookingRepository.findOverlapping(eq(10L), any(), any())).thenReturn(List.of());
        when(userService.loadById(1L)).thenReturn(driver);
        when(pricingService.calculate(eq(spot), any(), any())).thenReturn(new BigDecimal("10.00"));
        when(bookingRepository.save(any())).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(99L);
            return b;
        });

        var response = bookingService.create(1L, new BookingRequest(10L, null, start, end));

        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.totalAmount()).isEqualByComparingTo("10.00");
        assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(spot.getStatus()).isEqualTo(SpotStatus.RESERVED);
        verify(spotRepository).save(spot);
        verify(webSocketHandler).broadcastSpotUpdate(spot);
    }

    @Test
    void createBooking_rejectsOverlap() {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        when(spotRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(spot));
        when(bookingRepository.findOverlapping(eq(10L), any(), any()))
                .thenReturn(List.of(Booking.builder().id(50L).build()));

        assertThatThrownBy(() -> bookingService.create(1L, new BookingRequest(10L, null, start, end)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already booked");

        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_rejectsMaintenanceSpot() {
        spot.setStatus(SpotStatus.MAINTENANCE);
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = start.plus(1, ChronoUnit.HOURS);
        when(spotRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(spot));

        assertThatThrownBy(() -> bookingService.create(1L, new BookingRequest(10L, null, start, end)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("maintenance");
    }

    @Test
    void createBooking_rejectsTooShort() {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = start.plus(5, ChronoUnit.MINUTES); // < 15 min

        assertThatThrownBy(() -> bookingService.create(1L, new BookingRequest(10L, null, start, end)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Minimum booking duration");

        verifyNoInteractions(spotRepository);
    }

    @Test
    void createBooking_rejectsTooLong() {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = start.plus(40, ChronoUnit.DAYS); // > 30 days

        assertThatThrownBy(() -> bookingService.create(1L, new BookingRequest(10L, null, start, end)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Maximum booking duration");
    }

    @Test
    void createBooking_rejectsForeignVehicle() {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = start.plus(1, ChronoUnit.HOURS);
        User otherOwner = User.builder().id(99L).build();
        Vehicle foreign = Vehicle.builder().id(7L).owner(otherOwner).licensePlate("X").build();

        when(spotRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(spot));
        when(bookingRepository.findOverlapping(any(), any(), any())).thenReturn(List.of());
        when(userService.loadById(1L)).thenReturn(driver);
        when(vehicleRepository.findById(7L)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> bookingService.create(1L, new BookingRequest(10L, 7L, start, end)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void cancel_freesSpotAndBroadcasts() {
        spot.setStatus(SpotStatus.RESERVED);
        Booking booking = Booking.builder()
                .id(99L).user(driver).spot(spot)
                .status(BookingStatus.CONFIRMED)
                .startTime(Instant.now().plus(1, ChronoUnit.HOURS))
                .endTime(Instant.now().plus(3, ChronoUnit.HOURS))
                .totalAmount(new BigDecimal("10")).currency("USD")
                .build();
        when(bookingRepository.findById(99L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = bookingService.cancel(1L, 99L);

        assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(spot.getStatus()).isEqualTo(SpotStatus.FREE);
        verify(spotRepository).save(spot);
        verify(webSocketHandler).broadcastSpotUpdate(spot);
    }

    @Test
    void cancel_rejectsForeignBooking() {
        Booking booking = Booking.builder()
                .id(99L)
                .user(User.builder().id(2L).build())
                .spot(spot)
                .status(BookingStatus.CONFIRMED)
                .startTime(Instant.now().plus(1, ChronoUnit.HOURS))
                .endTime(Instant.now().plus(2, ChronoUnit.HOURS))
                .totalAmount(BigDecimal.ONE).currency("USD")
                .build();
        when(bookingRepository.findById(99L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancel(1L, 99L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void cancel_rejectsCompleted() {
        Booking booking = Booking.builder()
                .id(99L).user(driver).spot(spot)
                .status(BookingStatus.COMPLETED)
                .startTime(Instant.now().minus(2, ChronoUnit.HOURS))
                .endTime(Instant.now().minus(1, ChronoUnit.HOURS))
                .totalAmount(BigDecimal.ONE).currency("USD")
                .build();
        when(bookingRepository.findById(99L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.cancel(1L, 99L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be cancelled");
    }

    @Test
    void getById_404OnUnknown() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> bookingService.getById(1L, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
