
package com.queuectl.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TimeUtil {
    private static final DateTimeFormatter fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault());
    public static String fmt(Instant t) {
        if (t == null) return "-";
        return fmt.format(t);
    }
}
