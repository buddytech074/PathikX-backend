package com.vehiclebooking.backend.service;

import com.vehiclebooking.backend.model.enums.TripType;
import com.vehiclebooking.backend.model.enums.VehicleType;
import com.vehiclebooking.backend.repository.PricingConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PricingService {

    @Autowired
    private PricingConfigRepository pricingConfigRepository;

    // Cache for pricing configs to avoid database hits on every calculation
    private final Map<String, BigDecimal> configCache = new ConcurrentHashMap<>();

    /**
     * Get configuration value with caching
     */
    private double getConfig(String key, double defaultValue) {
        if (configCache.isEmpty()) {
            refreshCache();
        }
        return configCache.getOrDefault(key, BigDecimal.valueOf(defaultValue)).doubleValue();
    }

    /**
     * Refresh configuration cache from database
     */
    public void refreshCache() {
        try {
            pricingConfigRepository.findAll()
                    .forEach(config -> configCache.put(config.getConfigKey(), config.getConfigValue()));
        } catch (Exception e) {
            // Database might not be initialized yet
            System.out.println("⚠️ Could not load pricing config from database, using defaults");
        }
    }

    /**
     * Calculate estimated distance using Haversine formula
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = R * c;

        // Approximate Road Distance: Multiply air distance by 1.4
        // to account for road turns and indirect routes in city driving.
        distance = distance * 1.4;

        return Math.round(distance * 100.0) / 100.0; // Round to 2 decimal places
    }

    /**
     * Calculate base fare with advanced logic (Round Trip & Rental)
     */
    public BigDecimal calculateFare(double distance, int passengerCount, VehicleType vehicleType, TripType tripType,
            long durationHours, int stopCount) {
        // 1. Handle Round Trip Logic
        double actualDistance = distance;
        if (tripType == TripType.ROUND_TRIP) {
            actualDistance = distance * 2;
        }

        // 2. Identify Rate Per Km & Minimum Fare based on Vehicle Type
        double basePricePerKm;
        double minimumFare;

        if (vehicleType == VehicleType.BIKE) {
            basePricePerKm = getConfig("RATE_PER_KM_BIKE", 6.0);
            minimumFare = getConfig("MINIMUM_FARE_BIKE", 20.0);
        } else if (vehicleType == VehicleType.AUTO) {
            basePricePerKm = getConfig("RATE_PER_KM_AUTO", 8.0);
            minimumFare = getConfig("MINIMUM_FARE_AUTO", 30.0);
        } else if (vehicleType == VehicleType.SEDAN) {
            basePricePerKm = getConfig("RATE_PER_KM_SEDAN", 12.0);
            minimumFare = getConfig("MINIMUM_FARE_SEDAN", 50.0);
        } else if (vehicleType == VehicleType.SUV) {
            basePricePerKm = getConfig("RATE_PER_KM_SUV", 15.0);
            minimumFare = getConfig("MINIMUM_FARE_SUV", 60.0);
        } else if (vehicleType == VehicleType.SAFARI) {
            basePricePerKm = getConfig("RATE_PER_KM_SAFARI", 18.0);
            minimumFare = getConfig("MINIMUM_FARE_SAFARI", 80.0);
        } else if (vehicleType == VehicleType.EV) {
            basePricePerKm = getConfig("RATE_PER_KM_EV_RESERVE", 15.0);
            minimumFare = getConfig("MINIMUM_FARE_EV_RESERVE", 100.0);
        } else {
            // Default fallback to SEDAN pricing
            basePricePerKm = getConfig("RATE_PER_KM_SEDAN", 12.0);
            minimumFare = getConfig("MINIMUM_FARE_SEDAN", 50.0);
        }

        // 3. Rental Logic (> 5 Hours)
        int rentalThreshold = (int) getConfig("RENTAL_DURATION_THRESHOLD_HOURS", 5.0);
        if (durationHours > rentalThreshold) {
            BigDecimal fuelCost = BigDecimal.valueOf(actualDistance * basePricePerKm);
            BigDecimal rentalVehicleCost = BigDecimal.valueOf(getConfig("RENTAL_VEHICLE_COST", 1000.0));
            BigDecimal rentalDriverCost = BigDecimal.valueOf(getConfig("RENTAL_DRIVER_COST", 600.0));
            BigDecimal totalRentalCost = fuelCost.add(rentalVehicleCost).add(rentalDriverCost);
            return totalRentalCost.setScale(2, RoundingMode.HALF_UP);
        }

        // 4. Standard Distance-Based Logic
        double baseFare = actualDistance * basePricePerKm;
        baseFare = Math.max(baseFare, minimumFare);

        return BigDecimal.valueOf(baseFare).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Specialized fare calculation for EV (Toto) and Shared rides
     */
    public BigDecimal calculateFareWithSharing(double distance, int passengerCount, VehicleType vehicleType,
            TripType tripType, boolean isShared, int stopCount) {

        double actualDistance = (tripType == TripType.ROUND_TRIP) ? distance * 2 : distance;
        double basePricePerKm;
        double minimumFare;

        if (vehicleType == VehicleType.EV) {
            if (isShared) {
                basePricePerKm = getConfig("RATE_PER_KM_EV_PARTNER", 3.0);
                minimumFare = getConfig("MINIMUM_FARE_EV_PARTNER", 10.0);
            } else {
                basePricePerKm = getConfig("RATE_PER_KM_EV_RESERVE", 15.0);
                minimumFare = getConfig("MINIMUM_FARE_EV_RESERVE", 100.0);
            }
        } else if (vehicleType == VehicleType.BIKE) {
            basePricePerKm = getConfig("RATE_PER_KM_BIKE", 6.0);
            minimumFare = getConfig("MINIMUM_FARE_BIKE", 20.0);
        } else if (vehicleType == VehicleType.AUTO) {
            basePricePerKm = getConfig("BASE_PRICE_PER_KM_AUTO", 8.0);
            minimumFare = getConfig("MINIMUM_FARE_AUTO", 30.0);
        } else {
            basePricePerKm = getConfig("RATE_PER_KM_SEDAN", 12.0);
            minimumFare = getConfig("MINIMUM_FARE_SEDAN", 50.0);
        }

        double baseFare = actualDistance * basePricePerKm;
        baseFare = Math.max(baseFare, minimumFare);

        return BigDecimal.valueOf(baseFare).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate base fare without variations (Legacy/Simple support)
     */
    public BigDecimal calculateBaseFare(double distance, int passengerCount) {
        return calculateFare(distance, passengerCount, null, TripType.ONE_WAY, 0, 0);
    }

    /**
     * Calculate base fare with vehicle type consideration (for AUTO) -
     * Legacy/Simple support
     */
    public BigDecimal calculateBaseFare(double distance, int passengerCount, VehicleType vehicleType) {
        return calculateFare(distance, passengerCount, vehicleType, TripType.ONE_WAY, 0, 0);
    }

    /**
     * Calculate minimum estimated price (base - 20%)
     */
    public BigDecimal calculateMinEstimatedPrice(BigDecimal baseFare) {
        double priceVariation = getConfig("PRICE_VARIATION", 0.20);
        double minPrice = baseFare.doubleValue() * (1 - priceVariation);
        return BigDecimal.valueOf(minPrice).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate maximum estimated price (base + 20%)
     */
    public BigDecimal calculateMaxEstimatedPrice(BigDecimal baseFare) {
        double priceVariation = getConfig("PRICE_VARIATION", 0.20);
        double maxPrice = baseFare.doubleValue() * (1 + priceVariation);
        return BigDecimal.valueOf(maxPrice).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate platform charge
     */
    public BigDecimal calculatePlatformCharge(BigDecimal totalAmount) {
        double platformPercentage = getConfig("PLATFORM_CHARGE_PERCENTAGE", 0.15);
        double charge = totalAmount.doubleValue() * platformPercentage;
        return BigDecimal.valueOf(charge).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate remaining amount after platform charge
     */
    public BigDecimal calculateRemainingAmount(BigDecimal totalAmount, BigDecimal platformCharge) {
        return totalAmount.subtract(platformCharge);
    }

    /**
     * Calculate actual price when driver accepts
     * Uses vehicle's pricePerKm if available, otherwise uses base price
     * Handles shared rides correctly (especially EV Partnership vs Reserve)
     */
    public BigDecimal calculateActualPrice(double distance, int passengerCount, BigDecimal vehiclePricePerKm,
            TripType tripType, VehicleType vehicleType, Boolean isShared) {
        double actualDistance = (tripType == TripType.ROUND_TRIP) ? distance * 2 : distance;

        // For EV vehicles, pricing depends on whether it's shared or reserved
        if (vehicleType == VehicleType.EV) {
            boolean shared = (isShared != null && isShared);
            double basePricePerKm = shared ? getConfig("RATE_PER_KM_EV_PARTNER", 3.0)
                    : getConfig("RATE_PER_KM_EV_RESERVE", 15.0);
            double minimumFare = shared ? getConfig("MINIMUM_FARE_EV_PARTNER", 10.0)
                    : getConfig("MINIMUM_FARE_EV_RESERVE", 100.0);

            double actualPrice = actualDistance * basePricePerKm;
            actualPrice = Math.max(actualPrice, minimumFare);

            System.out.println("💰 EV PRICING - Shared: " + shared +
                    ", Rate: ₹" + basePricePerKm + "/km" +
                    ", Distance: " + actualDistance + "km" +
                    ", Price: ₹" + actualPrice);

            return BigDecimal.valueOf(actualPrice).setScale(2, RoundingMode.HALF_UP);
        }

        if (vehiclePricePerKm != null && vehiclePricePerKm.compareTo(BigDecimal.ZERO) > 0) {
            // Use vehicle's custom pricing
            double actualPrice = actualDistance * vehiclePricePerKm.doubleValue();
            double minimumFare = getConfig("MINIMUM_FARE", 50.0);
            actualPrice = Math.max(actualPrice, minimumFare);
            return BigDecimal.valueOf(actualPrice).setScale(2, RoundingMode.HALF_UP);
        } else {
            // Use base fare calculation
            return calculateBaseFare(actualDistance, passengerCount, vehicleType);
        }
    }
}
