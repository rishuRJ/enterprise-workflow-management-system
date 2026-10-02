package com.rishu.workflow.kafka;


import com.rishu.workflow.event.RequestLifecycleEvent;
import org.springframework.kafka.support.SendResult;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class RequestLifecycleKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "request-lifecycle";

    public void publish(RequestLifecycleEvent event) throws Exception {

        CompletableFuture<SendResult<String, Object>> future  = kafkaTemplate.send(TOPIC, event.requestId().toString(), event);

        future.get();
    }





}
