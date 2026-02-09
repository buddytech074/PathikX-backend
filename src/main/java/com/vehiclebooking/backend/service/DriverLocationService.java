package com.vehiclebooking.backend.service;

import com.vehiclebooking.backend.model.DriverLocation;

public interface DriverLocationService {
    void updateLocation(Long driverId, double lat, double lng, Double heading);

    DriverLocation getDriverLocation(Long driverId);
}
