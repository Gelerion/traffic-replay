package com.gelerion.traffic.replay.druid.config;

import java.util.List;

public interface DruidConfig {
    String url();
    String parser();
    List<String> rename();
    List<String> filter();
}
