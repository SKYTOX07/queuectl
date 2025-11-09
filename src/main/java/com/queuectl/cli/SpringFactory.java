
package com.queuectl.cli;

import org.springframework.context.ApplicationContext;
import picocli.CommandLine;

public class SpringFactory implements CommandLine.IFactory {
    private final ApplicationContext ctx;
    public SpringFactory(ApplicationContext ctx) { this.ctx = ctx; }
    @Override
    public <K> K create(Class<K> cls) throws Exception {
        return ctx.getBean(cls);
    }
}
