package com.coreib.kernel;

import java.time.Instant;

/** Serializable event boundary used by outbox or message-broker adapters. */
public interface DomainEvent {
    String eventId();

    String eventType();

    Instant occurredAt();

    String aggregateId();
}
