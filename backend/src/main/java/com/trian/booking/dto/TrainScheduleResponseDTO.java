package com.trian.booking.dto;

import java.util.List;

public class TrainScheduleResponseDTO {
    private String trainName;
    private List<StopDTO> stops;

    public TrainScheduleResponseDTO() {
    }

    public TrainScheduleResponseDTO(String trainName, List<StopDTO> stops) {
        this.trainName = trainName;
        this.stops = stops;
    }

    public String getTrainName() {
        return trainName;
    }

    public void setTrainName(String trainName) {
        this.trainName = trainName;
    }

    public List<StopDTO> getStops() {
        return stops;
    }

    public void setStops(List<StopDTO> stops) {
        this.stops = stops;
    }

    public static class StopDTO {
        private String stationCode;
        private String stationName;
        private String approximateTime;

        public StopDTO() {
        }

        public StopDTO(String stationCode, String stationName, String approximateTime) {
            this.stationCode = stationCode;
            this.stationName = stationName;
            this.approximateTime = approximateTime;
        }

        public String getStationCode() {
            return stationCode;
        }

        public void setStationCode(String stationCode) {
            this.stationCode = stationCode;
        }

        public String getStationName() {
            return stationName;
        }

        public void setStationName(String stationName) {
            this.stationName = stationName;
        }

        public String getApproximateTime() {
            return approximateTime;
        }

        public void setApproximateTime(String approximateTime) {
            this.approximateTime = approximateTime;
        }
    }
}
