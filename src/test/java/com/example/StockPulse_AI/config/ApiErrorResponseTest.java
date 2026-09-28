package com.example.StockPulse_AI.config;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ApiErrorResponseTest {

    @Test
    void constructorWithAllParameters_SetsAllFields() {
        // Arrange
        LocalDateTime timestamp = LocalDateTime.of(2023, 1, 1, 12, 0);
        int status = 400;
        String error = "Bad Request";
        String message = "Invalid input";

        // Act
        ApiErrorResponse response = new ApiErrorResponse(timestamp, status, error, message);

        // Assert
        assertEquals(timestamp, response.timestamp());
        assertEquals(status, response.status());
        assertEquals(error, response.error());
        assertEquals(message, response.message());
    }

    @Test
    void constructorWithStatusAndErrorAndMessage_SetsCurrentTimestamp() {
        // Arrange
        int status = 404;
        String error = "Not Found";
        String message = "Resource not found";

        // Act
        ApiErrorResponse response = new ApiErrorResponse(status, error, message);

        // Assert
        assertNotNull(response.timestamp());
        assertEquals(status, response.status());
        assertEquals(error, response.error());
        assertEquals(message, response.message());
    }
}