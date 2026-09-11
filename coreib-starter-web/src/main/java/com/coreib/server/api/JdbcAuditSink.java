package com.coreib.server.api;

import com.coreib.platform.AuditEvent;
import com.coreib.platform.AuditSink;
import org.springframework.jdbc.core.JdbcTemplate;

public final class JdbcAuditSink implements AuditSink {
    private final JdbcTemplate jdbc;

    public JdbcAuditSink(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void append(AuditEvent event) {
        jdbc.update("INSERT INTO coreib_audit_event (event_id, actor_id, action, resource, target_id, outcome, details, occurred_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                event.eventId(), event.actorId(), event.action(), event.resource(), event.targetId(), event.outcome(), event.details(), event.occurredAt());
    }
}
