package com.gelerion.traffic.replay.core.kafka.builder;

import com.gelerion.traffic.replay.core.kafka.SeekToOffsetsListener;
import com.gelerion.traffic.replay.core.service.scheduler.ScheduleTimeCalculator;
import com.gelerion.traffic.replay.core.zk.ZkClient;
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.cfg4j.provider.ConfigurationProvider;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;

@Slf4j
public class KafkaConsumerProvider {
    private final ConfigurationProvider config;
    private final ZkClient zkClient;
    private final ScheduleTimeCalculator scheduleTimeCalculator;
    private final EventBus eventBus;

    @Inject
    public KafkaConsumerProvider(ConfigurationProvider config,
                                 ZkClient zkClient,
                                 ScheduleTimeCalculator scheduleTimeCalculator,
                                 EventBus eventBus) {
        this.config = config;
        this.zkClient = zkClient;
        this.scheduleTimeCalculator = scheduleTimeCalculator;
        this.eventBus = eventBus;
    }

    public KafkaConsumer<String, String> subscribeAndGet() {
        //preferable approach:
        //create kafka props out of a dynamically reloaded config file
        //Map map = config.getProperty("kafka", Map.class);

//        String groupId = UUID.randomUUID().toString();
        String subscribeTopics = config.getProperty("kafka.topics", String.class);
        String groupId = config.getProperty("general.test-id", String.class);

        ImmutableMap<String, Object> kafkaProps = ImmutableMap.<String, Object>builder()
                .put(BOOTSTRAP_SERVERS_CONFIG, zkClient.getBrokerList()) //List of host:port pairs of Kafka brokers
                .put(GROUP_ID_CONFIG, groupId)
                .put(KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
                .put(VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer")
                .put(AUTO_OFFSET_RESET_CONFIG, "earliest")
                .put(ENABLE_AUTO_COMMIT_CONFIG, false)
                .put(MAX_POLL_INTERVAL_MS_CONFIG, (int) SECONDS.toMillis(2))
                .build();

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(kafkaProps);
        var seekToOffsetsListener = new SeekToOffsetsListener(config, consumer, eventBus, scheduleTimeCalculator);

        List<String> topics = Collections.singletonList(subscribeTopics);
        log.info("Kafka is about to subscribe to {} with group id {}", topics, groupId);

        //if enable.auto.commit is set to true (which is the default) the consumer automatically triggers offset commits
        //periodically according to the interval configured with “auto.commit.interval.ms.”
        consumer.subscribe(topics, seekToOffsetsListener);
        return consumer;
    }

}
