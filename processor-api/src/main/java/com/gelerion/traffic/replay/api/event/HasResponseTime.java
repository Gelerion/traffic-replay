package com.gelerion.traffic.replay.api.event;

import java.util.Optional;

public interface HasResponseTime {
    Optional<Long> responseTimeMs();
}
