package com.gelerion.traffic.replay.core.channels;

import com.gelerion.traffic.replay.core.kafka.types.KafkaRecords;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.cfg4j.provider.ConfigurationProvider;

import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.SECONDS;

@Singleton
public class BoundedBufferedChannel implements AsyncChannel<ConsumerRecord<String, String>> {
    private final BlockingQueue<ConsumerRecord<String, String>> queue;
    private AtomicBoolean interrupted = new AtomicBoolean(false);

    @Inject
    public BoundedBufferedChannel(ConfigurationProvider config) {
        this.queue = new ArrayBlockingQueue<>(config.getProperty("channel.capacity", Integer.class));
    }

    @SneakyThrows(InterruptedException.class)
    @Override
    public void put(ConsumerRecord<String, String> record) {
        boolean result;
        do {
            throwIfInterrupted();
            result = queue.offer(record, 10, SECONDS);
        } while (!result);
    }

    private void throwIfInterrupted() throws InterruptedException {
        if (interrupted.get()) {
            throw new InterruptedException("Consumer has unexpectedly stopped, check the logs");
        }
    }

    @SneakyThrows(InterruptedException.class)
    @Override
    public boolean offer(ConsumerRecord<String, String> record) {
        return queue.offer(record, 10, SECONDS);
    }

    @SneakyThrows(InterruptedException.class)
    @Override
    public ConsumerRecord<String, String> get() {
        return queue.take();
    }

    @SneakyThrows(InterruptedException.class)
    @Override
    public ConsumerRecord<String, String> pool() {
        return Optional.ofNullable(queue.poll(10, SECONDS))
                .orElse(KafkaRecords.EMPTY_RECORD);
    }

    @Override
    public void interrupt() {
        boolean result;
        do {
            if (interrupted.get()) return; //avoid infinity loop when called multiple times
            result = interrupted.compareAndSet(false, true);
        } while (!result);
    }
}
