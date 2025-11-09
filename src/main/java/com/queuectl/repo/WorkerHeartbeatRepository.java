
package com.queuectl.repo;

import com.queuectl.domain.WorkerHeartbeat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkerHeartbeatRepository extends JpaRepository<WorkerHeartbeat, String> {
}
