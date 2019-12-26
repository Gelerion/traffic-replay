package com.gelerion.traffic.replay.core.kafka.types;

import com.google.common.reflect.TypeToken;

public interface KafkaKey {
    <R> R value();
}
