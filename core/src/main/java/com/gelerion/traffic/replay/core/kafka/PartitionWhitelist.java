package com.gelerion.traffic.replay.core.kafka;

import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public enum  PartitionWhitelist {
    INSTANCE;

    private final Set<Integer> whitelist;

    PartitionWhitelist() {
        this.whitelist = ConcurrentHashMap.newKeySet();
    }

    public boolean contains(int partition) {
        return whitelist.contains(partition);
    }

    public void add(Set<Integer> assignedPartitions) {
        whitelist.addAll(assignedPartitions);
        log.info("Added partitions to whitelist: {}. New whitelist: {}", assignedPartitions, whitelist);
    }

    public void remove(Set<Integer> revokedPartitions) {
        whitelist.removeAll(revokedPartitions);
        log.info("Removed partitions from whitelist: {}. New whitelist: {}", revokedPartitions, whitelist);
    }

    @Override
    public String toString() {
        return whitelist.toString();
    }
}
