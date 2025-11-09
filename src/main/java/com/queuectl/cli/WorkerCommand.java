
package com.queuectl.cli;

import com.queuectl.service.ConfigService;
import com.queuectl.service.WorkerService;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Component
@Command(name="worker", description = "Manage background workers", subcommands = {
        WorkerStart.class, WorkerStop.class
})
public class WorkerCommand { }

@Component
@Command(name="start", description = "Start worker threads")
class WorkerStart implements Runnable {

    private final WorkerService workerService;

    public WorkerStart(WorkerService workerService) {
        this.workerService = workerService;
    }

    @Option(names="--count", description="Number of worker threads (default: 1)")
    private int count = 1;

    @Override
    public void run() {
        try {
            workerService.start(count);
            // block until interrupted; stop is signaled via config flag
            while (true) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("Failed to start workers: " + e.getMessage());
            System.exit(1);
        }
    }
}

@Component
@Command(name="stop", description = "Signal workers to stop gracefully (they finish current job)")
class WorkerStop implements Runnable {
    private final ConfigService cfg;
    public WorkerStop(ConfigService cfg) { this.cfg = cfg; }
    @Override
    public void run() {
        cfg.requestStop(true);
        System.out.println("Stop requested. Running workers will exit after finishing current job.");
    }
}
