
package com.queuectl.util;

public class OSCommands {
    public static String[] wrapShell(String command) {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return new String[] {"cmd.exe", "/c", command};
        } else {
            return new String[] {"sh", "-c", command};
        }
    }
}
