package com.gelerion.traffic.replay.core.config.guice.modules;

import com.gelerion.traffic.replay.api.RecordProcessor;
import com.gelerion.traffic.replay.api.event.Event;
import com.gelerion.traffic.replay.api.event.Timestamped;
import com.gelerion.traffic.replay.api.RecordProcessorProviderSPI;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.cfg4j.provider.ConfigurationProvider;

import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;

@Slf4j
public class RecordProcessorModule extends AbstractModule {

    @Provides
    @Singleton
    RecordProcessor<? extends Timestamped> eventsProcessor(ConfigurationProvider config) {
        String extensionName = config.getProperty("extensions.provider.name", String.class);

        RecordProcessorProviderSPI recordProcessorProvider = ServiceLoader.load(RecordProcessorProviderSPI.class)
                .stream()
                .map(Provider::get)
                .filter(processor -> processor.name().equals(extensionName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("There are no implementations of EventProcessorSPI named " + extensionName + " found on the classpath"));

        RecordProcessor<? extends Event> result = recordProcessorProvider.processor(config);
        log.info("Event processor implementation -- {}", result.getClass().getSimpleName());
        return result;
    }
}
