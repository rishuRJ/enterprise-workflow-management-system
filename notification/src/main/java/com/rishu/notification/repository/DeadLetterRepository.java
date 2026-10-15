package com.rishu.notification.repository;

import com.rishu.notification.entity.DeadLetter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeadLetterRepository extends JpaRepository<DeadLetter, Long> {

    boolean existsByDltPartitionAndDltOffset(int dltPartition, long dltOffset);
}
