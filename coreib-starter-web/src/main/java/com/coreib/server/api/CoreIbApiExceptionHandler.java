package com.coreib.server.api;

import com.coreib.security.PermissionDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Keeps authorization failures machine-readable for the Vue shell and API clients. */
@RestControllerAdvice
public class CoreIbApiExceptionHandler {
    @ExceptionHandler(PermissionDeniedException.class)
    ResponseEntity<PermissionErrorResponse> permissionDenied(PermissionDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new PermissionErrorResponse("PERMISSION_DENIED", exception.getMessage()));
    }

    @ExceptionHandler(PlatformCommandUnavailableException.class)
    ResponseEntity<PermissionErrorResponse> commandUnavailable(PlatformCommandUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new PermissionErrorResponse("PLATFORM_COMMAND_UNAVAILABLE", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<PermissionErrorResponse> invalidRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(new PermissionErrorResponse("INVALID_REQUEST", exception.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<PermissionErrorResponse> conflictingConfiguration(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new PermissionErrorResponse("CONFIGURATION_CONFLICT", "The requested configuration conflicts with stored data"));
    }

    record PermissionErrorResponse(String code, String message) {
    }
}
