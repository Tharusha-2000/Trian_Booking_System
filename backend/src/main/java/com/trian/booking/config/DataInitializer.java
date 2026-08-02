package com.trian.booking.config;

import com.trian.booking.model.Coach;
import com.trian.booking.model.Seat;
import com.trian.booking.model.Station;
import com.trian.booking.repository.CoachRepository;
import com.trian.booking.repository.SeatRepository;
import com.trian.booking.repository.StationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(
            StationRepository stationRepository,
            CoachRepository coachRepository,
            SeatRepository seatRepository) {
        return args -> {
            if (stationRepository.count() == 0) {
                stationRepository.saveAll(List.of(
                        new Station("CF", "Colombo Fort", 0),
                        new Station("KDY", "Kandy", 1),
                        new Station("NP", "Nuwara Eliya (Nanu Oya)", 2),
                        new Station("BDL", "Badulla", 3)
                ));
            }

            if (coachRepository.count() == 0) {
                Coach reserved1 = coachRepository.save(new Coach("R1", 1, true));
                Coach reserved2 = coachRepository.save(new Coach("R2", 2, true));
                Coach reserved3 = coachRepository.save(new Coach("R3", 3, true));
                Coach unreserved1 = coachRepository.save(new Coach("U1", 4, false));
                Coach unreserved2 = coachRepository.save(new Coach("U2", 5, false));
                Coach unreserved3 = coachRepository.save(new Coach("U3", 6, false));
                Coach unreserved4 = coachRepository.save(new Coach("U4", 7, false));
                Coach unreserved5 = coachRepository.save(new Coach("U5", 8, false));

                for (int seatNumber = 1; seatNumber <= 20; seatNumber++) {
                    seatRepository.save(new Seat(reserved1, String.format("R1-%02d", seatNumber)));
                }
                for (int seatNumber = 1; seatNumber <= 20; seatNumber++) {
                    seatRepository.save(new Seat(reserved2, String.format("R2-%02d", seatNumber)));
                }
                for (int seatNumber = 1; seatNumber <= 20; seatNumber++) {
                    seatRepository.save(new Seat(reserved3, String.format("R3-%02d", seatNumber)));
                }
                for (int seatNumber = 1; seatNumber <= 20; seatNumber++) {
                    seatRepository.save(new Seat(unreserved1, String.format("U1-%02d", seatNumber)));
                }
                for (int seatNumber = 1; seatNumber <= 20; seatNumber++) {
                    seatRepository.save(new Seat(unreserved2, String.format("U2-%02d", seatNumber)));
                }
                for (int seatNumber = 1; seatNumber <= 20; seatNumber++) {
                    seatRepository.save(new Seat(unreserved3, String.format("U3-%02d", seatNumber)));
                }
                for (int seatNumber = 1; seatNumber <= 20; seatNumber++) {
                    seatRepository.save(new Seat(unreserved4, String.format("U4-%02d", seatNumber)));
                }
                for (int seatNumber = 1; seatNumber <= 20; seatNumber++) {
                    seatRepository.save(new Seat(unreserved5, String.format("U5-%02d", seatNumber)));
                }
            }
        };
    }
}
