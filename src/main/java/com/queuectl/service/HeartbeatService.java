
package com.queuectl.service;

import com.queuectl.domain.WorkerHeartbeat;
import com.queuectl.repo.WorkerHeartbeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;

@Service
public class HeartbeatService {
    private static final Logger log = LoggerFactory.getLogger(HeartbeatService.class);
    private final WorkerHeartbeatRepository repo;
    private final ConfigService cfg;

    private Timer timer;
    private String prefix;
    private int threads;

    public HeartbeatService(WorkerHeartbeatRepository repo, ConfigService cfg) {
        this.repo = repo;
        this.cfg = cfg;
    }

    public void startBackground(String workerPrefix, int threads) throws Exception {
        stopBackground();
        this.prefix = workerPrefix;
        this.threads = threads;
        String host = InetAddress.getLocalHost().getHostName();
        long pid = ProcessHandle.current().pid();
        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    WorkerHeartbeat hb = repo.findById(prefix).orElse(new WorkerHeartbeat(prefix));
                    hb.setHost(host);
                    hb.setPid(pid);
                    hb.setThreads(threads);
                    hb.setUpdatedAt(Instant.now());
                    repo.save(hb);
                } catch (Exception e) {
                    log.warn("Heartbeat update failed: {}", e.toString());
                }
            }
        }, 0, Math.max(1000, cfg.heartbeatIntervalMs()));
    }

    public void stopBackground() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (prefix != null) {
            try {
                repo.deleteById(prefix);
            } catch (Exception ignored) {}
        }
    }
}
