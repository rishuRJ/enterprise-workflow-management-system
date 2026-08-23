package com.rishu.workflow.enums;

public enum OutboxEventStatus {
    PENDING,
    PROCESSING,
    FAILED,
    PROCESSED,
    PERMANENTLY_FAILED
}
