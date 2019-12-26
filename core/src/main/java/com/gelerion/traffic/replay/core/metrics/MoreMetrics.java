package com.gelerion.traffic.replay.core.metrics;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.dropwizard.DropwizardMeterRegistry;

public class MoreMetrics {
    private static MetricRegistry dropwizardRegistry = Metrics.globalRegistry.getRegistries().stream()
            .filter(registry -> registry instanceof DropwizardMeterRegistry)
            .map(it -> (DropwizardMeterRegistry) it)
            .findFirst()
            .orElseThrow()
            .getDropwizardRegistry();

    public static Histogram histogram(String name, String... tags) {
        String metricName = name + "." + String.join(".", tags);
        return dropwizardRegistry.histogram(metricName);
    }
}
