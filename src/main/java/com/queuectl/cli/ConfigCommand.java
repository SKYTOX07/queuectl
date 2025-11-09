
package com.queuectl.cli;

import com.queuectl.service.ConfigService;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Component
@Command(name="config", description = "Manage configuration (retry, backoff, etc.)",
        subcommands = { ConfigSet.class, ConfigGet.class })
public class ConfigCommand { }

@Component
@Command(name="set", description = "Set a configuration key to a value, e.g. max_retries 3")
class ConfigSet implements Runnable {
    private final ConfigService cfg;
    public ConfigSet(ConfigService cfg) { this.cfg = cfg; }

    @Parameters(index="0", description="Key (e.g., max_retries, backoff_base, poll_interval_ms)")
    private String key;

    @Parameters(index="1", description="Value")
    private String value;

    @Override
    public void run() {
        cfg.set(key, value);
        System.out.printf("Config set: %s=%s%n", key, value);
    }
}

@Component
@Command(name="get", description = "Get a configuration value")
class ConfigGet implements Runnable {
    private final ConfigService cfg;
    public ConfigGet(ConfigService cfg) { this.cfg = cfg; }

    @Parameters(index="0", description="Key")
    private String key;

    @Override
    public void run() {
        String val = cfg.get(key, "(unset)");
        System.out.printf("%s=%s%n", key, val);
    }
}
