package com.trian.booking.dto;

public class SeatAvailabilityResponseDTO {
    private Long seatId;
    private String coachCode;
    private String seatNumber;
    private boolean reserved;
    private boolean available;

    public SeatAvailabilityResponseDTO() {
    }

    public SeatAvailabilityResponseDTO(Long seatId, String coachCode, String seatNumber, boolean reserved, boolean available) {
        this.seatId = seatId;
        this.coachCode = coachCode;
        this.seatNumber = seatNumber;
        this.reserved = reserved;
        this.available = available;
    }

    public Long getSeatId() {
        return seatId;
    }

    public void setSeatId(Long seatId) {
        this.seatId = seatId;
    }

    public String getCoachCode() {
        return coachCode;
    }

    public void setCoachCode(String coachCode) {
        this.coachCode = coachCode;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public boolean isReserved() {
        return reserved;
    }

    public void setReserved(boolean reserved) {
        this.reserved = reserved;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
