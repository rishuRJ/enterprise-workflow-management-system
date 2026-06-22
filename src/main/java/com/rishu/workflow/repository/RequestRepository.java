package com.rishu.workflow.repository;

import com.rishu.workflow.entity.Request;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestRepository
        extends JpaRepository<Request, Long> {
}