package com.vehiclebooking.backend.service;

import com.vehiclebooking.backend.dto.VehicleDto;
import com.vehiclebooking.backend.model.Vehicle;
import com.vehiclebooking.backend.model.enums.VehicleType;
import java.time.LocalDateTime;
import java.util.List;

public interface VehicleService {
    Vehicle addVehicle(Long ownerId, VehicleDto vehicleDto);

    List<Vehicle> getVehiclesByType(VehicleType type);

    List<Vehicle> findNearbyVehicles(double lat, double lng, double radius);

    List<Vehicle> searchVehicles(VehicleType type, LocalDateTime start, LocalDateTime end);

    Vehicle getVehicleById(Long id);

    List<Vehicle> getMyVehicles(Long ownerId);

    Vehicle updateVehicleStatus(Long vehicleId, boolean isAvailable);

    Vehicle assignDriver(Long vehicleId, Long ownerId, String driverPhoneNumber);

    Vehicle unassignDriver(Long vehicleId, Long ownerId);
}
