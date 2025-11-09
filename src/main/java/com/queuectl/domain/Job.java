package com.queuectl.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "jobs", indexes = {
        @Index(name = "idx_jobs_state", columnList = "state"),
        @Index(name = "idx_jobs_next_run_at", columnList = "next_run_at"),
        @Index(name = "idx_jobs_priority", columnList = "priority")
})
public class Job {

    @Id
    @Column(length = 120)
    private String id;

    @Column(nullable = false)
    private String command;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobState state = JobState.PENDING;

    private int attempts = 0;
    private Integer maxRetries;
    private Integer backoffBase;

    @Column(name = "run_at")
    private Instant runAt;

    @Column(name = "next_run_at")
    private Instant nextRunAt;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @Column(name = "locked_by")
    private String lockedBy;

    @Column(name = "locked_at")
    private Instant lockedAt;

    private Integer exitCode;

    @Column(length = 4000)
    private String lastError;

    @Column(name = "log_path")
    private String logPath;

    private int priority = 0;

    public Job() {}

    public Job(String id, String command) {
        this.id = id;
        this.command = command;
        this.state = JobState.PENDING;
        this.runAt = Instant.now();
        this.nextRunAt = this.runAt;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void touch() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public JobState getState() { return state; }
    public void setState(JobState state) { this.state = state; }

    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }

    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }

    public Integer getBackoffBase() { return backoffBase; }
    public void setBackoffBase(Integer backoffBase) { this.backoffBase = backoffBase; }

    public Instant getRunAt() { return runAt; }
    public void setRunAt(Instant runAt) { this.runAt = runAt; }

    public Instant getNextRunAt() { return nextRunAt; }
    public void setNextRunAt(Instant nextRunAt) { this.nextRunAt = nextRunAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getLockedBy() { return lockedBy; }
    public void setLockedBy(String lockedBy) { this.lockedBy = lockedBy; }

    public Instant getLockedAt() { return lockedAt; }
    public void setLockedAt(Instant lockedAt) { this.lockedAt = lockedAt; }

    public Integer getExitCode() { return exitCode; }
    public void setExitCode(Integer exitCode) { this.exitCode = exitCode; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public String getLogPath() { return logPath; }
    public void setLogPath(String logPath) { this.logPath = logPath; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
}
