package com.trian.booking.repository;

import com.trian.booking.model.Seat;
import com.trian.booking.model.User;
import com.trian.booking.model.WaitlistEntry;
import com.trian.booking.model.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, Long> {

    List<WaitlistEntry> findBySeatAndTravelDateAndStatusOrderByRequestedAtAsc(Seat seat, LocalDate travelDate, WaitlistStatus status);

    List<WaitlistEntry> findByUserOrderByRequestedAtDesc(User user);

    boolean existsBySeatAndTravelDateAndUserAndStatus(Seat seat, LocalDate travelDate, User user, WaitlistStatus status);

    long countBySeatAndTravelDateAndStatusAndRequestedAtBefore(Seat seat, LocalDate travelDate, WaitlistStatus status, LocalDateTime before);
}
