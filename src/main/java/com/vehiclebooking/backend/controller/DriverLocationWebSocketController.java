package com.vehiclebooking.backend.controller;

import com.vehiclebooking.backend.dto.DriverLocationUpdate;
import com.vehiclebooking.backend.service.DriverLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class DriverLocationWebSocketController {

    private final DriverLocationService driverLocationService;

    @MessageMapping("/driver/location")
    public void updateLocation(DriverLocationUpdate update) {
        if (update.getDriverId() != null) {
            driverLocationService.updateLocation(
                    update.getDriverId(),
                    update.getLatitude(),
                    update.getLongitude(),
                    update.getHeading());
        }
    }
}
