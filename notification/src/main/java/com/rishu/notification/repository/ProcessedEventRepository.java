package com.rishu.notification.repository;

import com.rishu.notification.entity.ProcessedEvent;
import com.rishu.notification.entity.ProcessedEventId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, ProcessedEventId> {
}
