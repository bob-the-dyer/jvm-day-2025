package ru.spb.kupchinolab.jvmday2025.dining_philosophers;

import java.time.Instant;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.String.format;

public class Utils {

    private final static int TIME_TO_RUN_IN_MILLIS = 10_000;
    public final static int PHILOSOPHERS_COUNT = 1_000;

    public static void exitAfterDelay(AtomicLong stats) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("  end at " + Instant.now());
                System.out.println(format("stopping Main with overall eat attempts %s", stats.longValue()));
                System.exit(0);
            }
        }, TIME_TO_RUN_IN_MILLIS);
    }

}
