
package com.queuectl.service;

import com.queuectl.domain.Job;
import com.queuectl.domain.JobState;
import com.queuectl.repo.JobRepository;
import com.queuectl.util.OSCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

@Service
public class JobExecutor {
    private static final Logger log = LoggerFactory.getLogger(JobExecutor.class);
    private final JobRepository jobs;
    private final ConfigService cfg;

    public JobExecutor(JobRepository jobs, ConfigService cfg) {
        this.jobs = jobs;
        this.cfg = cfg;
    }

    public void process(String workerId, Job job) {
        log.info("[{}] Executing job {}: {}", workerId, job.getId(), job.getCommand());
        job.setState(JobState.PROCESSING);
        job.setLockedBy(workerId);
        job.setLockedAt(Instant.now());
        job.setUpdatedAt(Instant.now());
        jobs.save(job);

        File logsDir = new File("logs");
        if (!logsDir.exists()) logsDir.mkdirs();
        File logFile = new File(logsDir, job.getId() + ".log");
        job.setLogPath(logFile.getAbsolutePath());
        jobs.save(job);

        int exit;
        String error = null;

        try {
            String[] cmd = OSCommands.wrapShell(job.getCommand());
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            try (InputStream in = p.getInputStream();
                 FileOutputStream out = new FileOutputStream(logFile, true)) {
                out.write(("=== Run at " + Instant.now() + " ===\n").getBytes());
                byte[] buf = new byte[8192];
                int r;
                while ((r = in.read(buf)) != -1) {
                    out.write(buf, 0, r);
                }
            }
            exit = p.waitFor();
        } catch (IOException | InterruptedException e) {
            exit = -1;
            error = e.getMessage();
            log.warn("Job {} failed to execute: {}", job.getId(), error);
            Thread.currentThread().interrupt();
        }

        job.setExitCode(exit);

        if (exit == 0) {
            job.setState(JobState.COMPLETED);
            job.setLockedBy(null);
            job.setLockedAt(null);
            job.setUpdatedAt(Instant.now());
            jobs.save(job);
            log.info("[{}] Job {} completed.", workerId, job.getId());
            return;
        }

        job.setAttempts(job.getAttempts() + 1);
        if (error != null) job.setLastError(error);
        int maxRetries = job.getMaxRetries() != null ? job.getMaxRetries() : cfg.maxRetries();
        int base = job.getBackoffBase() != null ? job.getBackoffBase() : cfg.backoffBase();

        if (job.getAttempts() > maxRetries) {
            job.setState(JobState.DEAD);
            job.setLockedBy(null);
            job.setLockedAt(null);
            job.setUpdatedAt(Instant.now());
            jobs.save(job);
            log.info("[{}] Job {} moved to DLQ after {} attempts.", workerId, job.getId(), job.getAttempts());
        } else {
            long delay = (long) Math.pow(base, job.getAttempts());
            job.setState(JobState.FAILED);
            job.setNextRunAt(Instant.now().plusSeconds(delay));
            job.setLockedBy(null);
            job.setLockedAt(null);
            job.setUpdatedAt(Instant.now());
            jobs.save(job);
            log.info("[{}] Job {} failed (exit={}), retry in {}s (attempt {}/{}).", workerId, job.getId(), exit, delay, job.getAttempts(), maxRetries);
        }
    }
}
