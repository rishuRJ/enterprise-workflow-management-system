package com.rishu.notification.event;

import com.rishu.notification.entity.DeadLetter;
import com.rishu.notification.repository.DeadLetterRepository;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class DeadLetterListener {

    private final DeadLetterRepository deadLetterRepository;

    @KafkaListener(
            topics = "request-lifecycle-dlt",
            groupId = "notification-service-dlt",
            properties = "value.deserializer=org.apache.kafka.common.serialization.StringDeserializer")
    void onDeadLetter(ConsumerRecord<String, String> record) {
        if (deadLetterRepository.existsByDltPartitionAndDltOffset(record.partition(), record.offset())) {
            System.out.println("Dead letter already stored, skipping: partition="
                    + record.partition() + " offset=" + record.offset());
            return;
        }

        deadLetterRepository.save(DeadLetter.builder()
                .messageKey(record.key())
                .payload(record.value())
                .exceptionClass(header(record, "kafka_dlt-exception-fqcn"))
                .exceptionMessage(header(record, "kafka_dlt-exception-message"))
                .originalTopic(header(record, "kafka_dlt-original-topic"))
                .dltPartition(record.partition())
                .dltOffset(record.offset())
                .receivedAt(LocalDateTime.now())
                .build());

        System.out.println("Dead letter stored | key=" + record.key()
                + " | partition=" + record.partition() + " | offset=" + record.offset());
    }

    private String header(ConsumerRecord<String, String> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header == null ? null : new String(header.value(), StandardCharsets.UTF_8);
    }
}
