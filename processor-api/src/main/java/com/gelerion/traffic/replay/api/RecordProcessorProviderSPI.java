package com.gelerion.traffic.replay.api;

import com.gelerion.traffic.replay.api.event.Event;
import org.cfg4j.provider.ConfigurationProvider;

public interface RecordProcessorProviderSPI {
    String name();
    RecordProcessor<? extends Event> processor(ConfigurationProvider config);
}
