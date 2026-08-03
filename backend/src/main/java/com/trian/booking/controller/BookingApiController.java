package com.trian.booking.controller;

import com.trian.booking.dto.BookingRequestDTO;
import com.trian.booking.dto.SeatAvailabilityResponseDTO;
import com.trian.booking.model.SeatBooking;
import com.trian.booking.model.Station;
import com.trian.booking.service.BookingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/availability")
    public List<SeatAvailabilityResponseDTO> getAvailableSeats(
            @RequestParam("origin") String origin,
            @RequestParam("destination") String destination) {
        return bookingService.getAvailableSeats(origin, destination);
    }

    @PostMapping("/bookings")
    public ResponseEntity<?> createBooking(@RequestBody BookingRequestDTO request) {
        try {
            SeatBooking booking = bookingService.createBooking(request);
            return ResponseEntity.ok(booking);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }
}
