package com.gelerion.traffic.replay.core.kafka.types;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public class KafkaRecords {
    public static final ConsumerRecord<String, String> EMPTY_RECORD =
            new ConsumerRecord<>("", -1, -1L, "", "");
}
