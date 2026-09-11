package com.coreib.server.api;

import com.coreib.platform.AuditEvent;
import com.coreib.platform.AuditSink;

final class NoopAuditSink implements AuditSink {
    @Override public void append(AuditEvent event) { }
}
