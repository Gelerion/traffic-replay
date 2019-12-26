package com.gelerion.traffic.replay.core.service.scheduler;

import com.gelerion.traffic.replay.core.service.scheduler.model.ScheduleDelay;

import java.util.concurrent.ScheduledFuture;

public interface Scheduler {

    ScheduledFuture<?> schedule(Runnable trigger, ScheduleDelay delay);

    int scheduledTasks();

}
