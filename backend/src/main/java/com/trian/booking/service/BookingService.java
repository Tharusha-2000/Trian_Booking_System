package com.trian.booking.service;

import com.trian.booking.dto.AdminStatsResponseDTO;
import com.trian.booking.dto.BookingRequestDTO;
import com.trian.booking.dto.SeatAvailabilityResponseDTO;
import com.trian.booking.dto.TrainScheduleResponseDTO;
import com.trian.booking.dto.WaitlistRequestDTO;
import com.trian.booking.dto.WaitlistResponseDTO;
import com.trian.booking.model.Coach;
import com.trian.booking.model.Role;
import com.trian.booking.model.Seat;
import com.trian.booking.model.SeatBooking;
import com.trian.booking.model.Station;
import com.trian.booking.model.Train;
import com.trian.booking.model.TrainStop;
import com.trian.booking.model.User;
import com.trian.booking.model.WaitlistEntry;
import com.trian.booking.model.WaitlistStatus;
import com.trian.booking.repository.CoachRepository;
import com.trian.booking.repository.SeatBookingRepository;
import com.trian.booking.repository.SeatRepository;
import com.trian.booking.repository.StationRepository;
import com.trian.booking.repository.TrainRepository;
import com.trian.booking.repository.TrainStopRepository;
import com.trian.booking.repository.WaitlistEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    private final WaitlistEntryRepository waitlistEntryRepository;

    public BookingService(StationRepository stationRepository,
                          CoachRepository coachRepository,
                          SeatRepository seatRepository,
                          SeatBookingRepository seatBookingRepository,
                          TrainRepository trainRepository,
                          TrainStopRepository trainStopRepository,
                          WaitlistEntryRepository waitlistEntryRepository) {
        this.stationRepository = stationRepository;
        this.coachRepository = coachRepository;
        this.seatRepository = seatRepository;
        this.seatBookingRepository = seatBookingRepository;
        this.trainRepository = trainRepository;
        this.trainStopRepository = trainStopRepository;
        this.waitlistEntryRepository = waitlistEntryRepository;
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

    public AdminStatsResponseDTO getAdminStats(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();

        List<SeatBooking> allBookings = seatBookingRepository.findAll();
        long totalRevenue = allBookings.stream().mapToLong(SeatBooking::getFare).sum();
        long totalBookings = allBookings.size();

        List<SeatBooking> bookingsForDate = allBookings.stream()
                .filter(b -> b.getTravelDate().isEqual(targetDate))
                .toList();
        long revenueForDate = bookingsForDate.stream().mapToLong(SeatBooking::getFare).sum();

        // Occupancy is only meaningful for reserved coaches: unreserved coaches have no
        // seat assignment at all, so there is nothing per-seat to report on.
        List<AdminStatsResponseDTO.CoachOccupancyDTO> occupancy = coachRepository.findAll().stream()
                .filter(Coach::isReserved)
                .sorted(Comparator.comparing(Coach::getCode))
                .map(coach -> {
                    List<Seat> seats = seatRepository.findByCoach(coach);
                    long bookedSeats = seats.stream()
                            .filter(seat -> bookingsForDate.stream().anyMatch(b -> b.getSeat().getId().equals(seat.getId())))
                            .count();
                    double occupancyPercent = seats.isEmpty() ? 0.0 : (100.0 * bookedSeats / seats.size());
                    return new AdminStatsResponseDTO.CoachOccupancyDTO(coach.getCode(), coach.isReserved(), seats.size(), bookedSeats, occupancyPercent);
                })
                .toList();

        return new AdminStatsResponseDTO(totalRevenue, totalBookings, targetDate, revenueForDate, bookingsForDate.size(), occupancy);
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

        // Occupancy is a coach-level property for this date+leg, so compute it once
        // per coach rather than once per seat.
        Map<Long, Double> occupancyByCoachId = new HashMap<>();

        for (Seat seat : allSeats) {
            List<SeatBooking> overlapping = seatBookingRepository.findOverlappingBookings(seat, travelDate, originOrdinal, destinationOrdinal);

            double occupancyRate = occupancyByCoachId.computeIfAbsent(seat.getCoach().getId(),
                    id -> occupancyRateFor(seat.getCoach(), travelDate, originOrdinal, destinationOrdinal));
            long estimatedFare = calculateFare(seat.getCoach(), occupancyRate, originOrdinal, destinationOrdinal);

            seatMap.add(new SeatAvailabilityResponseDTO(
                    seat.getId(),
                    seat.getCoach().getCode(),
                    seat.getSeatNumber(),
                    seat.getCoach().isReserved(),
                    overlapping.isEmpty(),
                    estimatedFare,
                    demandLabel(occupancyRate)));
        }

        return seatMap;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<SeatBooking> createBooking(BookingRequestDTO request, User currentUser) {
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

        // Snapshot each coach's occupancy once at the start of this request, rather
        // than re-querying per seat, so a multi-seat request in the same coach prices
        // consistently instead of drifting upward against its own not-yet-saved seats.
        Map<Long, Double> occupancyByCoachId = new HashMap<>();

        List<SeatBooking> bookings = new ArrayList<>();
        for (Long seatId : seatIds) {
            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new IllegalArgumentException("Seat not found: " + seatId));

            List<SeatBooking> overlapping = seatBookingRepository.findOverlappingBookings(seat, travelDate, origin.getOrdinal(), destination.getOrdinal());
            if (!overlapping.isEmpty()) {
                throw new IllegalStateException("Seat " + seat.getSeatNumber() + " is not available for the selected leg on " + travelDate);
            }

            double occupancyRate = occupancyByCoachId.computeIfAbsent(seat.getCoach().getId(),
                    id -> occupancyRateFor(seat.getCoach(), travelDate, origin.getOrdinal(), destination.getOrdinal()));
            long fare = calculateFare(seat.getCoach(), occupancyRate, origin.getOrdinal(), destination.getOrdinal());
            String passengerName = (request.getPassengerName() == null || request.getPassengerName().isBlank())
                    ? currentUser.getEmail()
                    : request.getPassengerName();
            bookings.add(new SeatBooking(seat, origin, destination, currentUser, travelDate, passengerName, fare));
        }

        return seatBookingRepository.saveAll(bookings);
    }

    public List<SeatBooking> getMyBookings(User currentUser) {
        return seatBookingRepository.findByUserOrderByTravelDateDesc(currentUser);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void cancelBooking(Long bookingId, User currentUser) {
        SeatBooking booking = seatBookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        boolean isOwner = booking.getUser() != null && booking.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to cancel this booking");
        }

        Seat seat = booking.getSeat();
        LocalDate travelDate = booking.getTravelDate();

        // Deleting the row is what actually frees the seat: findOverlappingBookings
        // no longer sees it, so the segment becomes bookable again for that date.
        seatBookingRepository.delete(booking);

        // First-come-first-served: promote the longest-waiting entry whose requested
        // leg now fits in the freed segment. Runs in the same SERIALIZABLE transaction
        // as the delete, so it can't race a concurrent direct booking on this seat.
        promoteNextWaitlistEntry(seat, travelDate);
    }

    private void promoteNextWaitlistEntry(Seat seat, LocalDate travelDate) {
        List<WaitlistEntry> candidates = waitlistEntryRepository
                .findBySeatAndTravelDateAndStatusOrderByRequestedAtAsc(seat, travelDate, WaitlistStatus.WAITING);

        for (WaitlistEntry candidate : candidates) {
            List<SeatBooking> overlapping = seatBookingRepository.findOverlappingBookings(
                    seat, travelDate, candidate.getOrigin().getOrdinal(), candidate.getDestination().getOrdinal());
            if (overlapping.isEmpty()) {
                double occupancyRate = occupancyRateFor(seat.getCoach(), travelDate, candidate.getOrigin().getOrdinal(), candidate.getDestination().getOrdinal());
                long fare = calculateFare(seat.getCoach(), occupancyRate, candidate.getOrigin().getOrdinal(), candidate.getDestination().getOrdinal());
                SeatBooking promoted = new SeatBooking(seat, candidate.getOrigin(), candidate.getDestination(),
                        candidate.getUser(), travelDate, candidate.getPassengerName(), fare);
                seatBookingRepository.save(promoted);

                candidate.setStatus(WaitlistStatus.CONFIRMED);
                waitlistEntryRepository.save(candidate);
                return; // Only one seat just freed up, so only one entry can be promoted.
            }
        }
    }

    public WaitlistResponseDTO joinWaitlist(WaitlistRequestDTO request, User currentUser) {
        if (request.getSeatId() == null) {
            throw new IllegalArgumentException("Seat is required");
        }
        Seat seat = seatRepository.findById(request.getSeatId())
                .orElseThrow(() -> new IllegalArgumentException("Seat not found"));

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

        List<SeatBooking> overlapping = seatBookingRepository.findOverlappingBookings(
                seat, travelDate, origin.getOrdinal(), destination.getOrdinal());
        if (overlapping.isEmpty()) {
            throw new IllegalStateException("Seat " + seat.getSeatNumber() + " is already available — book it directly instead of waitlisting");
        }

        boolean bookedByCurrentUser = overlapping.stream()
                .anyMatch(booking -> booking.getUser() != null && booking.getUser().getId().equals(currentUser.getId()));
        if (bookedByCurrentUser) {
            throw new IllegalStateException("You already booked seat " + seat.getSeatNumber() + " for this leg on " + travelDate);
        }

        if (waitlistEntryRepository.existsBySeatAndTravelDateAndUserAndStatus(seat, travelDate, currentUser, WaitlistStatus.WAITING)) {
            throw new IllegalStateException("You are already on the waitlist for seat " + seat.getSeatNumber() + " on " + travelDate);
        }

        String passengerName = (request.getPassengerName() == null || request.getPassengerName().isBlank())
                ? currentUser.getEmail()
                : request.getPassengerName();

        WaitlistEntry entry = waitlistEntryRepository.save(
                new WaitlistEntry(seat, origin, destination, currentUser, travelDate, passengerName));

        return toWaitlistResponseDTO(entry);
    }

    public List<WaitlistResponseDTO> getMyWaitlist(User currentUser) {
        return waitlistEntryRepository.findByUserOrderByRequestedAtDesc(currentUser).stream()
                .map(this::toWaitlistResponseDTO)
                .toList();
    }

    @Transactional
    public void leaveWaitlist(Long id, User currentUser) {
        WaitlistEntry entry = waitlistEntryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Waitlist entry not found"));

        boolean isOwner = entry.getUser() != null && entry.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to modify this waitlist entry");
        }

        waitlistEntryRepository.delete(entry);
    }

    private WaitlistResponseDTO toWaitlistResponseDTO(WaitlistEntry entry) {
        long position = entry.getStatus() == WaitlistStatus.WAITING
                ? waitlistEntryRepository.countBySeatAndTravelDateAndStatusAndRequestedAtBefore(
                        entry.getSeat(), entry.getTravelDate(), WaitlistStatus.WAITING, entry.getRequestedAt()) + 1
                : 0;

        return new WaitlistResponseDTO(
                entry.getId(),
                entry.getSeat().getSeatNumber(),
                entry.getSeat().getCoach().getCode(),
                entry.getOrigin().getCode(),
                entry.getDestination().getCode(),
                entry.getTravelDate(),
                entry.getStatus().name(),
                position);
    }

    // Yield-management style pricing: a reserved seat that sits empty for the rest of
    // the journey can never be resold once the train departs, which is exactly the
    // revenue problem the department wants addressed. Charging a premium once a coach
    // is nearly full on a given date+leg recaptures some of that otherwise-lost value,
    // while a quiet coach still books at the plain distance-based rate.
    private double occupancyRateFor(Coach coach, LocalDate travelDate, int originOrdinal, int destinationOrdinal) {
        List<Seat> coachSeats = seatRepository.findByCoach(coach);
        if (coachSeats.isEmpty()) {
            return 0.0;
        }
        long occupied = coachSeats.stream()
                .filter(seat -> !seatBookingRepository.findOverlappingBookings(seat, travelDate, originOrdinal, destinationOrdinal).isEmpty())
                .count();
        return (double) occupied / coachSeats.size();
    }

    private double demandMultiplier(double occupancyRate) {
        if (occupancyRate >= 0.8) {
            return 1.5; // High demand: only a few seats left
        }
        if (occupancyRate >= 0.5) {
            return 1.2; // Busy: over half the coach is booked
        }
        return 1.0; // Standard rate
    }

    private String demandLabel(double occupancyRate) {
        if (occupancyRate >= 0.8) {
            return "High Demand";
        }
        if (occupancyRate >= 0.5) {
            return "Busy";
        }
        return "Standard";
    }

    private long calculateFare(Coach coach, double occupancyRate, int originOrdinal, int destinationOrdinal) {
        int distanceSegments = destinationOrdinal - originOrdinal;
        long baseFarePerSegment = coach.isReserved() ? 250 : 125;
        return Math.round(distanceSegments * baseFarePerSegment * demandMultiplier(occupancyRate));
    }
}
