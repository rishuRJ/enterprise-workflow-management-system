package com.rishu.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.rishu.workflow.entity.OutboxEvent;
import com.rishu.workflow.enums.OutboxEventStatus;
import com.rishu.workflow.exception.BusinessException;
import com.rishu.workflow.exception.ResourceNotFoundException;
import com.rishu.workflow.kafka.RequestLifecycleKafkaProducer;
import com.rishu.workflow.mapper.OutboxEventMapper;
import com.rishu.workflow.repository.OutboxEventRepository;
import jakarta.validation.constraints.Max;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxProcessorServiceTest {

    private static final String VALID_PAYLOAD = """
            {"requestId":1,"title":"Leave","actorEmail":"employee@test.com",
             "action":"REQUEST_SUBMITTED","employeeEmail":"employee@test.com",
             "status":"PENDING","occurredAt":"2026-10-01T10:00:00"}
            """;

    @Mock private OutboxEventRepository outboxEventRepository;
    @Mock private RequestLifecycleKafkaProducer kafkaProducer;
    @Mock private OutboxEventMapper outboxEventMapper;

    private OutboxProcessorService outboxProcessorService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
        outboxProcessorService = new OutboxProcessorService(
                outboxEventRepository, objectMapper, kafkaProducer, outboxEventMapper);
    }

    @Nested
    class Process{

        @Test
        void eventWithValidPayloadAndKafkaWorking() throws Exception{
            OutboxEvent event = eventWithRetryCount(0);

            outboxProcessorService.process(event);

            assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PROCESSED);
            assertThat(event.getProcessedAt()).isNotNull();

            verify(kafkaProducer).publish(any());
            verify(outboxEventRepository).save(event);
        }

        @Test
        void marksEventFailedWhenPublishFails () throws Exception{
            OutboxEvent event = eventWithRetryCount(0);
            doThrow(new RuntimeException("Kafka down"))
                    .when(kafkaProducer).publish(any());

            outboxProcessorService.process(event);

            assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
            assertThat(event.getLastError()).isEqualTo("Kafka down");
            assertThat(event.getRetryCount()).isEqualTo(1);

            verify(outboxEventRepository).save(event);
        }

        @ParameterizedTest
        @CsvSource({"0, 1", "1, 5", "2, 15"})
        void schedulesNextRetryWithBackoff(int retryCountBefore, int expectedDelayMinutes) throws Exception {
            OutboxEvent event = eventWithRetryCount(retryCountBefore);
            doThrow(new RuntimeException("Kafka down"))
                    .when(kafkaProducer).publish(any());

            LocalDateTime before = LocalDateTime.now();
            outboxProcessorService.process(event);
            LocalDateTime after = LocalDateTime.now();

            assertThat(event.getNextRetryAt())
                    .isBetween(before.plusMinutes(expectedDelayMinutes), after.plusMinutes(expectedDelayMinutes));
            assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
            assertThat(event.getLastError()).isEqualTo("Kafka down");
            assertThat(event.getRetryCount()).isEqualTo(retryCountBefore+1);

            verify(outboxEventRepository).save(event);
        }

        @Test
        void eventWithMoreThanThreeRetryCountMarkedPermanentlyFailed() throws Exception{
            OutboxEvent event = eventWithRetryCount(3);
            doThrow(new RuntimeException("Kafka down"))
                    .when(kafkaProducer).publish(any());

            outboxProcessorService.process(event);

            assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PERMANENTLY_FAILED);
            assertThat(event.getLastError()).isEqualTo("Kafka down");
            assertThat(event.getNextRetryAt()).isNull();
            assertThat(event.getRetryCount()).isEqualTo(4);

            verify(outboxEventRepository).save(event);
        }

        @Test
        void eventMarkedFailedWhenPayloadIsInvalid(){
            OutboxEvent event = OutboxEvent.builder().payload("Invalid-Payload").retryCount(0)
                    .id(1L).status(OutboxEventStatus.PROCESSING).build();

            outboxProcessorService.process(event);

            assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
            assertThat(event.getRetryCount()).isEqualTo(1);


            verifyNoInteractions(kafkaProducer);
            verify(outboxEventRepository).save(event);
        }

    }

    @Nested
    class RecoverStaleEvents{

        @Test
        void eventWithRetryCountMoreThanThreeShouldHaveStatusPermanentlyFailed(){
            OutboxEvent event = eventWithRetryCount(3);
            when(outboxEventRepository.findStaleEvents(10)).thenReturn(List.of(event));

            outboxProcessorService.recoverStaleEvents();

            assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PERMANENTLY_FAILED);
            assertThat(event.getLastError()).isEqualTo("Processing timed out");
            assertThat(event.getRetryCount()).isEqualTo(4);
            assertThat(event.getNextRetryAt()).isNull();
        }

        @ParameterizedTest
        @CsvSource({"0, 1", "1, 5", "2, 15"})
        void eventWithRetryCountLessThanEqualToThreeShouldHaveStatusFailed(int retryCountBefore, int expectedDelayMinutes){
            OutboxEvent event = eventWithRetryCount(retryCountBefore);
            when(outboxEventRepository.findStaleEvents(10)).thenReturn(List.of(event));


            LocalDateTime before = LocalDateTime.now();
            outboxProcessorService.recoverStaleEvents();
            LocalDateTime after = LocalDateTime.now();

            assertThat(event.getNextRetryAt())
                    .isBetween(before.plusMinutes(expectedDelayMinutes), after.plusMinutes(expectedDelayMinutes));

            assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
            assertThat(event.getLastError()).isEqualTo("Processing timed out");
            assertThat(event.getRetryCount()).isEqualTo(retryCountBefore+1);

        }
    }

    @Nested
    class RetryPermanentlyFailedEvent{

        @Test
        void eventNotFound(){
            when(outboxEventRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> outboxProcessorService.retryPermanentlyFailedEvent(1L)).isInstanceOf(ResourceNotFoundException.class);
            verifyNoInteractions(outboxEventMapper);
        }

        @ParameterizedTest
        @EnumSource(value = OutboxEventStatus.class, names = {"PROCESSING","PENDING","PROCESSED","FAILED"})
        void eventStatusIsNotPermanentlyFailed(OutboxEventStatus status){

            OutboxEvent event = OutboxEvent.builder().id(1L).status(status).build();
            when(outboxEventRepository.findById(1L)).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> outboxProcessorService.retryPermanentlyFailedEvent(1L)).isInstanceOf(BusinessException.class);

            verifyNoInteractions(outboxEventMapper);
        }

        @Test
        void permanentlyFailedEventRetried(){
            OutboxEvent event = OutboxEvent.builder()
                    .id(1L)
                    .status(OutboxEventStatus.PERMANENTLY_FAILED)
                    .retryCount(4)
                    .lastError("Kafka down")
                    .processingStartedAt(LocalDateTime.now())
                    .build();
            when(outboxEventRepository.findById(1L)).thenReturn(Optional.of(event));

            outboxProcessorService.retryPermanentlyFailedEvent(1L);
            assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PENDING);
            assertThat((event.getLastError())).isNull();
            assertThat((event.getNextRetryAt())).isNull();
            assertThat((event.getProcessedAt())).isNull();
            assertThat((event.getProcessingStartedAt())).isNull();
            assertThat(event.getRetryCount()).isEqualTo(0);

            verify(outboxEventMapper).toDto(event);
        }
    }

    @Nested
    class ClaimEvents{

        @ParameterizedTest
        @ValueSource(ints = {1,2})
        void eventClaimed(int eventCase){
            OutboxEvent event = switch (eventCase){
                case 1 -> OutboxEvent.builder().id(1L).status(OutboxEventStatus.PENDING).build();
                case 2 -> OutboxEvent.builder().id(2L).status(OutboxEventStatus.FAILED).retryCount(3).build();
                default -> throw new IllegalStateException("Unexpected value: " + eventCase);
            };
            when(outboxEventRepository.findPendingEvents(1)).thenReturn(List.of(event));

            outboxProcessorService.claimEvents(1);

            assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PROCESSING);
            assertThat(event.getProcessingStartedAt()).isBetween(LocalDateTime.now().minusMinutes(1), LocalDateTime.now());
        }
    }


    private OutboxEvent eventWithRetryCount(int retryCount) {
        return OutboxEvent.builder()
                .id(1L)
                .status(OutboxEventStatus.PROCESSING)
                .payload(VALID_PAYLOAD)
                .retryCount(retryCount)
                .build();
    }


}