package com.gelerion.traffic.replay.core.metrics;

import com.codahale.metrics.*;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.gelerion.traffic.replay.core.utils.EnvUtil;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.dropwizard.DropwizardClock;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;
import io.micrometer.core.instrument.logging.LoggingRegistryConfig;
import io.micrometer.graphite.GraphiteConfig;
import io.micrometer.graphite.GraphiteHierarchicalNameMapper;
import io.micrometer.graphite.GraphiteMeterRegistry;
import io.micrometer.statsd.StatsdConfig;
import io.micrometer.statsd.StatsdFlavor;
import io.micrometer.statsd.StatsdMeterRegistry;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
public class Reporters {
    private PrefixStrategy prefixStrategy = () -> "";

    private final List<Supplier<MeterRegistry>> reporters = new ArrayList<>();
    private boolean reportJvmMetrics = true;
    private boolean reportToJmx = true;

    private Reporters() {}

    private Reporters(PrefixStrategy prefixStrategy) {
        this.prefixStrategy = prefixStrategy;
    }

    public static Reporters setupDefaultReporters() {
        return new Reporters()
                .withJMXMetrics(false)
                .withJvmMetrics(true)
                .withGraphite(createGraphiteAddress(), 60);
    }

    public static Reporters setupDefaultReporters(final PrefixStrategy prefixStrategy) {
        return new Reporters(prefixStrategy)
                .withJMXMetrics(false)
                .withJvmMetrics(true)
                .withGraphite(createGraphiteAddress(), 60);
    }

    public static Reporters setup(final PrefixStrategy prefixStrategy) {
        return new Reporters(prefixStrategy);
    }

    public static Reporters setup() {
        return new Reporters();
    }

    public Reporters withStatsD(final InetSocketAddress address, final long reportPeriodSec) {
        reporters.add(() -> StatsdMeterRegistry.builder(StatsD.forAddress(address).getConfig()).build());
        //statsDReportPeriodSec = reportPeriodSec; -> config.period() instead
        return this;
    }

    public Reporters withGraphite() {
        withGraphite(createGraphiteAddress());
        return this;
    }

    public Reporters withGraphite(final InetSocketAddress address) {
        withGraphite(address, 60);
        return this;
    }

    public Reporters withGraphite(final InetSocketAddress address, final long reportPeriodSec) {
        reporters.add(() -> {
            var config = GraphiteConf.forAddress(address)
                    .withReportPeriodSec(reportPeriodSec)
                    .getConfig();

            //customize the default (ExponentiallyDecayingReservoir) reservoir to more precise
            // HdrHistograms for better performance?
            var registry = new MetricRegistry() {
                @Override
                public Timer timer(String name) {
                    return super.timer(name,
                            () -> new Timer(new SlidingTimeWindowArrayReservoir(reportPeriodSec, TimeUnit.SECONDS)));
                }

                @Override
                public Histogram histogram(String name) {
                    return super.histogram(name,
                            () -> new Histogram(new SlidingTimeWindowArrayReservoir(reportPeriodSec, TimeUnit.SECONDS)));
                }
            };
            var clock = Clock.SYSTEM;

            // https://micrometer.io/docs/registry/graphite
            // GraphiteConfig#tagsAsPrefix instead
            var graphiteReporter = GraphiteReporter.forRegistry(registry)
                    .withClock(new DropwizardClock(clock))
                    .prefixedWith(prefixStrategy.prefix())
                    .convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS)
                    //.disabledMetricAttributes(DISABLED_METRIC_ATTRIBUTES)
                    //pickled, udp, plain/text
                    .build(new Graphite(address));

            return new GraphiteMeterRegistry(config, clock, new GraphiteHierarchicalNameMapper(), registry, graphiteReporter);
        });
        return this;
    }

    public Reporters withLogging() {
        reporters.add(() -> new LoggingMeterRegistry(new LoggingRegistryConfig() {
            @Override
            public String get(String key) {
                return null;
            }

            @Override
            public String prefix() {
                return prefixStrategy.prefix();
            }
        }, Clock.SYSTEM));
        return this;
    }

    public Reporters withJvmMetrics(final boolean reportJvmMetrics) {
        this.reportJvmMetrics = reportJvmMetrics;
        return this;
    }

    public Reporters withJMXMetrics(final boolean reportToJmx) {
        this.reportToJmx = reportToJmx;
        return this;
    }

    public void startReporting() {
        if(reportJvmMetrics) {
            registerJvmMetrics();
        }
//        if (reportToJmx) {
//            reportJMX(metricRegistry);
//        }

        reporters.forEach(reporter -> Metrics.addRegistry(reporter.get()));
    }

