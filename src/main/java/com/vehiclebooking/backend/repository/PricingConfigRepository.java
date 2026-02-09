package com.vehiclebooking.backend.repository;

import com.vehiclebooking.backend.model.PricingConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PricingConfigRepository extends JpaRepository<PricingConfig, Long> {
    Optional<PricingConfig> findByConfigKey(String configKey);

    List<PricingConfig> findByCategory(String category);
}
