package com.vehiclebooking.backend.controller;

import com.vehiclebooking.backend.dto.ApiResponse;
import com.vehiclebooking.backend.dto.VehiclePricingDto;
import com.vehiclebooking.backend.model.VehiclePricing;
import com.vehiclebooking.backend.repository.VehiclePricingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/pricing")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminPricingController {

    private final VehiclePricingRepository pricingRepository;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<VehiclePricingDto>>> getAllPricing() {
        List<VehiclePricingDto> pricing = pricingRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(pricing, "Pricing fetched"));
    }

    @GetMapping("/{vehicleType}")
    public ResponseEntity<ApiResponse<VehiclePricingDto>> getPricingByType(@PathVariable String vehicleType) {
        VehiclePricing pricing = pricingRepository.findByVehicleType(vehicleType)
                .orElseThrow(() -> new RuntimeException("Pricing not found for vehicle type: " + vehicleType));
        return ResponseEntity.ok(ApiResponse.success(toDto(pricing), "Pricing fetched"));
    }

    @PostMapping("/update")
    public ResponseEntity<ApiResponse<VehiclePricingDto>> updatePricing(@RequestBody VehiclePricingDto dto) {
        VehiclePricing pricing = pricingRepository.findByVehicleType(dto.getVehicleType())
                .orElse(VehiclePricing.builder()
                        .vehicleType(dto.getVehicleType())
                        .build());

        pricing.setPricePerKm(dto.getPricePerKm());
        pricing.setPricePerHour(dto.getPricePerHour());
        pricing.setPricePerDay(dto.getPricePerDay());
        pricing.setReservationPrice(dto.getReservationPrice());
        pricing.setUpdatedAt(LocalDateTime.now());

        VehiclePricing saved = pricingRepository.save(pricing);

        System.out.println("✅ Pricing updated for " + dto.getVehicleType());
        System.out.println("   Per KM: ₹" + dto.getPricePerKm());
        System.out.println("   Per Hour: ₹" + dto.getPricePerHour());
        System.out.println("   Per Day: ₹" + dto.getPricePerDay());
        System.out.println("   Reservation: ₹" + dto.getReservationPrice());

        return ResponseEntity.ok(ApiResponse.success(toDto(saved), "Pricing updated successfully"));
    }

    @PostMapping("/init-defaults")
    public ResponseEntity<ApiResponse<String>> initializeDefaults() {
        initializePricingIfNotExists("CAR", 15.0, 100.0, 2000.0, null);
        initializePricingIfNotExists("BIKE", 8.0, 50.0, 800.0, null);
        initializePricingIfNotExists("JCB", 50.0, 500.0, 8000.0, null);
        initializePricingIfNotExists("TRUCK", 25.0, 300.0, 5000.0, null);
        initializePricingIfNotExists("EV", null, null, null, 500.0);

        return ResponseEntity.ok(ApiResponse.success("OK", "Default pricing initialized"));
    }

    private void initializePricingIfNotExists(String type, Double perKm, Double perHour, Double perDay,
            Double reservation) {
        if (!pricingRepository.findByVehicleType(type).isPresent()) {
            VehiclePricing pricing = VehiclePricing.builder()
                    .vehicleType(type)
                    .pricePerKm(perKm != null ? java.math.BigDecimal.valueOf(perKm) : null)
                    .pricePerHour(perHour != null ? java.math.BigDecimal.valueOf(perHour) : null)
                    .pricePerDay(perDay != null ? java.math.BigDecimal.valueOf(perDay) : null)
                    .reservationPrice(reservation != null ? java.math.BigDecimal.valueOf(reservation) : null)
                    .updatedAt(LocalDateTime.now())
                    .build();
            pricingRepository.save(pricing);
        }
    }

    private VehiclePricingDto toDto(VehiclePricing pricing) {
        return VehiclePricingDto.builder()
                .vehicleType(pricing.getVehicleType())
                .pricePerKm(pricing.getPricePerKm())
                .pricePerHour(pricing.getPricePerHour())
                .pricePerDay(pricing.getPricePerDay())
                .reservationPrice(pricing.getReservationPrice())
                .build();
    }
}
