package com.vehiclebooking.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    private String type; // "NEW_BOOKING", "BOOKING_ACCEPTED", "BOOKING_CANCELLED"
    private Long bookingId;
    private String message;
    private Object data; // Can be booking details or other data
    private Long excludeDriverId; // Driver ID to exclude from this notification
}
