package com.gelerion.traffic.replay.core.service.scheduler.impl;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.*;

public class NoopScheduledFuture<V> implements ScheduledFuture<V> {
    @Override
    public long getDelay(@NotNull TimeUnit unit) {
        return -1;
    }

    @Override
    public int compareTo(@NotNull Delayed o) {
        return -1;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return true;
    }

    @Override
    public boolean isCancelled() {
        return true;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public V get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }
}
