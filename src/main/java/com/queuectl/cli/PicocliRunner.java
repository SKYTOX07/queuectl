
package com.queuectl.cli;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import picocli.CommandLine;

@Component
public class PicocliRunner implements CommandLineRunner {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private QueueCtlCommand queueCtlCommand;

    @Override
    public void run(String... args) throws Exception {
        CommandLine.IFactory factory = new SpringFactory(applicationContext);
        CommandLine cmd = new CommandLine(queueCtlCommand, factory);
        if (args.length == 0) {
            cmd.usage(System.out);
            return;
        }
        int exit = cmd.execute(args);
        System.exit(exit);
    }
}
