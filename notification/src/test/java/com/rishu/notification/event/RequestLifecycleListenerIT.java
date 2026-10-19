package com.rishu.notification.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.LocalDateTime;

import com.rishu.notification.entity.ProcessedEventId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.rishu.notification.AbstractIntegrationTest;
import com.rishu.notification.repository.ProcessedEventRepository;

class RequestLifecycleListenerIT extends AbstractIntegrationTest {

    private static final String TOPIC = "request-lifecycle";

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @BeforeEach
    void setUp() {
        processedEventRepository.deleteAll();
    }

    @Test
    void storesAValidEventAsProcessed() {

        String key = "42";
        String event = eventJson(42L, "REQUEST_APPROVED");
        send(TOPIC, key, event);


        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(processedEventRepository.existsById(new ProcessedEventId(42L, "REQUEST_APPROVED")))
                        .isEqualTo(true)
        );
    }

    @Test
    void ignoresADuplicateEvent() {
        String key = "43";
        String event = eventJson(43L, "REQUEST_APPROVED");
        send(TOPIC, key, event);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(processedEventRepository.existsById(new ProcessedEventId(43L, "REQUEST_APPROVED")))
                        .isEqualTo(true)
        );

        LocalDateTime firstProcessedAt = processedEventRepository
                .findById(new ProcessedEventId(43L, "REQUEST_APPROVED"))
                .orElseThrow()
                .getProcessedAt();

        send(TOPIC, key, event);
        String marker = eventJson(43L, "REQUEST_REJECTED");
        send(TOPIC, key, marker);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(processedEventRepository.existsById(new ProcessedEventId(43L, "REQUEST_REJECTED")))
                        .isEqualTo(true)
        );

        assertThat(processedEventRepository.count()).isEqualTo(2);

        assertThat(processedEventRepository.findById(new ProcessedEventId(43L, "REQUEST_APPROVED")).orElseThrow().getProcessedAt())
                .isEqualTo(firstProcessedAt);

    }

    // The JSON the workflow service publishes for a request event
    private static String eventJson(Long requestId, String action) {
        return """
                {"requestId":%d,"title":"New laptop","actorEmail":"manager@test.com",
                 "action":"%s","employeeEmail":"alice@test.com","status":"APPROVED",
                 "occurredAt":"2026-10-19T10:00:00"}
                """.formatted(requestId, action);
    }
}