package com.gelerion.traffic.replay.core.utils;

import okhttp3.Interceptor;
import okhttp3.Response;

import java.util.concurrent.TimeUnit;

public class HttpInterceptors {
    public static final String X_LOADER_RESPONSE_TIME = "X-Loader-Response-Time-Ms";

    public static final Interceptor MEASURE_RESPONSE_TIME_INTERCEPTOR = chain -> {
        Response response = chain.proceed(chain.request());
        return response.newBuilder()
                .addHeader(X_LOADER_RESPONSE_TIME, String.valueOf(TimeUnit.NANOSECONDS.toMillis(System.nanoTime())))
                .build();
    };
}
