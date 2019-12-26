package com.gelerion.traffic.replay.api.model;

import com.gelerion.traffic.replay.api.event.Event;
import okhttp3.Request;

import java.util.Objects;

public class EventAndRequest {
    private final Event event;
    private final Request request;

    private EventAndRequest(Event event, Request request) {
        this.event = event;
        this.request = request;
    }

    public static EventAndRequest of(Event event, Request request) {
        return new EventAndRequest(event, request);
    }

    public Event event() {
        return this.event;
    }

    public Request request() {
        return this.request;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventAndRequest that = (EventAndRequest) o;
        return Objects.equals(event, that.event) &&
                Objects.equals(request, that.request);
    }

    @Override
    public int hashCode() {
        return Objects.hash(event, request);
    }

    @Override
    public String toString() {
        return "EventAndRequest{" + "event=" + event + ", request=" + request + '}';
    }
}
