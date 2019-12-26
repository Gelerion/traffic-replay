package com.gelerion.traffic.replay.core.service.scheduler.model;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;

@Data
@Builder
public class ScheduleDelay {
    Duration schedulingDelay;
    ZonedDateTime triggerFireTime;

    public boolean hasNegativeDelay() {
        return schedulingDelay.isNegative();
    }

    public long millis() {
        return schedulingDelay.toMillis();
    }

    public long differenceBetweenTriggerScheduledTimeAnd(ZonedDateTime time) {
        return Duration.between(triggerFireTime, time).toMillis();
    }
}
