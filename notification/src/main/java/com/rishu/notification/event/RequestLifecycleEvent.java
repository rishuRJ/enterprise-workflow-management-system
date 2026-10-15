package com.rishu.notification.event;

import java.time.LocalDateTime;

public record RequestLifecycleEvent(
        Long requestId,

        String title,

        String actorEmail,

        String action,

        String employeeEmail,

        String status,

        LocalDateTime occurredAt
) {

}
