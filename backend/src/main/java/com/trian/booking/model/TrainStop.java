package com.trian.booking.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalTime;

@Entity
@Table(name = "train_stops")
public class TrainStop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "train_id")
    private Train train;

    @ManyToOne
    @JoinColumn(name = "station_id")
    private Station station;

    private LocalTime approximateTime;

    public TrainStop() {
    }

    public TrainStop(Train train, Station station, LocalTime approximateTime) {
        this.train = train;
        this.station = station;
        this.approximateTime = approximateTime;
    }

    public Long getId() {
        return id;
    }

    public Train getTrain() {
        return train;
    }

    public void setTrain(Train train) {
        this.train = train;
    }

    public Station getStation() {
        return station;
    }

    public void setStation(Station station) {
        this.station = station;
    }

    public LocalTime getApproximateTime() {
        return approximateTime;
    }

    public void setApproximateTime(LocalTime approximateTime) {
        this.approximateTime = approximateTime;
    }
}
