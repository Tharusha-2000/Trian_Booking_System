package com.trian.booking.service;

import com.trian.booking.dto.BookingRequestDTO;
import com.trian.booking.dto.SeatAvailabilityResponseDTO;
import com.trian.booking.model.Coach;
import com.trian.booking.model.Seat;
import com.trian.booking.model.SeatBooking;
import com.trian.booking.model.Station;
import com.trian.booking.repository.CoachRepository;
import com.trian.booking.repository.SeatBookingRepository;
import com.trian.booking.repository.SeatRepository;
import com.trian.booking.repository.StationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final StationRepository stationRepository;
    private final CoachRepository coachRepository;
    private final SeatRepository seatRepository;
    private final SeatBookingRepository seatBookingRepository;

    public BookingService(StationRepository stationRepository,
                          CoachRepository coachRepository,
                          SeatRepository seatRepository,
                          SeatBookingRepository seatBookingRepository) {
        this.stationRepository = stationRepository;
        this.coachRepository = coachRepository;
        this.seatRepository = seatRepository;
        this.seatBookingRepository = seatBookingRepository;
    }

    public List<Station> getStations() {
        return stationRepository.findAll().stream()
                .sorted(Comparator.comparingInt(Station::getOrdinal))
                .toList();
    }

    public List<SeatAvailabilityResponseDTO> getAvailableSeats(String originCode, String destinationCode) {
        Station origin = stationRepository.findByCode(originCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid origin code"));
        Station destination = stationRepository.findByCode(destinationCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid destination code"));

        int originOrdinal = origin.getOrdinal();
        int destinationOrdinal = destination.getOrdinal();
        if (originOrdinal >= destinationOrdinal) {
            throw new IllegalArgumentException("Origin must come before destination");
        }

        List<Seat> allSeats = seatRepository.findAll();
        List<SeatAvailabilityResponseDTO> availableSeats = new ArrayList<>();

        for (Seat seat : allSeats) {
            List<SeatBooking> overlapping = seatBookingRepository.findOverlappingBookings(seat, originOrdinal, destinationOrdinal);
            if (overlapping.isEmpty()) {
                availableSeats.add(new SeatAvailabilityResponseDTO(
                        seat.getId(),
                        seat.getCoach().getCode(),
                        seat.getSeatNumber(),
                        seat.getCoach().isReserved()));
            }
        }

        return availableSeats;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public SeatBooking createBooking(BookingRequestDTO request) {
        Seat seat = seatRepository.findById(request.getSeatId())
                .orElseThrow(() -> new IllegalArgumentException("Seat not found"));

        Station origin = stationRepository.findByCode(request.getOriginCode())
                .orElseThrow(() -> new IllegalArgumentException("Invalid origin"));
        Station destination = stationRepository.findByCode(request.getDestinationCode())
                .orElseThrow(() -> new IllegalArgumentException("Invalid destination"));

        if (origin.getOrdinal() >= destination.getOrdinal()) {
            throw new IllegalArgumentException("Origin must come before destination");
        }

        List<SeatBooking> overlapping = seatBookingRepository.findOverlappingBookings(seat, origin.getOrdinal(), destination.getOrdinal());
        if (!overlapping.isEmpty()) {
            throw new IllegalStateException("Seat is not available for the selected leg");
        }

        long fare = calculateFare(seat.getCoach(), origin.getOrdinal(), destination.getOrdinal());
        SeatBooking booking = new SeatBooking(seat, origin, destination, request.getPassengerName(), fare);
        return seatBookingRepository.save(booking);
    }

    private long calculateFare(Coach coach, int originOrdinal, int destinationOrdinal) {
        int distanceSegments = destinationOrdinal - originOrdinal;
        long baseFarePerSegment = coach.isReserved() ? 250 : 125;
        return distanceSegments * baseFarePerSegment;
    }
}
