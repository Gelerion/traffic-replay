package com.gelerion.traffic.replay.core.zk.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class KafkaBrokerData {
    private final int version;
    @SerializedName("jmx_port")
    private final int jmxPort;
    private final long timestamp;
    private final List<String> endpoints;
    private final String host;
    private final int port;


    public KafkaBrokerData(int version, int jmxPort, long timestamp, List<String> endpoints, String host, int port) {
        this.version = version;
        this.jmxPort = jmxPort;
        this.timestamp = timestamp;
        this.endpoints = endpoints;
        this.host = host;
        this.port = port;
    }

    public String hostAndPort() {
        return host + ":" + port;
    }
}
