package com.gelerion.traffic.replay.druid.model;

import com.gelerion.traffic.replay.api.event.Event;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Optional;

@Data
@Builder
public class DruidEvent implements Event {
    public static final DruidEvent NON_VALID = new DruidEvent(Instant.MIN, -1, "");

    Instant startTime;
    long responseTime;
    String query;

    @Override
    public Instant timestamp() {
        return startTime;
    }

    @Override
    public Optional<Long> responseTimeMs() {
        return Optional.of(responseTime);
    }
}
