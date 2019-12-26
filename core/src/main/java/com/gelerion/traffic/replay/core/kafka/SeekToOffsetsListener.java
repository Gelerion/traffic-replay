package com.gelerion.traffic.replay.core.kafka;

import com.gelerion.traffic.replay.core.service.scheduler.ScheduleTimeCalculator;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.TopicPartition;
import org.cfg4j.provider.ConfigurationProvider;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@Slf4j
public class SeekToOffsetsListener implements ConsumerRebalanceListener {
    private final PartitionWhitelist partitionWhitelist;
    private final KafkaConsumer<?, ?> consumer;
    private final EventBus eventBus;
    private final boolean useTimestampSeek;

    //keep track of the partitions that we own across rebalances.  If we get
    //back the same partitions that we previously owned, we don't need to reset
    //the offsets or erase our scheduled state.
    private Set<Integer> partitionsWeOwn = new HashSet<>();
    private ScheduleTimeCalculator scheduleTimeCalculator;

    public SeekToOffsetsListener(ConfigurationProvider config,
                                 KafkaConsumer<?, ?> consumer,
                                 EventBus eventBus,
                                 ScheduleTimeCalculator scheduleTimeCalculator) {
        this.consumer = consumer;
        this.eventBus = eventBus;
        this.partitionWhitelist = PartitionWhitelist.INSTANCE;
        this.scheduleTimeCalculator = scheduleTimeCalculator;
        this.useTimestampSeek = config.getProperty("kafka.use-timestamp-seek", Boolean.class);
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> topicPartitions) {
        //after rebalance there is a good chance that we will get back most of the partitions that we currently have.
        Set<Integer> assignedPartitions = topicPartitions.stream().map(TopicPartition::partition)
                .collect(toSet());

        Sets.SetView<Integer> noLongerOwnedPartitions = Sets.difference(partitionsWeOwn, assignedPartitions);
        Sets.SetView<Integer> newPartitions = Sets.difference(assignedPartitions, partitionsWeOwn);

        Set<TopicPartition> newTopicPartitions = topicPartitions.stream()
                .filter(it -> newPartitions.contains(it.partition()))
                .collect(toSet());

        log.info("Partition assignment: {}\n\t" +
                "Newly assigned partitions: {}\n\t" +
                "Partitions we no longer own: {}\n\t", assignedPartitions, newPartitions, noLongerOwnedPartitions);

        moveOffsets(newTopicPartitions);

        partitionWhitelist.add(assignedPartitions);
        eventBus.post(noLongerOwnedPartitions); //cancel scheduled tasks for partitions we no longer own, see ScheduleService.onPartitionRevoked
        partitionsWeOwn = assignedPartitions;
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> topicPartitions) {
        Set<Integer> revokedPartitions = topicPartitions.stream().map(TopicPartition::partition).collect(toSet());
        partitionWhitelist.remove(revokedPartitions);
    }

    private void moveOffsets(Collection<TopicPartition> partitions) {
        if (useTimestampSeek) {
            log.info("Seeking to offsets for timestamp");
            long timestampToSeek = scheduleTimeCalculator.computeOriginalStartTimeForScheduleTime(ZonedDateTime.now())
                    .toInstant().toEpochMilli();

            Map<TopicPartition, Long> timestampsToSearch = partitions.stream()
                    .collect(toMap(identity(), key -> timestampToSeek));

            Map<TopicPartition, OffsetAndTimestamp> newOffsets = consumer.offsetsForTimes(timestampsToSearch);
            newOffsets.forEach((partition, offsetAndTimestamp) -> {
                log.info("Moving {} to offset {}", partition, offsetAndTimestamp.offset());
                consumer.seek(partition, offsetAndTimestamp.offset());
            });
        } else {
            log.info("Seeking to beginning");
            consumer.seekToBeginning(partitions);
        }
    }
}
