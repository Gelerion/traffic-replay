package com.gelerion.traffic.replay.core.service.scheduler.impl;

import com.gelerion.traffic.replay.core.service.scheduler.model.ScheduleDelay;
import com.gelerion.traffic.replay.core.service.scheduler.Scheduler;
import com.google.inject.Inject;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import org.cfg4j.provider.ConfigurationProvider;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class JavaExecutorScheduler implements Scheduler {
    private final ScheduledThreadPoolExecutor impl;

    @Inject
    public JavaExecutorScheduler(ConfigurationProvider config) {
        this.impl = new ScheduledThreadPoolExecutor(config.getProperty("scheduler.core-pool-size", Integer.class));
        this.impl.setRemoveOnCancelPolicy(true);

        registerMetrics();
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable trigger, ScheduleDelay delay) {
        return impl.schedule(trigger, delay.millis(), MILLISECONDS);
    }

    @Override
    public int scheduledTasks() {
        return impl.getQueue().size();
    }

    private void registerMetrics() {
        Metrics.gauge("scheduler", Tags.of("execution", "active-count"), impl, ThreadPoolExecutor::getActiveCount);
        Metrics.gauge("scheduler", Tags.of("execution", "pool-size"), impl, ThreadPoolExecutor::getPoolSize);
        Metrics.gauge("scheduler", Tags.of("execution", "task-count"), impl, ThreadPoolExecutor::getTaskCount);
        Metrics.gauge("scheduler", Tags.of("execution", "completed-task-count"), impl, ThreadPoolExecutor::getCompletedTaskCount);
        Metrics.gauge("scheduler", Tags.of("queue", "size"), impl, sched -> sched.getQueue().size());
    }
}
