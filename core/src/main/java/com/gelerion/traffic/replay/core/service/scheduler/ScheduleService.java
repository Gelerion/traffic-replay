package com.gelerion.traffic.replay.core.service.scheduler;

import com.gelerion.traffic.replay.api.RecordProcessor;
import com.gelerion.traffic.replay.api.event.Event;
import com.gelerion.traffic.replay.api.event.Timestamped;
import com.gelerion.traffic.replay.api.model.EventAndRequest;
import com.gelerion.traffic.replay.core.channels.AsyncChannel;
import com.gelerion.traffic.replay.core.http.client.HttpService;
import com.gelerion.traffic.replay.core.service.scheduler.model.ScheduleDelay;
import com.gelerion.traffic.replay.core.utils.ShutdownService;
import com.gelerion.traffic.replay.core.kafka.PartitionWhitelist;
import com.gelerion.traffic.replay.core.service.scheduler.impl.NoopScheduledFuture;
import com.gelerion.traffic.replay.core.service.scheduler.model.TriggerDecorator;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.cfg4j.provider.ConfigurationProvider;
import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.*;

import static java.time.temporal.ChronoUnit.MILLIS;

/**
 * Scheduling
 * Each request is scheduled based on a transformation of the requests original start-time.
 * The idea is to take requests that happened in the past and replay them at either their original rate or at a higher rate.
 * Three parameters determine the time a request is scheduled for.
 *
 * start-time, aka original-start-time -- The time when a particular request originally ran
 * query-cutoff-time -- Reference time for the original requests.  Request before this time will not be scheduled
 * scheduling-start-time -- Reference time for scheduling requests.  No requests will be scheduled before this time.
 * speedup-factor  -- How much faster to run the requests
 *
 * The formula for computing the new start-time of a scheduled request is
 * new-start-time = ((original-start-time - query-cutoff-time) / speedup-factor) + scheduling-start-time
 */
@Slf4j
@Singleton
public class ScheduleService {
    private final Counter skippedNegativeMeter  = Metrics.counter("scheduling", "status", "negative-delay");
    private final Counter scheduledMeter        = Metrics.counter("scheduling", "status", "scheduled");
    private final Counter skippedWhiteListMeter = Metrics.counter("scheduling", "status", "not-on-whitelist");

    private final AsyncChannel<ConsumerRecord<String, String>> kafkaConsumerChannel;
    private final RecordProcessor<? extends Timestamped> recordProcessor;
    private final PartitionWhitelist partitionWhitelist;
    private final HttpService httpService;
    private final Multimap<Integer, ScheduledFuture<?>> scheduledTasks;
    private final int maxScheduledTasks;

    private Scheduler scheduler;
    private ScheduleTimeCalculator scheduleTimeCalculator;

    private final ExecutorService schedulerServiceExecutor;

    @Inject
    public ScheduleService(ConfigurationProvider config,
                           AsyncChannel<ConsumerRecord<String, String>> kafkaConsumerChannel,
                           RecordProcessor<? extends Timestamped> recordProcessor,
                           HttpService httpService,
                           Scheduler scheduler,
                           ScheduleTimeCalculator scheduleTimeCalculator) {
        this.kafkaConsumerChannel = kafkaConsumerChannel;
        this.recordProcessor = recordProcessor;
        this.httpService = httpService;
        this.scheduler = scheduler;
        this.partitionWhitelist = PartitionWhitelist.INSTANCE;
        this.scheduledTasks = Multimaps.synchronizedListMultimap(ArrayListMultimap.create());
        this.scheduleTimeCalculator = scheduleTimeCalculator;
        this.maxScheduledTasks = config.getProperty("scheduler.max-scheduled-tasks", Integer.class);

        schedulerServiceExecutor = createSchedulerThread(kafkaConsumerChannel);
        createBackgroundFiredTasksRipperThread();
    }

