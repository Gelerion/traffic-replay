package com.gelerion.traffic.replay.core.service.scheduler;

import com.gelerion.traffic.replay.core.service.scheduler.model.ScheduleDelay;
import com.google.inject.Inject;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Metrics;
import lombok.extern.slf4j.Slf4j;
import org.cfg4j.provider.ConfigurationProvider;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;

import static java.time.temporal.ChronoUnit.MILLIS;

@Slf4j
public class ScheduleTimeCalculator {
    private static final ScheduleDelay NEGATIVE_DELAY = ScheduleDelay.builder()
            .schedulingDelay(Duration.ofMillis(-1))
            .build();

    private final DistributionSummary schedulingDelayHistogram = Metrics.summary("scheduling", "time", "delay-ms");
    private final DistributionSummary relativeTimeHistogram    = Metrics.summary("scheduling", "time", "relative");
    private final Counter skippedTooEarlyMeter                 = Metrics.counter("scheduling", "status", "too-early");

    private long speedupFactor;
    private ZonedDateTime queryCutOffTime;
    private ZonedDateTime schedulingStartTime;

    @Inject
    public ScheduleTimeCalculator(ConfigurationProvider config) {
        this.speedupFactor = config.getProperty("scheduler.speedup-factor", Long.class);
        this.queryCutOffTime = ZonedDateTime.parse(config.getProperty("scheduler.query-cutoff-time", String.class));
        this.schedulingStartTime = ZonedDateTime.parse(config.getProperty("scheduler.scheduling-start-time", String.class));
    }

     /*
        schedule-time = scheduling-start-time + ((original-start-time - query-cutoff-time) / speedup-factor)

        e.g.
        original start time is   2019-10-28T02:11:17.036Z
        query cutoff time is     2019-10-28T00:00:00.000Z
        scheduling start time is 2019-10-29T00:00:00.000Z
        speedup factor is        2

        Duration.between(originalStartTime, queryCutOffTime) is 2H-11M-15S
        That means the query will be scheduled for execution in ~2 hours relatively to the scheduling start time

        To speed it up we divide the difference by the speedup factor
        2H-11M-15S / 2 = 1H-05M-7S

        Calculate absolute scheduling time (supposed to be future time)
        schedulingStartTime.plus(1H-05M-7S)

        The actual time when event will be fired is 2019-10-29T01:05:07.036Z
     */
     public ScheduleDelay compute(Instant originalStartTime) {
         if (queryCutOffTime.toInstant().isAfter(originalStartTime)) {
             skippedTooEarlyMeter.increment();
             return NEGATIVE_DELAY;
         }

        Duration relativeStartTime = Duration.ofMillis(
                MILLIS.between(queryCutOffTime.toInstant(), originalStartTime) / speedupFactor
        );

        ZonedDateTime absoluteStartTime = schedulingStartTime.plus(relativeStartTime);

        // The java scheduler only accepts a delay from now, so we need to turn
        // absolute start time into a delay from now in order to schedule t"he job
        Duration schedulingDelay = Duration.between(Instant.now(), absoluteStartTime.toInstant());

        log.trace("original-start-time: {}\n\t" +
                "relative-start-time: {}\n\t" +
                "absolute-start-time: {}\n\t" +
                "scheduling-delay: {}", originalStartTime, relativeStartTime, absoluteStartTime, schedulingDelay);

        relativeTimeHistogram.record(relativeStartTime.toMillis());
        schedulingDelayHistogram.record(schedulingDelay.toMillis());

        return ScheduleDelay.builder()
                .triggerFireTime(absoluteStartTime)
                .schedulingDelay(schedulingDelay)
                .build();
    }

    /*
        original-start-time = query-cutoff-time + ((schedule-time - scheduling-start-time) * speedup-factor)

        e.g.
        schedule time is         2019-10-29T02:25:00.000Z
        scheduling start time is 2019-10-29T00:00:00.000Z
        query cutoff time is     2019-10-28T00:00:00.000Z
        speedup factor is        2

        Duration.between(scheduleTime, schedulingStartTime) is 2H-25M
        That means the query was scheduled for execution in ~2 hours relatively to the scheduling start time

        To get the original duration we multiply the difference by speedup factor
        2H-25M * 2 = 4H-50M

        Calculate original start time
        queryCutOffTime.plus(4H-50M)

        The original start time was 2019-10-28T04:50:00.000Z
     */
    public ZonedDateTime computeOriginalStartTimeForScheduleTime(ZonedDateTime scheduleTime) {
        Duration durationSinceStartTime = Duration.ofMillis(
                MILLIS.between(scheduleTime, schedulingStartTime) * speedupFactor
        );

        return queryCutOffTime.plus(durationSinceStartTime);
    }

}
