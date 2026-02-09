package com.vehiclebooking.backend.controller;

import com.vehiclebooking.backend.dto.ApiResponse;
import com.vehiclebooking.backend.dto.VehicleDto;
import com.vehiclebooking.backend.model.Vehicle;
import com.vehiclebooking.backend.model.enums.VehicleType;
import com.vehiclebooking.backend.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/vehicles")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<Vehicle>> addVehicle(@RequestParam Long ownerId,
            @RequestBody VehicleDto vehicleDto) {
        Vehicle vehicle = vehicleService.addVehicle(ownerId, vehicleDto);
        return ResponseEntity.ok(ApiResponse.success(vehicle, "Vehicle Added Successfully"));
    }

    @GetMapping("/by-type/{type}")
    public ResponseEntity<ApiResponse<List<Vehicle>>> getVehiclesByType(@PathVariable VehicleType type) {
        List<Vehicle> vehicles = vehicleService.getVehiclesByType(type);
        return ResponseEntity.ok(ApiResponse.success(vehicles, "Vehicles Fetched"));
    }

    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<Vehicle>>> getNearbyVehicles(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10.0") double radius) {
        List<Vehicle> vehicles = vehicleService.findNearbyVehicles(lat, lng, radius);
        return ResponseEntity.ok(ApiResponse.success(vehicles, "Nearby vehicles fetched successfully"));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Vehicle>>> searchVehicles(
            @RequestParam VehicleType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        List<Vehicle> vehicles = vehicleService.searchVehicles(type, start, end);
        return ResponseEntity.ok(ApiResponse.success(vehicles, "Available vehicles fetched"));
    }

    @GetMapping("/vehicle/{id}")
    public ResponseEntity<ApiResponse<Vehicle>> getVehicleById(@PathVariable Long id) {
        Vehicle vehicle = vehicleService.getVehicleById(id);
        return ResponseEntity.ok(ApiResponse.success(vehicle, "Vehicle Fetched"));
    }

    @PostMapping("/{id}/assign-driver")
    public ResponseEntity<ApiResponse<Vehicle>> assignDriver(
            @PathVariable Long id,
            @RequestParam Long ownerId,
            @RequestParam String driverPhoneNumber) {
        Vehicle vehicle = vehicleService.assignDriver(id, ownerId, driverPhoneNumber);
        return ResponseEntity.ok(ApiResponse.success(vehicle, "Driver assigned successfully"));
    }

    @PostMapping("/{id}/unassign-driver")
    public ResponseEntity<ApiResponse<Vehicle>> unassignDriver(
            @PathVariable Long id,
            @RequestParam Long ownerId) {
        Vehicle vehicle = vehicleService.unassignDriver(id, ownerId);
        return ResponseEntity.ok(ApiResponse.success(vehicle, "Driver unassigned successfully"));
    }

    @GetMapping("/active-drivers")
    public ResponseEntity<ApiResponse<List<com.vehiclebooking.backend.dto.ActiveDriverDto>>> getActiveDrivers() {
        List<com.vehiclebooking.backend.dto.ActiveDriverDto> activeDrivers = vehicleService.getActiveDrivers();
        return ResponseEntity.ok(ApiResponse.success(activeDrivers, "Active drivers fetched successfully"));
    }
}
