package com.trian.booking.dto;

import java.time.LocalDate;
import java.util.List;

public class AdminStatsResponseDTO {
    private long totalRevenue;
    private long totalBookings;
    private LocalDate date;
    private long revenueForDate;
    private long bookingsForDate;
    private List<CoachOccupancyDTO> coachOccupancy;

    public AdminStatsResponseDTO() {
    }

    public AdminStatsResponseDTO(long totalRevenue, long totalBookings, LocalDate date,
                                  long revenueForDate, long bookingsForDate, List<CoachOccupancyDTO> coachOccupancy) {
        this.totalRevenue = totalRevenue;
        this.totalBookings = totalBookings;
        this.date = date;
        this.revenueForDate = revenueForDate;
        this.bookingsForDate = bookingsForDate;
        this.coachOccupancy = coachOccupancy;
    }

    public long getTotalRevenue() {
        return totalRevenue;
    }

    public long getTotalBookings() {
        return totalBookings;
    }

    public LocalDate getDate() {
        return date;
    }

    public long getRevenueForDate() {
        return revenueForDate;
    }

    public long getBookingsForDate() {
        return bookingsForDate;
    }

    public List<CoachOccupancyDTO> getCoachOccupancy() {
        return coachOccupancy;
    }

    public static class CoachOccupancyDTO {
        private String coachCode;
        private boolean reserved;
        private long totalSeats;
        private long bookedSeats;
        private double occupancyPercent;

        public CoachOccupancyDTO() {
        }

        public CoachOccupancyDTO(String coachCode, boolean reserved, long totalSeats, long bookedSeats, double occupancyPercent) {
            this.coachCode = coachCode;
            this.reserved = reserved;
            this.totalSeats = totalSeats;
            this.bookedSeats = bookedSeats;
            this.occupancyPercent = occupancyPercent;
        }

        public String getCoachCode() {
            return coachCode;
        }

        public boolean isReserved() {
            return reserved;
        }

        public long getTotalSeats() {
            return totalSeats;
        }

        public long getBookedSeats() {
            return bookedSeats;
        }

        public double getOccupancyPercent() {
            return occupancyPercent;
        }
    }
}
