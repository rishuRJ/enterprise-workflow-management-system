package com.rishu.workflow.repository;

import com.rishu.workflow.entity.Request;
import com.rishu.workflow.entity.User;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface RequestRepository
        extends JpaRepository<Request, Long>,
        JpaSpecificationExecutor<Request>
{
    @EntityGraph(attributePaths = {
            "employee",
            "manager"
    })
    Page<Request> findByEmployee(User employee, Pageable pageable);

    Page<Request> findAll(Specification<Request> spec, Pageable pageable);

    List<Request> findByManager(User manager);
}