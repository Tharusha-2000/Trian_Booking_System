package com.trian.booking.dto;

import java.time.LocalDate;

public class WaitlistResponseDTO {
    private Long id;
    private String seatNumber;
    private String coachCode;
    private String originCode;
    private String destinationCode;
    private LocalDate travelDate;
    private String status;
    private long position;

    public WaitlistResponseDTO() {
    }

    public WaitlistResponseDTO(Long id, String seatNumber, String coachCode, String originCode,
                                String destinationCode, LocalDate travelDate, String status, long position) {
        this.id = id;
        this.seatNumber = seatNumber;
        this.coachCode = coachCode;
        this.originCode = originCode;
        this.destinationCode = destinationCode;
        this.travelDate = travelDate;
        this.status = status;
        this.position = position;
    }

    public Long getId() {
        return id;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public String getCoachCode() {
        return coachCode;
    }

    public String getOriginCode() {
        return originCode;
    }

    public String getDestinationCode() {
        return destinationCode;
    }

    public LocalDate getTravelDate() {
        return travelDate;
    }

    public String getStatus() {
        return status;
    }

    public long getPosition() {
        return position;
    }
}
