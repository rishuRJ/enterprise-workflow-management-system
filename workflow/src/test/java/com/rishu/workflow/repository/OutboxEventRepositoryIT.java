package com.rishu.workflow.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import com.rishu.workflow.AbstractIntegrationTest;
import com.rishu.workflow.entity.OutboxEvent;
import com.rishu.workflow.enums.OutboxEventStatus;

/**
 * Runs the outbox poller's native SQL against real PostgreSQL. These queries
 * use NOW(), INTERVAL and FOR UPDATE SKIP LOCKED, which mocks and H2 cannot check.
 */
class OutboxEventRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
    }

    @Test
    void pendingQueryReturnsOnlyEventsThatAreDue() {
        LocalDateTime now = LocalDateTime.now();
        Long pending = save(OutboxEventStatus.PENDING, 0, null, null);
        Long failedAndDue = save(OutboxEventStatus.FAILED, 2, now.minusMinutes(1), null);
        save(OutboxEventStatus.FAILED, 2, now.plusMinutes(10), null);    // retry not due yet
        save(OutboxEventStatus.FAILED, 4, now.minusMinutes(1), null);    // over MAX_RETRY_COUNT
        save(OutboxEventStatus.PROCESSED, 0, null, null);
        save(OutboxEventStatus.PERMANENTLY_FAILED, 4, null, null);

        List<Long> claimed = inTransaction(() -> ids(outboxEventRepository.findPendingEvents(10)));

        assertThat(claimed).containsExactly(pending, failedAndDue);
    }

    @Test
    void pendingQueryRespectsTheBatchSizeInIdOrder() {
        Long first = save(OutboxEventStatus.PENDING, 0, null, null);
        Long second = save(OutboxEventStatus.PENDING, 0, null, null);
        save(OutboxEventStatus.PENDING, 0, null, null);

        List<Long> claimed = inTransaction(() -> ids(outboxEventRepository.findPendingEvents(2)));

        assertThat(claimed).containsExactly(first, second);
    }

    @Test
    void staleQueryReturnsOnlyEventsProcessingForMoreThanFiveMinutes() {
        LocalDateTime now = LocalDateTime.now();
        Long stuck = save(OutboxEventStatus.PROCESSING, 0, null, now.minusMinutes(10));
        save(OutboxEventStatus.PROCESSING, 0, null, now.minusMinutes(1));
        save(OutboxEventStatus.PENDING, 0, null, null);

        List<Long> stale = inTransaction(() -> ids(outboxEventRepository.findStaleEvents(10)));

        assertThat(stale).containsExactly(stuck);
    }

    @Test
    void concurrentPollersNeverClaimTheSameEvent() throws Exception {
        Long e1 = save(OutboxEventStatus.PENDING, 0, null, null);
        Long e2 = save(OutboxEventStatus.PENDING, 0, null, null);
        Long e3 = save(OutboxEventStatus.PENDING, 0, null, null);
        Long e4 = save(OutboxEventStatus.PENDING, 0, null, null);

        CountDownLatch firstPollerHoldsLocks = new CountDownLatch(1);
        CountDownLatch secondPollerDone = new CountDownLatch(1);

        // First poller claims two events and keeps its transaction (and row locks) open
        CompletableFuture<List<Long>> firstPoller = CompletableFuture.supplyAsync(() ->
                inTransaction(() -> {
                    List<Long> claimed = ids(outboxEventRepository.findPendingEvents(2));
                    firstPollerHoldsLocks.countDown();
                    await(secondPollerDone);
                    return claimed;
                }));

        await(firstPollerHoldsLocks);
        // Second poller runs while those rows are locked: SKIP LOCKED passes over them
        List<Long> secondClaimed = inTransaction(() -> ids(outboxEventRepository.findPendingEvents(10)));
        secondPollerDone.countDown();

        assertThat(firstPoller.get(10, TimeUnit.SECONDS)).containsExactly(e1, e2);
        assertThat(secondClaimed).containsExactly(e3, e4);
    }

    private Long save(OutboxEventStatus status, int retryCount,
                      LocalDateTime nextRetryAt, LocalDateTime processingStartedAt) {
        return outboxEventRepository.save(OutboxEvent.builder()
                .status(status)
                .eventType("REQUEST_LIFECYCLE")
                .aggregateId(1L)
                .payload("{}")
                .retryCount(retryCount)
                .nextRetryAt(nextRetryAt)
                .processingStartedAt(processingStartedAt)
                .createdAt(LocalDateTime.now())
                .build()).getId();
    }

    // The queries use FOR UPDATE, which needs a transaction
    private <T> T inTransaction(Supplier<T> work) {
        return transactionTemplate.execute(status -> work.get());
    }

    private static List<Long> ids(List<OutboxEvent> events) {
        return events.stream().map(OutboxEvent::getId).toList();
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for the other poller");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
