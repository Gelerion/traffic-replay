package com.gelerion.traffic.replay.core.http.client;

import com.codahale.metrics.Histogram;
import com.gelerion.traffic.replay.core.metrics.MoreMetrics;
import com.gelerion.traffic.replay.core.utils.HttpInterceptors;
import com.gelerion.traffic.replay.api.event.Event;
import com.gelerion.traffic.replay.api.model.EventAndRequest;
import com.google.inject.Inject;
import com.jasongoodwin.monads.Try;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.cfg4j.provider.ConfigurationProvider;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Slf4j
public class OkHttpService implements HttpService {
    private final OkHttpClient httpClient;
    private boolean shouldLogResponseBody;

    @Inject
    public OkHttpService(OkHttpClient okHttpClient, ConfigurationProvider config) {
        this.httpClient = okHttpClient;
        this.shouldLogResponseBody = config.getProperty("http.service.log-response-body", Boolean.class);
    }

    @Override
    public Runnable createTrigger(EventAndRequest eventAndRequest) {
        return () -> {
            long queryStartTimeMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());

            httpClient.newCall(eventAndRequest.request())
                    .enqueue(new RequestCallback(eventAndRequest.event(), queryStartTimeMs, shouldLogResponseBody));
        };
    }

    private static class RequestCallback implements Callback {
        private static HttpMetrics httpMetrics = new HttpMetrics();
//        private static DistributionSummary responseTimeDiffHistogram = Metrics.summary("http", "response-time", "diff-ms");
        //quite a hack, micrometer doesn't allow to record negative values, though it's perfectly fine for dropwizard
        private static Histogram responseTimeDiffHistogram = MoreMetrics.histogram("http", "response-time", "diff-ms");
        private static Counter responseSuccessCounter = Metrics.counter("http", "request", "success");
        private static Counter responseFailureCounter = Metrics.counter("http", "request", "failure");
        private static Timer oldResponseTimeTimer = Metrics.timer("http", "response-time", "old-ms");
        private static Timer newResponseTimeTimer = Metrics.timer("http", "response-time", "new-ms");

        private final Event event;
        private final long queryStartTimeMs;
        private final boolean logResponseBody;

        private RequestCallback(Event event, long queryStartTimeMs, boolean logResponseBody) {
            this.event = event;
            this.queryStartTimeMs = queryStartTimeMs;
            this.logResponseBody = logResponseBody;
        }

        @Override
        public void onResponse(@NotNull Call call, @NotNull Response response) {
            responseSuccessCounter.increment();
            StringBuilder logRecord = new StringBuilder();
            logRecord.append("Status: ").append(response.code()).append("\n\t");

            long newResponseTimeMs = responseTimeMs(response) - queryStartTimeMs;
            newResponseTimeTimer.record(newResponseTimeMs, MILLISECONDS);
            httpMetrics.reportQueryTime(newResponseTimeMs, HttpMetrics.Type.NEW);
            logRecord.append("New response time: ").append(newResponseTimeMs).append("\n\t");

            event.responseTimeMs().ifPresent(oldResponseTimeMs -> {
                oldResponseTimeTimer.record(oldResponseTimeMs, MILLISECONDS);
                httpMetrics.reportQueryTime(oldResponseTimeMs, HttpMetrics.Type.OLD);
                logRecord.append("Old Response time: ").append(oldResponseTimeMs).append("\n\t");

                long responseTimeDiff = newResponseTimeMs - oldResponseTimeMs;
                responseTimeDiffHistogram.update(responseTimeDiff);
                logRecord.append("Response time difference: ").append(responseTimeDiff);
            });

            //potentially might be very big and lead to OOM
            if (logResponseBody) {
                logRecord.append("\n\t").append("Response body: ")
                        .append(Try.ofFailable(() -> requireNonNull(response.body()).string()).orElse("Failed to parse"));
            }

            log.debug(logRecord.toString());
        }

        private long responseTimeMs(@NotNull Response response) {
            return Long.parseLong(requireNonNull(response.header(HttpInterceptors.X_LOADER_RESPONSE_TIME)));
        }

        @Override
        public void onFailure(@NotNull Call call, @NotNull IOException e) {
            responseFailureCounter.increment();
            log.warn("Failed to execute query {}", event, e);
        }
    }
}
