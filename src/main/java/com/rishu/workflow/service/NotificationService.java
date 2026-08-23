package com.rishu.workflow.service;


import com.rishu.workflow.event.RequestLifecycleEvent;



public interface NotificationService {
    void sendNotification(RequestLifecycleEvent event);
}
