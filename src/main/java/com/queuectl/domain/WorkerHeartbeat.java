package com.queuectl.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "worker_heartbeats", indexes = {
        @Index(name = "idx_hb_updated_at", columnList = "updated_at")
})
public class WorkerHeartbeat {

    @Id
    @Column(name = "worker_id", length = 120)
    private String workerId;

    @Column(name = "host")
    private String host;

    @Column(name = "pid")
    private long pid;

    @Column(name = "threads")
    private int threads;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    public WorkerHeartbeat() {}

    public WorkerHeartbeat(String workerId) {
        this.workerId = workerId;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    public void touch() {
        this.updatedAt = Instant.now();
    }

    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public long getPid() { return pid; }
    public void setPid(long pid) { this.pid = pid; }

    public int getThreads() { return threads; }
    public void setThreads(int threads) { this.threads = threads; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
