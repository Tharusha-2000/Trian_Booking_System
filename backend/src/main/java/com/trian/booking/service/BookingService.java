package com.trian.booking.service;

import com.trian.booking.dto.BookingRequestDTO;
import com.trian.booking.dto.SeatAvailabilityResponseDTO;
import com.trian.booking.dto.TrainScheduleResponseDTO;
import com.trian.booking.model.Coach;
import com.trian.booking.model.Seat;
import com.trian.booking.model.SeatBooking;
import com.trian.booking.model.Station;
import com.trian.booking.model.Train;
import com.trian.booking.model.TrainStop;
import com.trian.booking.repository.CoachRepository;
import com.trian.booking.repository.SeatBookingRepository;
import com.trian.booking.repository.SeatRepository;
import com.trian.booking.repository.StationRepository;
import com.trian.booking.repository.TrainRepository;
import com.trian.booking.repository.TrainStopRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final StationRepository stationRepository;
    private final CoachRepository coachRepository;
    private final SeatRepository seatRepository;
    private final SeatBookingRepository seatBookingRepository;
    private final TrainRepository trainRepository;
    private final TrainStopRepository trainStopRepository;

    public BookingService(StationRepository stationRepository,
                          CoachRepository coachRepository,
                          SeatRepository seatRepository,
                          SeatBookingRepository seatBookingRepository,
                          TrainRepository trainRepository,
                          TrainStopRepository trainStopRepository) {
        this.stationRepository = stationRepository;
        this.coachRepository = coachRepository;
        this.seatRepository = seatRepository;
        this.seatBookingRepository = seatBookingRepository;
        this.trainRepository = trainRepository;
        this.trainStopRepository = trainStopRepository;
    }

    public TrainScheduleResponseDTO getTrainSchedule() {
        Train train = trainRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No train configured"));

        List<TrainScheduleResponseDTO.StopDTO> stops = trainStopRepository.findAllByOrderByStation_Ordinal().stream()
                .map(stop -> new TrainScheduleResponseDTO.StopDTO(
                        stop.getStation().getCode(),
                        stop.getStation().getName(),
                        stop.getApproximateTime().format(TIME_FORMAT)))
                .toList();

        return new TrainScheduleResponseDTO(train.getName(), stops);
    }

    // The line runs one train per day: if the requested trip starts today and
    // that train has already passed the origin station's approximate time, there
    // is nothing left to book on today's run.
    private void assertTrainHasNotDeparted(Station origin, LocalDate travelDate) {
        if (!travelDate.isEqual(LocalDate.now())) {
            return;
        }

        TrainStop originStop = trainStopRepository.findByStation(origin).orElse(null);
        if (originStop == null) {
            return;
        }

        if (LocalTime.now().isAfter(originStop.getApproximateTime())) {
            throw new IllegalStateException(
                    "The train has already departed " + origin.getName() + " today (approx. "
                            + originStop.getApproximateTime().format(TIME_FORMAT) + ").choose a future date for travel.");
        }
    }

    public List<Station> getStations() {
        return stationRepository.findAll().stream()
                .sorted(Comparator.comparingInt(Station::getOrdinal))
                .toList();
    }

    public List<SeatAvailabilityResponseDTO> getAvailableSeats(String originCode, String destinationCode, LocalDate travelDate) {
        Station origin = stationRepository.findByCode(originCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid origin code"));
        Station destination = stationRepository.findByCode(destinationCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid destination code"));

        if (travelDate == null) {
            throw new IllegalArgumentException("Travel date is required");
        }

        int originOrdinal = origin.getOrdinal();
        int destinationOrdinal = destination.getOrdinal();


        log.info("Origin: {}, Destination: {}, Travel Date: {}, Origin Ordinal: {}, Destination Ordinal: {}", originCode, destinationCode, travelDate, originOrdinal, destinationOrdinal);

        if (originOrdinal >= destinationOrdinal) {
            throw new IllegalArgumentException("Origin must come before destination");
        }

        assertTrainHasNotDeparted(origin, travelDate);

        List<Seat> allSeats = seatRepository.findByCoach_ReservedTrue();
        List<SeatAvailabilityResponseDTO> seatMap = new ArrayList<>();

        for (Seat seat : allSeats) {
            List<SeatBooking> overlapping = seatBookingRepository.findOverlappingBookings(seat, travelDate, originOrdinal, destinationOrdinal);
            seatMap.add(new SeatAvailabilityResponseDTO(
                    seat.getId(),
                    seat.getCoach().getCode(),
                    seat.getSeatNumber(),
                    seat.getCoach().isReserved(),
                    overlapping.isEmpty()));
        }

        return seatMap;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<SeatBooking> createBooking(BookingRequestDTO request) {
        List<Long> seatIds = request.getSeatIds();
        if (seatIds == null || seatIds.isEmpty()) {
            throw new IllegalArgumentException("At least one seat must be selected");
        }
        if (seatIds.contains(null)) {
            throw new IllegalArgumentException("Seat selection contains an invalid seat id");
        }
        if (new HashSet<>(seatIds).size() != seatIds.size()) {
            throw new IllegalArgumentException("Duplicate seat in selection");
        }

        LocalDate travelDate = request.getTravelDate();
        if (travelDate == null) {
            throw new IllegalArgumentException("Travel date is required");
        }
        if (travelDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Travel date cannot be in the past");
        }

        Station origin = stationRepository.findByCode(request.getOriginCode())
                .orElseThrow(() -> new IllegalArgumentException("Invalid origin"));
        Station destination = stationRepository.findByCode(request.getDestinationCode())
                .orElseThrow(() -> new IllegalArgumentException("Invalid destination"));

        if (origin.getOrdinal() >= destination.getOrdinal()) {
            throw new IllegalArgumentException("Origin must come before destination");
        }

        assertTrainHasNotDeparted(origin, travelDate);

        List<SeatBooking> bookings = new ArrayList<>();
        for (Long seatId : seatIds) {
            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new IllegalArgumentException("Seat not found: " + seatId));

            List<SeatBooking> overlapping = seatBookingRepository.findOverlappingBookings(seat, travelDate, origin.getOrdinal(), destination.getOrdinal());
            if (!overlapping.isEmpty()) {
                throw new IllegalStateException("Seat " + seat.getSeatNumber() + " is not available for the selected leg on " + travelDate);
            }

            long fare = calculateFare(seat.getCoach(), origin.getOrdinal(), destination.getOrdinal());
            bookings.add(new SeatBooking(seat, origin, destination, travelDate, request.getPassengerName(), fare));
        }

        return seatBookingRepository.saveAll(bookings);
    }

    private long calculateFare(Coach coach, int originOrdinal, int destinationOrdinal) {
        int distanceSegments = destinationOrdinal - originOrdinal;
        long baseFarePerSegment = coach.isReserved() ? 250 : 125;
        return distanceSegments * baseFarePerSegment;
    }
}