    public void startScheduling() {
        schedulerServiceExecutor.execute(() ->
                ShutdownService.whileNotStopped(() -> {
                    //potentially infinity loop here...
                    while (!hasRoomInScheduleQueue()) {
                        sleep();
                    }

                    var kafkaRecord = kafkaConsumerChannel.get();

                    if (!partitionWhitelist.contains(kafkaRecord.partition())) {
                        log.info("Not executing the request because its kafka partition: {} is not in the whitelist: {}",
                                kafkaRecord.partition(), partitionWhitelist);
                        skippedWhiteListMeter.increment();
                        return;
                    }

                    recordProcessor.process(kafkaRecord.value())
                            .map(this::scheduleEvent)
                            .forEach(scheduledTask -> scheduledTasks.put(kafkaRecord.partition(), scheduledTask));

                    scheduledMeter.increment();
                })
        );
    }

    private ScheduledFuture<?> scheduleEvent(EventAndRequest eventAndRequest) {
        Event event = eventAndRequest.event();

        ScheduleDelay scheduleDelay = scheduleTimeCalculator.compute(event.timestamp());
        if (scheduleDelay.hasNegativeDelay()) {
            skippedNegativeMeter.increment();
            return new NoopScheduledFuture<>();
        }

        Runnable trigger = httpService.createTrigger(eventAndRequest);

        TriggerDecorator decorator = TriggerDecorator.of(event, scheduleDelay);
        Runnable decoratedTrigger = decorator.logException(decorator.measureAccuracy(trigger));

        return scheduler.schedule(decoratedTrigger, scheduleDelay);
    }

    @SuppressWarnings("all")
    @Subscribe
    public void onPartitionRevoked(Set<Integer> revokedPartitions) {
        //should be executed in the thread that invokes execute()
        //it's safe as we use synchronized map
        revokedPartitions.forEach(partition -> {
            Collection<ScheduledFuture<?>> tasks = scheduledTasks.get(partition);
            if (tasks.size() > 0) log.info("Cancelling {} tasks from partition: {}", tasks.size(), partition);
            tasks.forEach(task -> task.cancel(true));
        });
    }

    private void removeFiredTasks() {
        scheduledTasks.values().removeIf(Future::isDone);
    }

    @SneakyThrows
    private void sleep() {
        Thread.sleep(1000);
    }

    private boolean hasRoomInScheduleQueue() {
        return scheduler.scheduledTasks() <= maxScheduledTasks;
    }

    private void createBackgroundFiredTasksRipperThread() {
        Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("fired-task-ripper-thread")
                .build())
                .scheduleAtFixedRate(this::removeFiredTasks, 20, 20, TimeUnit.SECONDS);
    }

    @NotNull
    private ExecutorService createSchedulerThread(AsyncChannel<ConsumerRecord<String, String>> kafkaConsumerChannel) {
        return Executors.newSingleThreadExecutor(new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("schedule-service-thread")
                .setUncaughtExceptionHandler((thread, ex) -> {
                    log.error("Unexpected error - {} - shutting down", thread, ex);
                    kafkaConsumerChannel.interrupt(); //notify consumer side is dead
                    ShutdownService.shutDown();
                })
                .build());
    }

    public static void main(String[] args) throws ParseException {
        SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
        ft.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date parse = ft.parse("2019-11-14T00:00:00");
        long shedTimeMilis = parse.getTime();

        ZonedDateTime schedTime = ZonedDateTime.parse("2019-11-13T00:00:00.000Z");
        ZonedDateTime absTime = schedTime.plus(Duration.ofHours(3));
        System.out.println("absTime = " + absTime.toInstant().toEpochMilli());

        long absTimeMillis = ft.parse("2019-11-14T03:00:00").getTime();
        System.out.println("absTimeMillis = " + absTimeMillis);

        Instant now = Instant.now();
        long nowMilis = System.currentTimeMillis();

        ZonedDateTime betweenMinusMillis = absTime.minus(nowMilis, MILLIS);
        System.out.println("betweenMinusMillis = " + betweenMinusMillis.toInstant().toEpochMilli());
        Duration between = Duration.between(now, absTime.toInstant());
        System.out.println("between = " + between.toMillis());
        System.out.println("between.isNegative() = " + between.isNegative());

        Duration betweenMilis = Duration.ofMillis(absTime.toInstant().toEpochMilli() - nowMilis);
        System.out.println("betweenMilis = " + betweenMilis.toMillis());

        System.out.println(absTimeMillis - nowMilis);
    }
}
