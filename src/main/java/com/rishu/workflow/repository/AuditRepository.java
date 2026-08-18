package com.rishu.workflow.repository;

import com.rishu.workflow.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditRepository extends JpaRepository<AuditLog, Integer> {

}
