package com.vehiclebooking.backend.config;

import com.vehiclebooking.backend.model.User;
import com.vehiclebooking.backend.model.Vehicle;
import com.vehiclebooking.backend.model.enums.Role;
import com.vehiclebooking.backend.model.enums.VehicleType;
import com.vehiclebooking.backend.repository.UserRepository;
import com.vehiclebooking.backend.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

        private final UserRepository userRepository;
        private final VehicleRepository vehicleRepository;
        private final PasswordEncoder passwordEncoder;

        @Override
        public void run(String... args) throws Exception {
                if (userRepository.count() == 0) {
                        // Create Admin/Driver
                        User driver = User.builder()
                                        .fullName("Ramesh Driver")
                                        .phoneNumber("9876543210")
                                        .password(passwordEncoder.encode("password123")) // Default password
                                        .role(Role.DRIVER)
                                        .isVerified(true)
                                        .build();
                        userRepository.save(driver);

                        // Create Vehicles
                        List<Vehicle> vehicles = List.of(
                                        Vehicle.builder()
                                                        .owner(driver)
                                                        .type(VehicleType.CAR)
                                                        .model("Toyota Innova Crysta")
                                                        .numberPlate("KA-01-AB-1234")
                                                        .capacity(7)
                                                        .pricePerKm(new BigDecimal("15.0"))
                                                        .pricePerDay(new BigDecimal("3500.0"))
                                                        .isAvailable(true)
                                                        .latitude(12.9716)
                                                        .longitude(77.5946)
                                                        .build(),
                                        Vehicle.builder()
                                                        .owner(driver)
                                                        .type(VehicleType.JCB)
                                                        .model("JCB 3DX")
                                                        .numberPlate("KA-05-XY-9876")
                                                        .capacity(1)
                                                        .pricePerHour(new BigDecimal("800.0")) // JCB usually hourly
                                                        .pricePerDay(new BigDecimal("8000.0"))
                                                        .isAvailable(true)
                                                        .latitude(12.9716)
                                                        .longitude(77.5946)
                                                        .build(),
                                        Vehicle.builder()
                                                        .owner(driver)
                                                        .type(VehicleType.TRACTOR)
                                                        .model("Mahindra Arjun")
                                                        .numberPlate("KA-04-TR-5555")
                                                        .capacity(1)
                                                        .pricePerHour(new BigDecimal("500.0"))
                                                        .isAvailable(true)
                                                        .latitude(12.9716)
                                                        .longitude(77.5946)
                                                        .build());

                        vehicleRepository.saveAll(vehicles);
                        System.out.println("Data Seeding Completed!");
                }
        }
}
