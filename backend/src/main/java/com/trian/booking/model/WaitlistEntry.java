package com.trian.booking.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "waitlist_entries")
public class WaitlistEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "seat_id")
    private Seat seat;

    @ManyToOne
    @JoinColumn(name = "origin_station_id")
    private Station origin;

    @ManyToOne
    @JoinColumn(name = "destination_station_id")
    private Station destination;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDate travelDate;
    private String passengerName;
    private LocalDateTime requestedAt;

    @Enumerated(EnumType.STRING)
    private WaitlistStatus status;

    public WaitlistEntry() {
    }

    public WaitlistEntry(Seat seat, Station origin, Station destination, User user, LocalDate travelDate, String passengerName) {
        this.seat = seat;
        this.origin = origin;
        this.destination = destination;
        this.user = user;
        this.travelDate = travelDate;
        this.passengerName = passengerName;
        this.requestedAt = LocalDateTime.now();
        this.status = WaitlistStatus.WAITING;
    }

    public Long getId() {
        return id;
    }

    public Seat getSeat() {
        return seat;
    }

    public void setSeat(Seat seat) {
        this.seat = seat;
    }

    public Station getOrigin() {
        return origin;
    }

    public void setOrigin(Station origin) {
        this.origin = origin;
    }

    public Station getDestination() {
        return destination;
    }

    public void setDestination(Station destination) {
        this.destination = destination;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDate getTravelDate() {
        return travelDate;
    }

    public void setTravelDate(LocalDate travelDate) {
        this.travelDate = travelDate;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public WaitlistStatus getStatus() {
        return status;
    }

    public void setStatus(WaitlistStatus status) {
        this.status = status;
    }
}
