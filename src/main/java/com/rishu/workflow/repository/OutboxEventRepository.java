package com.rishu.workflow.repository;

import com.rishu.workflow.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query(value = """
            SELECT *
            FROM outbox_event
            WHERE 
            (status = 'PENDING'
            OR (status = 'FAILED' AND retry_count < 3))
            AND (next_retry_at IS NULL or next_retry_at <= NOW())
            ORDER BY id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> findPendingEvents(
            @Param("batchSize") int batchSize
    );


    @Query(value = """
            Select *
            from outbox_event
            where
            status = 'PROCESSING' AND processing_started_at< NOW() - INTERVAL '5 minutes'
            order by id
            limit :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> findStaleEvents(
            @Param("batchSize") int batchSize
    );
}
