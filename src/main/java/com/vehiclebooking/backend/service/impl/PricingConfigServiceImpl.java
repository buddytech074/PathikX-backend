package com.vehiclebooking.backend.service.impl;

import com.vehiclebooking.backend.exception.ResourceNotFoundException;
import com.vehiclebooking.backend.model.PricingConfig;
import com.vehiclebooking.backend.repository.PricingConfigRepository;
import com.vehiclebooking.backend.service.PricingConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PricingConfigServiceImpl implements PricingConfigService {

    @Autowired
    private PricingConfigRepository pricingConfigRepository;

    @PostConstruct
    public void init() {
        // Initialize default configs if database is empty
        if (pricingConfigRepository.count() == 0) {
            initializeDefaultConfigs();
        }
    }

    @Override
    public List<PricingConfig> getAllConfigs() {
        return pricingConfigRepository.findAll();
    }

    @Override
    public PricingConfig getConfigByKey(String key) {
        return pricingConfigRepository.findByConfigKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Pricing config not found: " + key));
    }

    @Override
    @Transactional
    public PricingConfig updateConfig(String key, BigDecimal value) {
        PricingConfig config = getConfigByKey(key);
        config.setConfigValue(value);
        config.setUpdatedAt(LocalDateTime.now());
        return pricingConfigRepository.save(config);
    }

    @Override
    public Map<String, BigDecimal> getAllConfigsAsMap() {
        Map<String, BigDecimal> configMap = new HashMap<>();
        List<PricingConfig> configs = getAllConfigs();
        for (PricingConfig config : configs) {
            configMap.put(config.getConfigKey(), config.getConfigValue());
        }
        return configMap;
    }

    @Override
    @Transactional
    public void initializeDefaultConfigs() {
        System.out.println("📋 Initializing default pricing configurations...");

        // Base Rates - Per Vehicle Type
        createConfig("RATE_PER_KM_BIKE", "6.0", "Rate per km for Bike", "BASE_RATES");
        createConfig("RATE_PER_KM_AUTO", "8.0", "Rate per km for Auto-rickshaw", "BASE_RATES");
        createConfig("RATE_PER_KM_SEDAN", "12.0", "Rate per km for Sedan", "BASE_RATES");
        createConfig("RATE_PER_KM_SUV", "15.0", "Rate per km for SUV", "BASE_RATES");
        createConfig("RATE_PER_KM_SAFARI", "18.0", "Rate per km for Safari", "BASE_RATES");
        createConfig("RATE_PER_KM_EV_PARTNER", "3.0", "Rate per km for EV partnership (shared)", "BASE_RATES");
        createConfig("RATE_PER_KM_EV_RESERVE", "15.0", "Rate per km for EV reserved", "BASE_RATES");

        // Minimum Fares - Per Vehicle Type
        createConfig("MINIMUM_FARE_BIKE", "20.0", "Minimum fare for Bike", "MINIMUM_FARES");
        createConfig("MINIMUM_FARE_AUTO", "30.0", "Minimum fare for Auto-rickshaw", "MINIMUM_FARES");
        createConfig("MINIMUM_FARE_SEDAN", "50.0", "Minimum fare for Sedan", "MINIMUM_FARES");
        createConfig("MINIMUM_FARE_SUV", "60.0", "Minimum fare for SUV", "MINIMUM_FARES");
        createConfig("MINIMUM_FARE_SAFARI", "80.0", "Minimum fare for Safari", "MINIMUM_FARES");
        createConfig("MINIMUM_FARE_EV_PARTNER", "10.0", "Minimum fare for EV partnership", "MINIMUM_FARES");
        createConfig("MINIMUM_FARE_EV_RESERVE", "100.0", "Minimum fare for EV reserved", "MINIMUM_FARES");

        // Rental Pricing
        createConfig("RENTAL_VEHICLE_COST", "1000.0", "Vehicle cost for rental (>5 hours)", "RENTAL");
        createConfig("RENTAL_DRIVER_COST", "600.0", "Driver cost for rental (>5 hours)", "RENTAL");
        createConfig("RENTAL_DURATION_THRESHOLD_HOURS", "5.0", "Hours threshold for rental pricing", "RENTAL");

        // System Settings
        createConfig("PLATFORM_CHARGE_PERCENTAGE", "0.15", "Platform commission percentage (15%)", "SYSTEM");
        createConfig("PRICE_VARIATION", "0.20", "Price variation for estimates (±20%)", "SYSTEM");

        System.out.println("✅ Default pricing configurations initialized");
    }

    private void createConfig(String key, String value, String description, String category) {
        // Check if config already exists
        pricingConfigRepository.findByConfigKey(key).ifPresentOrElse(
                existingConfig -> {
                    // Update existing config (optional - comment this out if you don't want to
                    // overwrite)
                    // existingConfig.setConfigValue(new BigDecimal(value));
                    // existingConfig.setUpdatedAt(LocalDateTime.now());
                    // pricingConfigRepository.save(existingConfig);
                    System.out.println("⏭️  Config already exists, skipping: " + key);
                },
                () -> {
                    // Create new config
                    PricingConfig config = PricingConfig.builder()
                            .configKey(key)
                            .configValue(new BigDecimal(value))
                            .description(description)
                            .category(category)
                            .updatedAt(LocalDateTime.now())
                            .build();
                    pricingConfigRepository.save(config);
                    System.out.println("✅ Created new config: " + key);
                });
    }
}
