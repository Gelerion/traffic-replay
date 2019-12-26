package com.gelerion.traffic.replay.core.config.guice;

import com.gelerion.traffic.replay.core.config.guice.modules.*;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

public class Initializer {

    public static Injector makeInjector() {
        return Guice.createInjector(Modules.combine(
                new ConfigModule(),
                new ChannelsModule(),
                new HttpClientModule(),
                new ZookeeperModule(),
                new TrafficReaderModule(),
                new RecordProcessorModule(),
                new SchedulerModule(),
                new EventBusModule()
        ));
    }


}
