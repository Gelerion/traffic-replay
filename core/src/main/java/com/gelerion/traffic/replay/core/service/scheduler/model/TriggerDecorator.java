package com.gelerion.traffic.replay.core.service.scheduler.model;

import com.gelerion.traffic.replay.api.event.Event;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;

@Slf4j
@Value(staticConstructor = "of")
public class TriggerDecorator {
    private static DistributionSummary schedulingAccuracyHistogram = Metrics.summary("scheduling", Tags.of("execution", "accuracy-ms"));
    private static Counter executed = Metrics.counter("scheduling", Tags.of("execution", "executed"));
    private static Counter responseFailureCounter = Metrics.counter("http", "request", "failure");


    private final Event event;
    private final ScheduleDelay schedulingTime;

    public Runnable measureAccuracy(Runnable trigger) {
        return () -> {
            ZonedDateTime now = ZonedDateTime.now();
            //0 is perfect, positive is running late
            long schedulingAccuracy = schedulingTime.differenceBetweenTriggerScheduledTimeAnd(now);
            schedulingAccuracyHistogram.record(schedulingAccuracy);

            log.debug("Scheduled for: {}\n\t" +
                    "Now: {}\n\t" +
                    "Scheduling accuracy: {}\n\t" +
                    "Event: {}", schedulingTime.getTriggerFireTime(), now, schedulingAccuracy, event);

            executed.increment();
            trigger.run();
        };
    }

    public Runnable logException(Runnable trigger) {
        return () -> {
            try {
                trigger.run();
            } catch (Exception e) {
                responseFailureCounter.increment();
                log.warn("Failed to execute trigger {}", event, e);
            }
        };
    }
}
