package com.gelerion.traffic.replay.api.event;

import java.time.Instant;

public interface Timestamped {
    Instant timestamp();
}
