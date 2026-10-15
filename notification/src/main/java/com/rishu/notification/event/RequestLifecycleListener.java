package com.rishu.notification.event;

import com.rishu.notification.entity.ProcessedEvent;
import com.rishu.notification.entity.ProcessedEventId;
import com.rishu.notification.repository.ProcessedEventRepository;
import lombok.AllArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@AllArgsConstructor
public class RequestLifecycleListener {

    private final ProcessedEventRepository processedEventRepository;

    @KafkaListener(topics = "request-lifecycle")
    void process(RequestLifecycleEvent event){
        ProcessedEventId id = new ProcessedEventId(event.requestId(), event.action());

        if(processedEventRepository.existsById(id)){
            System.out.println("Duplicate event, skipping: " + event);
            return;
        }

        System.out.println("Request Lifecycle Listener event: " + event);

        processedEventRepository.save(new ProcessedEvent(id, LocalDateTime.now()));


    }

}
