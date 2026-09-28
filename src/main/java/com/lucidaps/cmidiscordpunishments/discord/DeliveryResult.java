package com.lucidaps.cmidiscordpunishments.discord;

public record DeliveryResult(DeliveryStatus status, int httpStatus, String message) {
    public boolean successful() {
        return status == DeliveryStatus.DELIVERED;
    }
}
