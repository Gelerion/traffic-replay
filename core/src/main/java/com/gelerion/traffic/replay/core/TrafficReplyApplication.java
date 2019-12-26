package com.gelerion.traffic.replay.core;

import com.gelerion.traffic.replay.core.config.guice.Initializer;
import com.gelerion.traffic.replay.core.kafka.TrafficReader;
import com.gelerion.traffic.replay.core.metrics.PrefixStrategies;
import com.gelerion.traffic.replay.core.metrics.Reporters;
import com.gelerion.traffic.replay.core.service.scheduler.ScheduleService;
import com.gelerion.traffic.replay.core.utils.ShutdownService;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TrafficReplyApplication {

    //suppress warning: VM Options, --add-opens java.base/java.lang=ALL-UNNAMED
    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(ShutdownService::shutDown));

        //setup metrics
        Reporters.setupDefaultReporters(PrefixStrategies.SIMPLE)
                .withLogging()
                .startReporting();

        //DI
        Injector injector = Initializer.makeInjector();

        ScheduleService scheduler = injector.getInstance(ScheduleService.class);
        scheduler.startScheduling();

        TrafficReader trafficReader = injector.getInstance(TrafficReader.class);
        trafficReader.startProcessing();
    }
}
