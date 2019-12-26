package com.gelerion.traffic.replay.druid;

import com.gelerion.traffic.replay.api.RecordProcessor;
import com.gelerion.traffic.replay.api.RecordProcessorProviderSPI;
import com.gelerion.traffic.replay.api.event.Timestamped;
import com.gelerion.traffic.replay.druid.config.DruidConfig;
import org.cfg4j.provider.ConfigurationProvider;

public class DruidRecordProcessorProvider implements RecordProcessorProviderSPI {
    @Override
    public String name() {
        return "druid";
    }

    @Override
    public RecordProcessor<? extends Timestamped> processor(ConfigurationProvider config) {
        return new DruidRecordProcessor(config.bind(name(), DruidConfig.class));
    }
}
