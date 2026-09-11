package com.coreib.platform;

import java.time.Instant;

public record AuditEvent(String eventId, String actorId, String action, String resource, String targetId, String outcome, String details, Instant occurredAt) {
    public AuditEvent {
        requireText("eventId", eventId); requireText("actorId", actorId); requireText("action", action); requireText("resource", resource); requireText("targetId", targetId); requireText("outcome", outcome);
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
    }
    private static void requireText(String name, String value) { if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank"); }
}
