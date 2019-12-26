package com.gelerion.traffic.replay.core.config.guice.modules;

import com.gelerion.traffic.replay.core.channels.AsyncChannel;
import com.gelerion.traffic.replay.core.channels.BoundedBufferedChannel;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import org.apache.kafka.clients.consumer.ConsumerRecord;

@Singleton
public class ChannelsModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(new TypeLiteral<AsyncChannel<ConsumerRecord<String, String>>>(){})
                .to(new TypeLiteral<BoundedBufferedChannel>(){});
    }
}
