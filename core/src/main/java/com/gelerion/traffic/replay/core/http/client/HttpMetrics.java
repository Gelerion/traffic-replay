package com.gelerion.traffic.replay.core.http.client;

import com.google.common.collect.Sets;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import lombok.Value;

import java.util.Set;
import java.util.concurrent.TimeUnit;

class HttpMetrics {
    private Set<TimeWindow> timeWindows;

    HttpMetrics() {
        this.timeWindows = Sets.newHashSet(
                TimeWindow.of(0 ,1,
                        Metrics.counter("http", "response", "time", "new", "under-1s"),
                        Metrics.counter("http", "response", "time", "old", "under-1s")),
                TimeWindow.of(1 ,5,
                        Metrics.counter("http", "response", "time", "new", "1-5s"),
                        Metrics.counter("http", "response", "time", "old", "1-5s")),
                TimeWindow.of(5 ,10,
                        Metrics.counter("http", "response", "time", "new", "5-10s"),
                        Metrics.counter("http", "response", "time", "old", "5-10s")),
                TimeWindow.of(10 ,30,
                        Metrics.counter("http", "response", "time", "new", "10-30s"),
                        Metrics.counter("http", "response", "time", "old", "10-30s")),
                TimeWindow.of(30 ,60,
                        Metrics.counter("http", "response", "time", "new", "30-60s"),
                        Metrics.counter("http", "response", "time", "old", "30-60s")),
                TimeWindow.of(60 ,Integer.MAX_VALUE,
                        Metrics.counter("http", "response", "time", "new", "above-60s"),
                        Metrics.counter("http", "response", "time", "old", "above-60s"))
        );
    }

    void reportQueryTime(long queryTimeMs, Type type) {
        long queryTimeSec = TimeUnit.MILLISECONDS.toSeconds(queryTimeMs);

        timeWindows.stream()
                .filter(window -> window.start <= queryTimeSec && window.end > queryTimeSec)
                .map(window -> window.counter(type))
                .findFirst()
                .ifPresent(Counter::increment);
    }

    enum Type {
        NEW, OLD
    }

    @Value(staticConstructor = "of")
    private static class TimeWindow {
        int start;
        int end;
        Counter newCounter;
        Counter oldCounter;

        Counter counter(HttpMetrics.Type type) {
            return type == Type.NEW ? newCounter : oldCounter;
        }
    }
}
