
package com.queuectl.repo;

import com.queuectl.domain.Job;
import com.queuectl.domain.JobState;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, String> {

    @Query(value = "SELECT * FROM jobs WHERE (state='PENDING' OR state='FAILED') AND (nextRunAt IS NULL OR nextRunAt <= :now) ORDER BY priority DESC, createdAt ASC LIMIT 1", nativeQuery = true)
    Optional<Job> findNextRunnable(@Param("now") Instant now);

    @Modifying
    @Query("UPDATE Job j SET j.state = 'PROCESSING', j.lockedBy = :workerId, j.lockedAt = :now WHERE j.id = :id AND (j.state = 'PENDING' OR j.state='FAILED')")
    int claimById(@Param("id") String id, @Param("workerId") String workerId, @Param("now") Instant now);

    long countByState(JobState state);
    List<Job> findTop100ByStateOrderByCreatedAtAsc(JobState state);
    List<Job> findTop100ByOrderByCreatedAtDesc();
}
