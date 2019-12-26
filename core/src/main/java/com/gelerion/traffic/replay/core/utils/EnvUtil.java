package com.gelerion.traffic.replay.core.utils;

public class EnvUtil {
    public static String getenv(final String name, final String defaultVal) {
        final String val = System.getenv(name);
        return val == null ? defaultVal : val;
    }
}
