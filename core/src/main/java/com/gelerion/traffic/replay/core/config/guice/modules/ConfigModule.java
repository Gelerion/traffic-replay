package com.gelerion.traffic.replay.core.config.guice.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.cfg4j.provider.ConfigurationProvider;
import org.cfg4j.provider.ConfigurationProviderBuilder;
import org.cfg4j.source.ConfigurationSource;
import org.cfg4j.source.classpath.ClasspathConfigurationSource;

import java.nio.file.Paths;

import static java.util.Collections.singletonList;

public class ConfigModule extends AbstractModule {

    @Provides
    @Singleton
    ConfigurationProvider config() {
        ConfigurationSource source = new ClasspathConfigurationSource(() -> singletonList(
                Paths.get("application.yml")
        ));

        return new ConfigurationProviderBuilder()
                .withConfigurationSource(source)
                .build();
    }
}
