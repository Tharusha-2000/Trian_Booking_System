package com.trian.booking.controller;

import com.trian.booking.dto.BookingRequestDTO;
import com.trian.booking.dto.SeatAvailabilityResponseDTO;
import com.trian.booking.dto.TrainScheduleResponseDTO;
import com.trian.booking.model.SeatBooking;
import com.trian.booking.model.Station;
import com.trian.booking.service.BookingService;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api")
public class BookingApiController {

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

    private static final int MAX_BOOKING_ATTEMPTS = 3;

    @PostMapping("/bookings")
    public ResponseEntity<?> createBooking(@RequestBody BookingRequestDTO request) {
        for (int attempt = 1; attempt <= MAX_BOOKING_ATTEMPTS; attempt++) {
            try {
                List<SeatBooking> bookings = bookingService.createBooking(request);
                return ResponseEntity.ok(bookings);
            } catch (ConcurrencyFailureException e) {
                // Postgres detects the SERIALIZABLE conflict at commit time, i.e. after
                // createBooking() returns, so it surfaces here rather than inside the
                // transactional method. Retrying re-runs the whole transaction, since one
                // of the racing bookings has by now committed and the overlap check will
                // see it.
                if (attempt == MAX_BOOKING_ATTEMPTS) {
                    return ResponseEntity.status(409).body("Seat is being booked by someone else. Please try again.");
                }
            }
        }
        return ResponseEntity.status(409).body("Seat is being booked by someone else. Please try again.");
    }

    // Shared across every endpoint in this controller (getAvailableSeats included),
    // so a bad request or a business-state conflict (e.g. "train already departed",
    // "seat not available") always comes back as a clean 4xx instead of a raw 500.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleConflict(IllegalStateException e) {
        return ResponseEntity.status(409).body(e.getMessage());
    }
}
