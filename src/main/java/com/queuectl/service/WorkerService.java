
package com.queuectl.service;

import com.queuectl.domain.Job;
import com.queuectl.repo.JobRepository;
import com.queuectl.domain.JobState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class WorkerService {
    private static final Logger log = LoggerFactory.getLogger(WorkerService.class);
    private final JobRepository jobs;
    private final JobExecutor executor;
    private final ConfigService cfg;
    private final HeartbeatService hb;

    private volatile boolean running = false;
    private ExecutorService pool;

    public WorkerService(JobRepository jobs, JobExecutor executor, ConfigService cfg, HeartbeatService hb) {
        this.jobs = jobs;
        this.executor = executor;
        this.cfg = cfg;
        this.hb = hb;
    }

    public void start(int count) throws Exception {
        if (running) { log.info("Workers already running."); return; }
        running = true;
        cfg.requestStop(false);

        String workerPrefix = InetAddress.getLocalHost().getHostName() + "-" + ProcessHandle.current().pid() + "-" + UUID.randomUUID().toString().substring(0,8);

        pool = Executors.newFixedThreadPool(count);
        log.info("Starting {} worker threads (prefix={})", count, workerPrefix);

        for (int i = 0; i < count; i++) {
            final String workerId = workerPrefix + "-w" + i;
            pool.submit(() -> runLoop(workerId));
        }

        hb.startBackground(workerPrefix, count);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                log.info("Shutdown hook: stopping workers gracefully...");
                stop();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
    }

    public void stop() throws InterruptedException {
        running = false;
        cfg.requestStop(true);
        if (pool != null) {
            pool.shutdown();
            pool.awaitTermination(30, TimeUnit.SECONDS);
        }
        hb.stopBackground();
        log.info("Workers stopped.");
    }

    private void runLoop(String workerId) {
        while (running && !cfg.stopRequested()) {
            try {
                Optional<Job> next = jobs.findNextRunnable(Instant.now());
                if (next.isEmpty()) {
                    Thread.sleep(cfg.pollIntervalMs());
                    continue;
                }
                Job j = next.get();
                if (claim(workerId, j.getId())) {
                    executor.process(workerId, j);
                } else {
                    // lost race
                }
            } catch (Exception e) {
                log.warn("Worker {} loop error: {}", workerId, e.toString());
                try { Thread.sleep(cfg.pollIntervalMs()); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
        log.info("Worker thread {} exiting gracefully.", workerId);
    }

    @Transactional
    public boolean claim(String workerId, String jobId) {
        int updated = jobs.claimById(jobId, workerId, Instant.now());
        return updated == 1;
    }

    public long count(JobState s) { return jobs.countByState(s); }
}
