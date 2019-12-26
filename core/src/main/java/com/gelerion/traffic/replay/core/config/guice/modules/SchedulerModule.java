package com.gelerion.traffic.replay.core.config.guice.modules;

import com.gelerion.traffic.replay.core.http.client.HttpService;
import com.gelerion.traffic.replay.core.http.client.OkHttpService;
import com.gelerion.traffic.replay.core.service.scheduler.ScheduleService;
import com.gelerion.traffic.replay.core.service.scheduler.Scheduler;
import com.gelerion.traffic.replay.core.service.scheduler.impl.JavaExecutorScheduler;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

@Singleton
public class SchedulerModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Scheduler.class).to(JavaExecutorScheduler.class);
        bind(HttpService.class).to(OkHttpService.class);
        bind(ScheduleService.class);
    }
}
