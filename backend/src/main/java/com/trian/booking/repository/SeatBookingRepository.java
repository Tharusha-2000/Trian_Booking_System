package com.trian.booking.repository;

import com.trian.booking.model.SeatBooking;
import com.trian.booking.model.Station;
import com.trian.booking.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatBookingRepository extends JpaRepository<SeatBooking, Long> {

    @Query("SELECT b FROM SeatBooking b WHERE b.seat = :seat AND b.origin.ordinal < :destinationOrdinal AND b.destination.ordinal > :originOrdinal")
    List<SeatBooking> findOverlappingBookings(
            @Param("seat") Seat seat,
            @Param("originOrdinal") int originOrdinal,
            @Param("destinationOrdinal") int destinationOrdinal);

    @Query("SELECT b FROM SeatBooking b WHERE b.seat = :seat ORDER BY b.origin.ordinal")
    List<SeatBooking> findBySeatOrderByOriginOrdinal(@Param("seat") Seat seat);
}
