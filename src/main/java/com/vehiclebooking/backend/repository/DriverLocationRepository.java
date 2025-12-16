package com.vehiclebooking.backend.repository;

import com.vehiclebooking.backend.model.DriverLocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverLocationRepository extends JpaRepository<DriverLocation, Long> {
}
