package com.rishu.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rishu.workflow.dto.OutboxEventResponseDto;
import com.rishu.workflow.entity.OutboxEvent;
import com.rishu.workflow.enums.OutboxEventStatus;
import com.rishu.workflow.event.RequestLifecycleEvent;
import com.rishu.workflow.exception.BusinessException;
import com.rishu.workflow.exception.ResourceNotFoundException;
import com.rishu.workflow.mapper.OutboxEventMapper;
import com.rishu.workflow.repository.OutboxEventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OutboxProcessorService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final OutboxEventMapper outboxEventMapper;

    @Transactional
    List<OutboxEvent> claimEvents(int batchSize) {

        List<OutboxEvent> events = outboxEventRepository.findPendingEvents(batchSize);

        events.forEach(event -> {
            event.setStatus(OutboxEventStatus.PROCESSING);
            event.setProcessingStartedAt(LocalDateTime.now());
        });

        return events;
    }

    @Transactional
    public void process(OutboxEvent event){

        try{
                RequestLifecycleEvent lifecycleEvent = objectMapper
                        .readValue(event.getPayload(),RequestLifecycleEvent.class);

                notificationService.sendNotification(lifecycleEvent);

                event.setStatus(OutboxEventStatus.PROCESSED);
                event.setProcessedAt(LocalDateTime.now());

                outboxEventRepository.save(event);

        } catch (Exception e) {

            int retryCount = event.getRetryCount() + 1;

            event.setRetryCount(retryCount);
            event.setLastError(e.getMessage());

            if (retryCount > 3) {
                event.setStatus(OutboxEventStatus.PERMANENTLY_FAILED);
                event.setNextRetryAt(null);
            } else {
                event.setStatus(OutboxEventStatus.FAILED);
                event.setNextRetryAt(calculateNextRetryAt(retryCount));
            }

            outboxEventRepository.save(event);
        }
    }

    public void recoverStaleEvents() {
        List<OutboxEvent> events = outboxEventRepository.findStaleEvents(10);

        events.forEach(event -> {
            int retryCount = event.getRetryCount() + 1;

            event.setRetryCount(retryCount);
            event.setLastError(
                    "Processing timed out"
            );
            event.setProcessingStartedAt(null);

            if (retryCount > 3) {
                event.setStatus(
                        OutboxEventStatus.PERMANENTLY_FAILED
                );
                event.setNextRetryAt(null);
            } else {
                event.setStatus(
                        OutboxEventStatus.FAILED
                );
                event.setNextRetryAt(
                        calculateNextRetryAt(retryCount)
                );
            }
        });
    }

    @Transactional
    public OutboxEventResponseDto retryPermanentlyFailedEvent(Long eventId) {
        OutboxEvent event = outboxEventRepository.findById(eventId)
                .orElseThrow(()->new ResourceNotFoundException("Outbox event not found"));

        if (event.getStatus()
                != OutboxEventStatus.PERMANENTLY_FAILED) {

            throw new BusinessException(
                    "Only permanently failed events can be manually retried");
        }

        event.setStatus(OutboxEventStatus.PENDING);
        event.setRetryCount(0);
        event.setNextRetryAt(null);
        event.setProcessedAt(null);
        event.setLastError(null);
        event.setProcessingStartedAt(null);

        return outboxEventMapper.toDto(event);

    }





    private LocalDateTime calculateNextRetryAt(int retryCount) {

        return switch (retryCount){
            case 1 -> LocalDateTime.now().plusMinutes(1);
            case 2 -> LocalDateTime.now().minusMinutes(5);
            case 3 -> LocalDateTime.now().minusMinutes(15);
            default -> null;
        };
    }
}
