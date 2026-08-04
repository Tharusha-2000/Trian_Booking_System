package com.trian.booking.controller;

import com.trian.booking.dto.BookingRequestDTO;
import com.trian.booking.dto.SeatAvailabilityResponseDTO;
import com.trian.booking.dto.TrainScheduleResponseDTO;
import com.trian.booking.dto.WaitlistRequestDTO;
import com.trian.booking.dto.WaitlistResponseDTO;
import com.trian.booking.model.SeatBooking;
import com.trian.booking.model.Station;
import com.trian.booking.model.User;
import com.trian.booking.service.BookingService;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class BookingApiController {

    private static final int MAX_ATTEMPTS = 3;

    private final BookingService bookingService;

    public BookingApiController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/stations")
    public List<Station> getStations() {
        return bookingService.getStations();
    }

    @GetMapping("/train-schedule")
    public TrainScheduleResponseDTO getTrainSchedule() {
        return bookingService.getTrainSchedule();
    }

    @GetMapping("/availability")
    public List<SeatAvailabilityResponseDTO> getAvailableSeats(
            @RequestParam("origin") String origin,
            @RequestParam("destination") String destination,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return bookingService.getAvailableSeats(origin, destination, date);
    }

    @PostMapping("/bookings")
    public ResponseEntity<?> createBooking(@RequestBody BookingRequestDTO request, @AuthenticationPrincipal User currentUser) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                List<SeatBooking> bookings = bookingService.createBooking(request, currentUser);
                return ResponseEntity.ok(bookings);
            } catch (ConcurrencyFailureException e) {
                // Postgres detects the SERIALIZABLE conflict at commit time, i.e. after
                // createBooking() returns, so it surfaces here rather than inside the
                // transactional method. Retrying re-runs the whole transaction, since one
                // of the racing bookings has by now committed and the overlap check will
                // see it.
                if (attempt == MAX_ATTEMPTS) {
                    return ResponseEntity.status(409).body("Seat is being booked by someone else. Please try again.");
                }
            }
        }
        return ResponseEntity.status(409).body("Seat is being booked by someone else. Please try again.");
    }

    @GetMapping("/my-bookings")
    public List<SeatBooking> getMyBookings(@AuthenticationPrincipal User currentUser) {
        return bookingService.getMyBookings(currentUser);
    }

    @DeleteMapping("/bookings/{id}")
    public ResponseEntity<Void> cancelBooking(@PathVariable("id") Long id, @AuthenticationPrincipal User currentUser) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                bookingService.cancelBooking(id, currentUser);
                return ResponseEntity.noContent().build();
            } catch (ConcurrencyFailureException e) {
                if (attempt == MAX_ATTEMPTS) {
                    return ResponseEntity.status(409).body(null);
                }
            }
        }
        return ResponseEntity.status(409).build();
    }

    @PostMapping("/waitlist")
    public WaitlistResponseDTO joinWaitlist(@RequestBody WaitlistRequestDTO request, @AuthenticationPrincipal User currentUser) {
        return bookingService.joinWaitlist(request, currentUser);
    }

    @GetMapping("/my-waitlist")
    public List<WaitlistResponseDTO> getMyWaitlist(@AuthenticationPrincipal User currentUser) {
        return bookingService.getMyWaitlist(currentUser);
    }

    @DeleteMapping("/waitlist/{id}")
    public ResponseEntity<Void> leaveWaitlist(@PathVariable("id") Long id, @AuthenticationPrincipal User currentUser) {
        bookingService.leaveWaitlist(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
