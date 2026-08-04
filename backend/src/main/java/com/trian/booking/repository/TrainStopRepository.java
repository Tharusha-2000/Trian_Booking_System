package com.trian.booking.repository;

import com.trian.booking.model.Station;
import com.trian.booking.model.TrainStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrainStopRepository extends JpaRepository<TrainStop, Long> {
    List<TrainStop> findAllByOrderByStation_Ordinal();

    Optional<TrainStop> findByStation(Station station);
}
