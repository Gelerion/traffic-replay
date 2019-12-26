package com.gelerion.traffic.replay.core.metrics;

import com.jasongoodwin.monads.Try;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;

import static com.gelerion.traffic.replay.core.utils.EnvUtil.getenv;

@Slf4j
public class PrefixStrategies {

    public static final PrefixStrategy SIMPLE = () -> {
        final String service = getenv("SERVICE_NAME", "gelerion");
        final String mode = getenv("MODE", "local");
        return String.format("app.%s.%s", service, mode);
    };

    public static final PrefixStrategy V1 = () -> {
        // app.<cloud-provider>.<region>.<dc>.<az>.<env-type>.<namespace>.<service>.<mode>.<host-prefix>.<cluster-id>.<host>
        final String cloudProvider = getenv("CLOUD_PROVIDER", "dev"); //aws
        final String region = getenv("REGION", "dev"); //us-west
        final String dc = getenv("DC", "dev"); //eu1
        final String az = getenv("AZ", "dev"); //1a
        final String service = getenv("SERVICE_NAME", "dev"); //traffic-replay
        final String mode = getenv("PROFILE", "default"); //profile
        final String hostPrefix = getenv("HOST", hostname());
        return String.format("app.%s.%s.%s.%s.%s.%s.%s",
                cloudProvider, region, dc, az, service, mode, hostPrefix);
    };
    
    private static String hostname() {
        return Try.ofFailable(() -> InetAddress.getLocalHost().getHostName())
                .onFailure(e -> log.error("Failed to get local host name", e))
                .orElse("unknown");

    }
}
