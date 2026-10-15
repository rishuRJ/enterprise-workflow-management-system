package com.rishu.workflow.event;

import com.rishu.workflow.entity.Request;
import com.rishu.workflow.entity.User;
import com.rishu.workflow.enums.Action;
import com.rishu.workflow.enums.RequestStatus;

import java.time.LocalDateTime;

public record RequestLifecycleEvent(
        Long requestId,

        String title,

        String actorEmail,

        Action action,

        String employeeEmail,

        RequestStatus status,

        LocalDateTime occurredAt
) {

    public static RequestLifecycleEvent from(
            Request request,
            Action action,
            User actor
    ){
        return new RequestLifecycleEvent(
            request.getId(),
            request.getTitle(),
            actor.getEmail(),
            action,
            request.getEmployee().getEmail(),
            request.getStatus(),
            request.getUpdatedAt()
        );
    }

}
