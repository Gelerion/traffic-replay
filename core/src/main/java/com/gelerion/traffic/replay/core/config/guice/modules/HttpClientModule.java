package com.gelerion.traffic.replay.core.config.guice.modules;

import com.gelerion.traffic.replay.core.utils.HttpInterceptors;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import okhttp3.OkHttpClient;

public class HttpClientModule extends AbstractModule {

    @Provides
    @Singleton
    OkHttpClient httpClient() {
        return new OkHttpClient.Builder()
                .addInterceptor(HttpInterceptors.MEASURE_RESPONSE_TIME_INTERCEPTOR)
                .build();
    }
}
