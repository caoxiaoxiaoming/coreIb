package com.coreib.security;

/** Raised by a business service when action or row scope is not granted. */
public class PermissionDeniedException extends RuntimeException {
    public PermissionDeniedException(String resource, PermissionAction action, String reason) {
        super("Permission denied for " + resource + " / " + action + ": " + reason);
    }
}
