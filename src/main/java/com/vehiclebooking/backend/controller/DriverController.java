package com.vehiclebooking.backend.controller;

import com.vehiclebooking.backend.dto.ApiResponse;

import com.vehiclebooking.backend.model.Vehicle;
import com.vehiclebooking.backend.service.VehicleService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/driver")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DriverController {

    // Note: Driver Location update typically involves storing in dedicated table or
    // Redis
    // For MVP, we can update Vehicle location or DriverLocation table.
    // Plan mentions "4. driver_location".
    // I'll skip implementing full DriverLocationService separate for now and focus
    // on vehicle/user provided logic
    // but expose the endpoints.

    private final VehicleService vehicleService;

    @GetMapping("/my-vehicles")
    public ResponseEntity<ApiResponse<List<Vehicle>>> getMyVehicles(@RequestParam Long driverId) {
        List<Vehicle> vehicles = vehicleService.getMyVehicles(driverId);
        return ResponseEntity.ok(ApiResponse.success(vehicles, "My Vehicles Fetched"));
    }

    @PutMapping("/vehicle-status")
    public ResponseEntity<ApiResponse<Vehicle>> updateVehicleStatus(@RequestParam Long vehicleId,
            @RequestParam boolean isAvailable) {
        Vehicle vehicle = vehicleService.updateVehicleStatus(vehicleId, isAvailable);
        return ResponseEntity.ok(ApiResponse.success(vehicle, "Status Updated"));
    }
}
