
package com.queuectl.cli;

import com.queuectl.domain.Job;
import com.queuectl.domain.JobState;
import com.queuectl.repo.JobRepository;
import com.queuectl.util.TimeUtil;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;

@Component
@Command(name="list", description = "List jobs (optionally by state)")
public class ListCommand implements Runnable {

    private final JobRepository jobs;

    public ListCommand(JobRepository jobs) {
        this.jobs = jobs;
    }

    @Option(names="--state", description="Filter by state (PENDING|PROCESSING|COMPLETED|FAILED|DEAD)")
    private String state;

    @Override
    public void run() {
        List<Job> list;
        if (state == null) {
            list = jobs.findTop100ByOrderByCreatedAtDesc();
        } else {
            JobState st = JobState.valueOf(state.toUpperCase());
            list = jobs.findTop100ByStateOrderByCreatedAtAsc(st);
        }
        System.out.printf("%-36s  %-11s  %-7s  %-20s  %s%n", "ID", "STATE", "ATT", "NEXT_RUN", "COMMAND");
        for (Job j: list) {
            System.out.printf("%-36s  %-11s  %-7d  %-20s  %s%n",
                    j.getId(), j.getState(), j.getAttempts(), TimeUtil.fmt(j.getNextRunAt()), j.getCommand());
        }
    }
}
