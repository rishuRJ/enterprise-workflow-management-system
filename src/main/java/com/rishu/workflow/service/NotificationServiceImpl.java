package com.rishu.workflow.service;

import com.rishu.workflow.event.RequestLifecycleEvent;
import org.springframework.stereotype.Service;


@Service
public class NotificationServiceImpl implements NotificationService {

    public void sendNotification(RequestLifecycleEvent event){

        System.out.println(
                "Sending notification | "
                        + "requestId=" + event.requestId()
                        + " | employee=" + event.employeeEmail()
                        + " | action=" + event.action()
        );

    }
}
