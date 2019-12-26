package com.gelerion.traffic.replay.core.utils;

import java.util.concurrent.atomic.AtomicBoolean;

public class ShutdownService {
    private static final AtomicBoolean stopped = new AtomicBoolean();

    public static void shutDown() {
        boolean result;
        do {
            if (stopped.get()) return; //avoid infinity loop when called multiple times
            result = stopped.compareAndSet(false, true);
        } while (!result);
    }

    public static boolean isCancelled() {
        return stopped.get();
    }

    public static void whileNotStopped(Runnable action) {
        while (!isCancelled()) {
            action.run();
        }
    }
}
