
package com.queuectl.service;

import com.queuectl.domain.KeyValueConfig;
import com.queuectl.repo.KeyValueConfigRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ConfigService {
    private final KeyValueConfigRepository repo;

    @Value("${queuectl.defaults.max_retries:3}")
    private int defaultMaxRetries;

    @Value("${queuectl.defaults.backoff_base:2}")
    private int defaultBackoffBase;

    @Value("${queuectl.defaults.poll_interval_ms:1000}")
    private long defaultPollMs;

    @Value("${queuectl.defaults.heartbeat_interval_ms:2000}")
    private long defaultHeartbeatMs;

    public ConfigService(KeyValueConfigRepository repo) {
        this.repo = repo;
    }

    public void set(String key, String value) {
        repo.save(new KeyValueConfig(key, value));
    }

    public Optional<String> getOptional(String key) {
        return repo.findById(key).map(KeyValueConfig::getV);
    }

    public String get(String key, String fallback) {
        return getOptional(key).orElse(fallback);
    }

    public int maxRetries() {
        return Integer.parseInt(get("max_retries", String.valueOf(defaultMaxRetries)));
    }

    public int backoffBase() {
        return Integer.parseInt(get("backoff_base", String.valueOf(defaultBackoffBase)));
    }

    public long pollIntervalMs() {
        return Long.parseLong(get("poll_interval_ms", String.valueOf(defaultPollMs)));
    }

    public long heartbeatIntervalMs() {
        return Long.parseLong(get("heartbeat_interval_ms", String.valueOf(defaultHeartbeatMs)));
    }

    public boolean stopRequested() {
        return Boolean.parseBoolean(get("workers.stop", "false"));
    }

    public void requestStop(boolean stop) {
        set("workers.stop", String.valueOf(stop));
    }
}
