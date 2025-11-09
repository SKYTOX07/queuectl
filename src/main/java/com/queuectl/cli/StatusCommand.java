
package com.queuectl.cli;

import com.queuectl.domain.JobState;
import com.queuectl.domain.WorkerHeartbeat;
import com.queuectl.repo.WorkerHeartbeatRepository;
import com.queuectl.service.ConfigService;
import com.queuectl.repo.JobRepository;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

import java.time.Instant;
import java.util.List;

@Component
@Command(name="status", description = "Show summary of job states & active workers")
public class StatusCommand implements Runnable {

    private final JobRepository jobs;
    private final WorkerHeartbeatRepository hbRepo;
    private final ConfigService cfg;

    public StatusCommand(JobRepository jobs, WorkerHeartbeatRepository hbRepo, ConfigService cfg) {
        this.jobs = jobs;
        this.hbRepo = hbRepo;
        this.cfg = cfg;
    }

    @Override
    public void run() {
        long pending = jobs.countByState(JobState.PENDING);
        long processing = jobs.countByState(JobState.PROCESSING);
        long completed = jobs.countByState(JobState.COMPLETED);
        long failed = jobs.countByState(JobState.FAILED);
        long dead = jobs.countByState(JobState.DEAD);

        System.out.println("=== Job Summary ===");
        System.out.printf("PENDING    : %d%n", pending);
        System.out.printf("PROCESSING : %d%n", processing);
        System.out.printf("COMPLETED  : %d%n", completed);
        System.out.printf("FAILED     : %d%n", failed);
        System.out.printf("DEAD (DLQ) : %d%n", dead);
        System.out.println();

        System.out.println("=== Active Workers (last 10s) ===");
        Instant cutoff = Instant.now().minusSeconds(10);
        List<WorkerHeartbeat> all = hbRepo.findAll();
        int active = 0;
        for (WorkerHeartbeat hb : all) {
            if (hb.getUpdatedAt() != null && hb.getUpdatedAt().isAfter(cutoff)) {
                active++;
                System.out.printf("- %s host=%s pid=%d threads=%d last=%s%n",
                        hb.getWorkerId(), hb.getHost(), hb.getPid(), hb.getThreads(), hb.getUpdatedAt());
            }
        }
        if (active == 0) System.out.println("(no active workers detected)");
        System.out.println();

        System.out.println("=== Config ===");
        System.out.printf("max_retries=%d, backoff_base=%d, poll_interval_ms=%d, heartbeat_interval_ms=%d%n",
                cfg.maxRetries(), cfg.backoffBase(), cfg.pollIntervalMs(), cfg.heartbeatIntervalMs());
        System.out.printf("workers.stop=%s%n", cfg.stopRequested());
    }
}
