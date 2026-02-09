package com.vehiclebooking.backend.controller;

import com.vehiclebooking.backend.model.PricingConfig;
import com.vehiclebooking.backend.service.PricingConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pricing-config")
@CrossOrigin(origins = "*")
public class PricingConfigController {

    @Autowired
    private PricingConfigService pricingConfigService;

    @GetMapping
    public ResponseEntity<List<PricingConfig>> getAllConfigs() {
        return ResponseEntity.ok(pricingConfigService.getAllConfigs());
    }

    @GetMapping("/map")
    public ResponseEntity<Map<String, BigDecimal>> getAllConfigsAsMap() {
        return ResponseEntity.ok(pricingConfigService.getAllConfigsAsMap());
    }

    @GetMapping("/{key}")
    public ResponseEntity<PricingConfig> getConfigByKey(@PathVariable String key) {
        return ResponseEntity.ok(pricingConfigService.getConfigByKey(key));
    }

    @PutMapping("/{key}")
    public ResponseEntity<PricingConfig> updateConfig(
            @PathVariable String key,
            @RequestBody Map<String, BigDecimal> payload) {
        BigDecimal value = payload.get("value");
        return ResponseEntity.ok(pricingConfigService.updateConfig(key, value));
    }

    @PostMapping("/initialize")
    public ResponseEntity<String> initializeDefaults() {
        pricingConfigService.initializeDefaultConfigs();
        return ResponseEntity.ok("Default configurations initialized");
    }
}
