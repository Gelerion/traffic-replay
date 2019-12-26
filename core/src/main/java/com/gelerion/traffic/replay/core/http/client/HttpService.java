package com.gelerion.traffic.replay.core.http.client;

import com.gelerion.traffic.replay.api.model.EventAndRequest;

public interface HttpService {
    Runnable createTrigger(EventAndRequest eventAndRequest);
}
