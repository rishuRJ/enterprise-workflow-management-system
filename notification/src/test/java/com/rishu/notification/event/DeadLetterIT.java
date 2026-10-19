package com.rishu.notification.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.rishu.notification.AbstractIntegrationTest;
import com.rishu.notification.repository.DeadLetterRepository;

/**
 * A message the listener cannot read must not block the topic: the error
 * handler sends it to request-lifecycle-dlt, and DeadLetterListener stores it.
 */
class DeadLetterIT extends AbstractIntegrationTest {

    @Autowired
    private DeadLetterRepository deadLetterRepository;

    @BeforeEach
    void setUp() {
        deadLetterRepository.deleteAll();
    }

    @Test
    void unreadableMessageIsStoredAsADeadLetter() {
        // A unique key, so this test only looks at its own message
        String key = "bad-" + UUID.randomUUID();

        send("request-lifecycle", key, "this is not json");

        // The listener runs on its own thread, so poll until the row appears
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(deadLetterRepository.findAll())
                        .filteredOn(deadLetter -> key.equals(deadLetter.getMessageKey()))
                        .singleElement()
                        .satisfies(deadLetter -> {
                            assertThat(deadLetter.getPayload()).isEqualTo("this is not json");
                            assertThat(deadLetter.getOriginalTopic()).isEqualTo("request-lifecycle");
                            assertThat(deadLetter.getExceptionClass()).endsWith("DeserializationException");
                        }));
    }
}
