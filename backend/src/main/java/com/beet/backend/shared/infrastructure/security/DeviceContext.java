package com.beet.backend.shared.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeviceContext {

    public static final String DEVICE_ID_HEADER = "X-Device-Id";

    private final HttpServletRequest request;

    public UUID getDeviceId() {
        String value = request.getHeader(DEVICE_ID_HEADER);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(DEVICE_ID_HEADER + " header is required");
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(DEVICE_ID_HEADER + " header must be a UUID");
        }
    }
}
