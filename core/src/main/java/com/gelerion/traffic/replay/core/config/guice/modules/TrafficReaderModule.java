package com.gelerion.traffic.replay.core.config.guice.modules;

import com.gelerion.traffic.replay.core.kafka.TrafficReader;
import com.google.inject.AbstractModule;

public class TrafficReaderModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(TrafficReader.class);
    }
}
