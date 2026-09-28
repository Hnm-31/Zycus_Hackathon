package com.example.StockPulse_AI.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();
    private final WebRequest webRequest = mock(WebRequest.class);

    @Test
    void handleIllegalArgumentException_ReturnsBadRequest() {
        // Arrange
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument provided");

        // Act
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleIllegalArgumentException(exception, webRequest);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getBody().status());
        assertEquals(HttpStatus.BAD_REQUEST.getReasonPhrase(), response.getBody().error());
        assertEquals("Invalid argument provided", response.getBody().message());
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    void handleIllegalStateException_ReturnsConflict() {
        // Arrange
        IllegalStateException exception = new IllegalStateException("State conflict occurred");

        // Act
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleIllegalStateException(exception, webRequest);

        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.CONFLICT.value(), response.getBody().status());
        assertEquals(HttpStatus.CONFLICT.getReasonPhrase(), response.getBody().error());
        assertEquals("State conflict occurred", response.getBody().message());
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    void handleEntityNotFoundException_ReturnsNotFound() {
        // Arrange
        jakarta.persistence.EntityNotFoundException exception = new jakarta.persistence.EntityNotFoundException("Entity not found");

        // Act
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleNotFoundException(exception, webRequest);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.NOT_FOUND.value(), response.getBody().status());
        assertEquals(HttpStatus.NOT_FOUND.getReasonPhrase(), response.getBody().error());
        assertEquals("Entity not found", response.getBody().message());
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    void handleGenericException_ReturnsInternalServerError() {
        // Arrange
        Exception exception = new Exception("Unexpected error");

        // Act
        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleGenericException(exception, webRequest);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getBody().status());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), response.getBody().error());
        assertTrue(response.getBody().message().contains("Unexpected error"));
        assertNotNull(response.getBody().timestamp());
    }
}