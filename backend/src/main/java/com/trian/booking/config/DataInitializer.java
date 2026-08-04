package com.trian.booking.config;

import com.trian.booking.model.Coach;
import com.trian.booking.model.Role;
import com.trian.booking.model.Seat;
import com.trian.booking.model.Station;
import com.trian.booking.model.Train;
import com.trian.booking.model.TrainStop;
import com.trian.booking.model.User;
import com.trian.booking.repository.CoachRepository;
import com.trian.booking.repository.SeatRepository;
import com.trian.booking.repository.StationRepository;
import com.trian.booking.repository.TrainRepository;
import com.trian.booking.repository.TrainStopRepository;
import com.trian.booking.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalTime;
import java.util.List;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(
            StationRepository stationRepository,
            CoachRepository coachRepository,
            SeatRepository seatRepository,
            TrainRepository trainRepository,
            TrainStopRepository trainStopRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {
                userRepository.save(new User("admin@trian.com", passwordEncoder.encode("admin123"), Role.ADMIN));
            }

            if (stationRepository.count() == 0) {
                stationRepository.saveAll(List.of(
                        new Station("CF", "Colombo Fort", 0),
                        new Station("GAM", "Gampaha", 1),
                        new Station("KDY", "Kandy", 2),
                        new Station("NP", "Nuwara Eliya (Nanu Oya)", 3),
                        new Station("BDL", "Badulla", 4)
                ));
            }

            if (trainRepository.count() == 0) {
                Train train = trainRepository.save(new Train("Udarata Kumari"));
                trainStopRepository.saveAll(List.of(
                        new TrainStop(train, stationRepository.findByCode("CF").orElseThrow(), LocalTime.of(12, 45)),
                        new TrainStop(train, stationRepository.findByCode("GAM").orElseThrow(), LocalTime.of(13, 45)),
                        new TrainStop(train, stationRepository.findByCode("KDY").orElseThrow(), LocalTime.of(15, 45)),
                        new TrainStop(train, stationRepository.findByCode("NP").orElseThrow(), LocalTime.of(16, 45)),
                        new TrainStop(train, stationRepository.findByCode("BDL").orElseThrow(), LocalTime.of(18, 0))
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
