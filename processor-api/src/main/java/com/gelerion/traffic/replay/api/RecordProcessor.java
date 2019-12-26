package com.gelerion.traffic.replay.api;

import com.gelerion.traffic.replay.api.event.Event;
import com.gelerion.traffic.replay.api.model.EventAndRequest;
import okhttp3.Request;

import java.util.stream.Stream;

public interface RecordProcessor<E extends Event> {

    default Stream<EventAndRequest> process(String record) {
        return Stream.of(record)
                .map(this::parse)
                .filter(this::filter)
                .map(this::edit)
                .map(event -> EventAndRequest.of(event, runFn(event)));
    }

    E parse(String record);

    default boolean filter(E event) {
        return true;
    }

    default E edit(E event) {
        return event;
    }

    Request runFn(E event);
}
