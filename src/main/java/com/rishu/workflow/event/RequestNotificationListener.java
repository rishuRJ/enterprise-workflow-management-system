package com.rishu.workflow.event;



import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;



@Component
public class RequestNotificationListener {




    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(RequestLifecycleEvent requestLifecycleEvent) {
        System.out.println("Sending email to " + requestLifecycleEvent.employeeEmail());
        System.out.println(
                "Your current status is " + requestLifecycleEvent.status()
        );
    }
}
