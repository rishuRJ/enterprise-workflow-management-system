package com.rishu.workflow.repository;

import com.rishu.workflow.entity.Request;
import com.rishu.workflow.entity.RequestHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequestHistoryRepository extends JpaRepository<RequestHistory, Long> {
    List<RequestHistory> findByRequestOrderByOccurredAtAsc(Request request);
}
