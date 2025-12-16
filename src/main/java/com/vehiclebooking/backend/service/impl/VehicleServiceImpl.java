package com.vehiclebooking.backend.service.impl;

import com.vehiclebooking.backend.dto.VehicleDto;
import com.vehiclebooking.backend.exception.ResourceNotFoundException;
import com.vehiclebooking.backend.model.User;
import com.vehiclebooking.backend.model.Vehicle;
import com.vehiclebooking.backend.model.enums.VehicleType;
import com.vehiclebooking.backend.repository.UserRepository;
import com.vehiclebooking.backend.repository.BookingRepository;
import com.vehiclebooking.backend.repository.VehicleRepository;
import com.vehiclebooking.backend.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.vehiclebooking.backend.model.enums.BookingStatus;
import java.time.LocalDateTime;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    @Override
    public Vehicle addVehicle(Long ownerId, VehicleDto vehicleDto) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Owner not found"));

        Vehicle vehicle = Vehicle.builder()
                .owner(owner)
                .type(vehicleDto.getType())
                .model(vehicleDto.getModel())
                .numberPlate(vehicleDto.getNumberPlate())
                .images(vehicleDto.getImages())
                .capacity(vehicleDto.getCapacity())
                .pricePerKm(vehicleDto.getPricePerKm())
                .pricePerHour(vehicleDto.getPricePerHour())
                .pricePerDay(vehicleDto.getPricePerDay())
                .isAvailable(vehicleDto.isAvailable())
                .latitude(vehicleDto.getLatitude())
                .longitude(vehicleDto.getLongitude())
                .build();

        return vehicleRepository.save(vehicle);
    }

    @Override
    public List<Vehicle> getVehiclesByType(VehicleType type) {
        return vehicleRepository.findByType(type);
    }

    @Override
    public List<Vehicle> findNearbyVehicles(double lat, double lng, double radius) {
        // Using custom query - converted radius to reasonable logic if needed or
        // relying on DB
        // Calling repository method
        return vehicleRepository.findNearbyVehicles(lat, lng, radius);
    }

    @Override
    public List<Vehicle> searchVehicles(VehicleType type, LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return vehicleRepository.findByType(type);
        }

        List<Long> busyIds = bookingRepository.findBusyVehicleIds(
                List.of(BookingStatus.PENDING, BookingStatus.ACCEPTED), start, end);

        List<Vehicle> allVehicles = vehicleRepository.findByType(type);

        if (busyIds.isEmpty()) {
            return allVehicles.stream().filter(Vehicle::isAvailable).toList();
        }

        return allVehicles.stream()
                .filter(v -> !busyIds.contains(v.getId()))
                .filter(Vehicle::isAvailable)
                .toList();
    }

    @Override
    public Vehicle getVehicleById(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));
    }

    @Override
    public List<Vehicle> getMyVehicles(Long ownerId) {
        // Returns vehicles where user is owner OR assigned driver
        return vehicleRepository.findByOwnerIdOrAssignedDriverId(ownerId, ownerId);
    }

    @Override
    public Vehicle updateVehicleStatus(Long vehicleId, boolean isAvailable) {
        Vehicle vehicle = getVehicleById(vehicleId);
        vehicle.setAvailable(isAvailable);
        return vehicleRepository.save(vehicle);
    }

    @Override
    public Vehicle assignDriver(Long vehicleId, Long ownerId, String driverPhoneNumber) {
        Vehicle vehicle = getVehicleById(vehicleId);

        // Use Long.equals to compare object Longs safely
        if (!vehicle.getOwner().getId().equals(ownerId)) {
            throw new RuntimeException("Unauthorized: You are not the owner of this vehicle");
        }

        if (driverPhoneNumber == null || driverPhoneNumber.trim().isEmpty()) {
            // Unassign logic
            vehicle.setAssignedDriver(null);
            return vehicleRepository.save(vehicle);
        }

        User driver = userRepository.findByPhoneNumber(driverPhoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with phone: " + driverPhoneNumber));

        // In a real app, verify role is DRIVER
        // if (driver.getRole() != Role.DRIVER) throw ...

        // Check if driver is already assigned to another vehicle
        if (vehicleRepository.existsByAssignedDriverId(driver.getId())) {
            throw new RuntimeException("Driver is already assigned to another vehicle");
        }

        vehicle.setAssignedDriver(driver);
        return vehicleRepository.save(vehicle);
    }

    @Override
    public Vehicle unassignDriver(Long vehicleId, Long ownerId) {
        Vehicle vehicle = getVehicleById(vehicleId);

        if (!vehicle.getOwner().getId().equals(ownerId)) {
            throw new RuntimeException("Unauthorized: You are not the owner of this vehicle");
        }

        vehicle.setAssignedDriver(null);
        return vehicleRepository.save(vehicle);
    }
}
