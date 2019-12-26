package com.gelerion.traffic.replay.druid;

import com.gelerion.traffic.replay.api.RecordProcessor;
import com.gelerion.traffic.replay.druid.config.DruidConfig;
import com.gelerion.traffic.replay.druid.model.DruidEvent;
import com.gelerion.traffic.replay.druid.parser.RecordParser;
import com.gelerion.traffic.replay.druid.parser.RecordParserFactory;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

public class DruidRecordProcessor implements RecordProcessor<DruidEvent> {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final DruidConfig config;
    private final RecordParser recordParser;

    DruidRecordProcessor(DruidConfig config) {
        this.config = config;
        this.recordParser = RecordParserFactory.getByName(config.parser());
    }

    @Override
    public DruidEvent parse(String record) {
        return recordParser.parse(record);
    }

    @Override
    public boolean filter(DruidEvent event) {
        return event != DruidEvent.NON_VALID;
    }

    @Override
    public Request runFn(DruidEvent event) {
        return new Request.Builder()
                .url(config.url())
                .header("User-Agent", "Http Load Test")
//              .header("Accept-Encoding", "gzip") <- added automatically and the gzip'd response will be ungzipped transparently
                .post(RequestBody.create(event.getQuery(), JSON))
                .build();
    }
}
