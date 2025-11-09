
package com.queuectl.cli;

import com.queuectl.domain.Job;
import com.queuectl.repo.JobRepository;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@Component
@Command(name="enqueue", description = "Add a new job to the queue")
public class EnqueueCommand implements Runnable {

    private final JobRepository jobs;

    public EnqueueCommand(JobRepository jobs) {
        this.jobs = jobs;
    }

    @Parameters(index="0", arity="1", description = "Job command to execute, e.g. \"sleep 2\" or \"echo hello\"")
    private String command;

    @Option(names="--id", description="Job ID (default: random UUID)")
    private String id;

    @Option(names="--max-retries", description="Override max retries for this job")
    private Integer maxRetries;

    @Option(names="--backoff-base", description="Override backoff base for this job")
    private Integer backoffBase;

    @Option(names="--run-at", description="Schedule time (ISO-8601, default now)")
    private String runAtStr;

    @Option(names="--priority", description="Job priority (higher first, default 0)")
    private int priority = 0;

    @Override
    public void run() {
        String jid = id != null ? id : UUID.randomUUID().toString();
        Job j = new Job(jid, command);
        j.setPriority(priority);
        if (maxRetries != null) j.setMaxRetries(maxRetries);
        if (backoffBase != null) j.setBackoffBase(backoffBase);
        if (runAtStr != null) {
            try {
                Instant ts = OffsetDateTime.parse(runAtStr).toInstant();
                j.setRunAt(ts);
                j.setNextRunAt(ts);
            } catch (DateTimeParseException e) {
                System.err.println("Invalid --run-at timestamp. Use ISO-8601, e.g. 2025-11-09T10:00:00+05:30");
                System.exit(2);
            }
        }
        jobs.save(j);
        System.out.printf("Enqueued job %s : %s%n", j.getId(), j.getCommand());
    }
}
