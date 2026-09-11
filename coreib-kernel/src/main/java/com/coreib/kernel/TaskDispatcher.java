package com.coreib.kernel;

import java.time.Instant;
import java.util.Map;

/** Durable task boundary for exports, notifications, ETL, OCR and AI jobs. */
@FunctionalInterface
public interface TaskDispatcher {
    TaskReceipt dispatch(String taskType, Map<String, Object> payload);

    record TaskReceipt(String taskId, String taskType, Instant acceptedAt) {
    }
}
