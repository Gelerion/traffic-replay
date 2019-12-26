package com.gelerion.traffic.replay.core.channels;

public interface AsyncChannel<T> {

    void put(T record);

    boolean offer(T record);

    T get();

    T pool();

    void interrupt();

}
