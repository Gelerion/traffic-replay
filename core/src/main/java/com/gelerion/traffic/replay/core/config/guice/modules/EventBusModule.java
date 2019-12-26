package com.gelerion.traffic.replay.core.config.guice.modules;

import com.gelerion.traffic.replay.core.service.scheduler.ScheduleService;
import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class EventBusModule extends AbstractModule {

    @Provides
    @Singleton
    @SuppressWarnings("all")
    EventBus eventBus(ScheduleService scheduleService) {
        EventBus eventBus = new EventBus();
        eventBus.register(scheduleService);

        return eventBus;
    }
}
