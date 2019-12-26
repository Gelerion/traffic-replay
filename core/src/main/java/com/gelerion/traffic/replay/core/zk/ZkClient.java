package com.gelerion.traffic.replay.core.zk;

import com.gelerion.traffic.replay.core.zk.model.KafkaBrokerData;
import com.google.gson.Gson;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.zalando.fauxpas.ThrowingFunction;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.zalando.fauxpas.FauxPas.throwingFunction;

public class ZkClient {
    private final CuratorFramework client;
    private final Gson gson;

    public ZkClient(String zookeeperUrl) {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        CuratorFramework client = CuratorFrameworkFactory.newClient(zookeeperUrl, retryPolicy);
        client.start();
        this.client = client;
        this.gson = new Gson();
    }

    public Collection<String> getBrokerList() {
        return Stream.of(getChildren().apply("/brokers/ids"))
                .flatMap(List::stream)
                .map(brokerId -> getData().apply(brokerId))
                .map(String::new)
                .map(brokerData -> gson.fromJson(brokerData, KafkaBrokerData.class))
                .map(KafkaBrokerData::hostAndPort)
                .collect(toList());
    }

    private ThrowingFunction<String, byte[], Exception> getData() {
        return throwingFunction(id -> client.getData().forPath("/brokers/ids/" + id));
    }

    private ThrowingFunction<String, List<String>, Exception> getChildren() {
        return throwingFunction(path -> client.getChildren().forPath(path));
    }
}
