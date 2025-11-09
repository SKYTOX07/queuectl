
package com.queuectl.cli;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;

@Component
@Command(name = "queuectl", mixinStandardHelpOptions = true,
        version = "queuectl 1.0",
        subcommands = {
                EnqueueCommand.class,
                WorkerCommand.class,
                StatusCommand.class,
                ListCommand.class,
                DlqCommand.class,
                ConfigCommand.class
        },
        description = "CLI for managing background jobs and workers.")
public class QueueCtlCommand {}
