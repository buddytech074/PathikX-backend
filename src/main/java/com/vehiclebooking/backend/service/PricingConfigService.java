package com.vehiclebooking.backend.service;

import com.vehiclebooking.backend.model.PricingConfig;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface PricingConfigService {
    List<PricingConfig> getAllConfigs();

    PricingConfig getConfigByKey(String key);

    PricingConfig updateConfig(String key, BigDecimal value);

    Map<String, BigDecimal> getAllConfigsAsMap();

    void initializeDefaultConfigs();
}