//    private static void reportJMX(MetricRegistry metricRegistry) {
//        JmxReporter.forRegistry(metricRegistry).build().start();
//    }
//
    private static void registerJvmMetrics() {
//        new ClassLoaderMetrics().bindTo(registry);
        new JvmMemoryMetrics().bindTo(Metrics.globalRegistry);
        new JvmGcMetrics().bindTo(Metrics.globalRegistry);
        new JvmThreadMetrics().bindTo(Metrics.globalRegistry);
//        new ProcessorMetrics().bindTo(registry);

        /*
            <groupId>io.dropwizard.metrics</groupId>
            <artifactId>metrics-jvm</artifactId>
         */
//        metricRegistry.register("jvm.memory", new MemoryUsageGaugeSet());
//        metricRegistry.register("jvm.gc", new GarbageCollectorMetricSet());
//        metricRegistry.register("jvm.threads", new ThreadStatesGaugeSet());
//        metricRegistry.register("jvm.files", new FileDescriptorRatioGauge());
//        metricRegistry.register("jvm.memoryPools", new BufferPoolMetricSet(ManagementFactory.getPlatformMBeanServer()));
    }

    private static InetSocketAddress createGraphiteAddress() {
        final String host = EnvUtil.getenv("METRICS_TARGET_GRAPHITE_HOST", "carbonrelay-consul");
        final int port = Integer.parseInt(EnvUtil.getenv("METRICS_TARGET_GRAPHITE_PORT", "2001"));

        log.info("Using graphite target {}:{}", host, port);
        return InetSocketAddress.createUnresolved(host, 2001);
    }

    private static class StatsD {
        private final InetSocketAddress address;

        static StatsD forAddress(InetSocketAddress address) {
            return new StatsD(address);
        }

        private StatsD(InetSocketAddress address) {
            this.address = address;
        }

        StatsdConfig getConfig() {
            return new StatsdConfig() {
                @Override
                public String get(String k) {
                    return null;
                }

                @Override
                public String prefix() {
                    return "gelerion.local";
                }

                @Override
                public StatsdFlavor flavor() {
                    return StatsdFlavor.ETSY;
                }

                @Override
                public String host() {
                    return address.getHostName();
                }

                @Override
                public int port() {
                    return address.getPort();
                }
            };
        }
    }

    private static class GraphiteConf {
        private final InetSocketAddress address;
        private long reportPeriodSec = 60;

        static GraphiteConf forAddress(InetSocketAddress address) {
            return new GraphiteConf(address);
        }

        GraphiteConf(InetSocketAddress address) {
            this.address = address;
        }

        GraphiteConf withReportPeriodSec(long period) {
            reportPeriodSec = period;
            return this;
        }

        GraphiteConfig getConfig() {
            return new GraphiteConfig() {
                @Override
                public String get(String k) {
                    return null;
                }

                @Override
                public String prefix() {
                    return "gelerion.local";
                }

                @Override
                public String host() {
                    return address.getHostName();
                }

                @Override
                public int port() {
                    return address.getPort();
                }

                @Override
                public Duration step() {
                    return Duration.of(reportPeriodSec, ChronoUnit.SECONDS);
                }
            };
        }
    }
}


