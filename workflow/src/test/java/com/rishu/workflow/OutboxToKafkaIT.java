package com.rishu.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.rishu.workflow.entity.OutboxEvent;
import com.rishu.workflow.enums.Action;
import com.rishu.workflow.enums.OutboxEventStatus;
import com.rishu.workflow.enums.RequestStatus;
import com.rishu.workflow.event.RequestLifecycleEvent;
import com.rishu.workflow.repository.OutboxEventRepository;
import com.rishu.workflow.service.OutboxScheduler;
import com.rishu.workflow.service.OutboxService;

/**
 * End to end through the transactional outbox: an event saved to the outbox
 * table is picked up by the poller, published to the real Kafka topic, and the
 * row is marked PROCESSED.
 */
class OutboxToKafkaIT extends AbstractIntegrationTest {

    private static final String TOPIC = "request-lifecycle";

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private OutboxScheduler outboxScheduler;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
    }

    @Test
    void savedEventIsPublishedToKafkaAndMarkedProcessed() {
        RequestLifecycleEvent event = new RequestLifecycleEvent(
                42L, "New laptop", "manager@test.com", Action.REQUEST_APPROVED,
                "alice@test.com", RequestStatus.APPROVED, LocalDateTime.now());

        try (KafkaConsumer<String, String> consumer = newConsumer()) {
            consumer.subscribe(List.of(TOPIC));

            outboxService.save(event);
            // Scheduling is off in tests, so run one poll cycle by hand
            outboxScheduler.processOutbox();

            List<ConsumerRecord<String, String>> records = pollUntilOneRecord(consumer);

            assertThat(records).hasSize(1);
            assertThat(records.get(0).key()).isEqualTo("42");
            assertThat(records.get(0).value())
                    .contains("\"requestId\":42")
                    .contains("\"action\":\"REQUEST_APPROVED\"")
                    .contains("\"employeeEmail\":\"alice@test.com\"");
        }

        OutboxEvent stored = outboxEventRepository.findAll().get(0);
        assertThat(stored.getStatus()).isEqualTo(OutboxEventStatus.PROCESSED);
        assertThat(stored.getProcessedAt()).isNotNull();
        assertThat(stored.getRetryCount()).isZero();
    }

    private KafkaConsumer<String, String> newConsumer() {
        return new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "outbox-it-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class));
    }

    private static List<ConsumerRecord<String, String>> pollUntilOneRecord(KafkaConsumer<String, String> consumer) {
        List<ConsumerRecord<String, String>> records = new ArrayList<>();
        long deadline = System.currentTimeMillis() + Duration.ofSeconds(30).toMillis();
        while (records.isEmpty() && System.currentTimeMillis() < deadline) {
            consumer.poll(Duration.ofMillis(500)).forEach(records::add);
        }
        return records;
    }
}
