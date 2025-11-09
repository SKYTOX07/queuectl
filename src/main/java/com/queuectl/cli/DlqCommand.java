
package com.queuectl.cli;

import com.queuectl.domain.Job;
import com.queuectl.domain.JobState;
import com.queuectl.repo.JobRepository;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@Command(name="dlq", description = "View or retry DLQ jobs", subcommands = {
        DlqList.class, DlqRetry.class
})
public class DlqCommand {}

@Component
@Command(name="list", description = "List jobs in the Dead Letter Queue")
class DlqList implements Runnable {
    private final JobRepository jobs;
    public DlqList(JobRepository jobs) { this.jobs = jobs; }
    @Override
    public void run() {
        List<Job> list = jobs.findTop100ByStateOrderByCreatedAtAsc(JobState.DEAD);
        for (Job j : list) {
            System.out.printf("%s (attempts=%d, exit=%s) err=%s%n",
                    j.getId(), j.getAttempts(), j.getExitCode(), j.getLastError());
        }
    }
}

@Component
@Command(name="retry", description = "Retry a DLQ job by ID (resets attempts and schedules now)")
class DlqRetry implements Runnable {
    private final JobRepository jobs;
    public DlqRetry(JobRepository jobs) { this.jobs = jobs; }

    @Parameters(index="0", description = "Job ID to retry")
    private String id;

    @Override
    public void run() {
        Optional<Job> opt = jobs.findById(id);
        if (opt.isEmpty()) {
            System.err.println("Job not found: " + id);
            System.exit(2);
        }
        Job j = opt.get();
        if (j.getState() != JobState.DEAD) {
            System.err.println("Job is not in DLQ: " + id);
            System.exit(2);
        }
        j.setState(JobState.PENDING);
        j.setAttempts(0);
        j.setNextRunAt(Instant.now());
        j.setLastError(null);
        jobs.save(j);
        System.out.println("DLQ job scheduled for retry: " + id);
    }
}
