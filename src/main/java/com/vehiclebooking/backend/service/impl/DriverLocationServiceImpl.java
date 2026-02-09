package com.vehiclebooking.backend.service.impl;

import com.vehiclebooking.backend.model.DriverLocation;
import com.vehiclebooking.backend.repository.DriverLocationRepository;
import com.vehiclebooking.backend.service.DriverLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DriverLocationServiceImpl implements DriverLocationService {

    private final DriverLocationRepository driverLocationRepository;

    @Override
    @Transactional
    public void updateLocation(Long driverId, double lat, double lng, Double heading) {
        DriverLocation location = driverLocationRepository.findById(driverId)
                .orElse(new DriverLocation());

        location.setDriverId(driverId);
        location.setLatitude(lat);
        location.setLongitude(lng);
        location.setHeading(heading);

        driverLocationRepository.save(location);
    }

    @Override
    public DriverLocation getDriverLocation(Long driverId) {
        return driverLocationRepository.findById(driverId).orElse(null);
    }
}
