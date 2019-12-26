package com.gelerion.traffic.replay.core.config.guice.modules;

import com.gelerion.traffic.replay.core.zk.ZkClient;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.cfg4j.provider.ConfigurationProvider;

public class ZookeeperModule extends AbstractModule {

    @Provides
    ZkClient zkClient(ConfigurationProvider config) {
        return new ZkClient(config.getProperty("zk.url", String.class));
    }
}
