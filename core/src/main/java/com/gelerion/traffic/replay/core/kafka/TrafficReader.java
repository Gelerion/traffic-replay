package com.gelerion.traffic.replay.core.kafka;

import com.gelerion.traffic.replay.core.channels.BoundedBufferedChannel;
import com.gelerion.traffic.replay.core.kafka.builder.KafkaConsumerProvider;
import com.gelerion.traffic.replay.core.utils.ShutdownService;
import com.google.inject.Inject;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Metrics;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.cfg4j.provider.ConfigurationProvider;

import java.time.Duration;

@Slf4j
public class TrafficReader {
    private final DistributionSummary consumed = Metrics.summary("kafka", "consumer", "consumed"); //backed by histogram
    private final Counter produced = Metrics.counter("kafka", "consumer", "produced"); //backed by meter

    private final ConfigurationProvider config;
    private final KafkaConsumerProvider kafkaConsumerProvider;
    private final BoundedBufferedChannel channel;

    @Inject
    public TrafficReader(ConfigurationProvider config,
                         KafkaConsumerProvider kafkaConsumerProvider,
                         BoundedBufferedChannel channel) {
        this.config = config;
        this.kafkaConsumerProvider = kafkaConsumerProvider;
        this.channel = channel;
    }

    public void startProcessing() {
        //N threads?
        try (KafkaConsumer<String, String> consumer = kafkaConsumerProvider.subscribeAndGet()) {
            ShutdownService.whileNotStopped(() -> {
                //consumers must keep polling Kafka or they will be considered dead and the partitions
                //they are consuming will be handed to another consumer in the group to continue consuming
                //controls how long poll() will block if data is not available in the consumer buffer
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                consumed.record(records.count());

                //log.info("Pooled {} records", records.count());
                for (ConsumerRecord<String, String> record : records) {
                    channel.put(record);
                    produced.increment();
                }
            });
        } catch (Exception e) {
            log.error("Failed to start", e);
            ShutdownService.shutDown();
        }
    }
}
