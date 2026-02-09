package com.vehiclebooking.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingStopDto {
    private String location;
    private double latitude;
    private double longitude;
    private int stopOrder;
}
