package com.rishu.workflow.repository;

import com.rishu.workflow.entity.Request;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequestRepository
        extends JpaRepository<Request, Long> {
    List<Request> findByEmployeeId(Long employeeId);

    List<Request> findByManagerId(Long managerId);
}