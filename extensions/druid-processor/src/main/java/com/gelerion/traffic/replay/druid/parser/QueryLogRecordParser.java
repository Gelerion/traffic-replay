package com.gelerion.traffic.replay.druid.parser;

import com.gelerion.traffic.replay.druid.model.DruidEvent;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Map;

@Slf4j
public class QueryLogRecordParser implements RecordParser {
    public final static String NAME = "druid-query-log";
    private final static Type MAP_TYPE = new TypeToken<Map<String, String>>(){}.getType();
    private final static Gson GSON = new Gson();

    public QueryLogRecordParser() {}


    public DruidEvent parse(String record) {
        try {
            return doParse(record);
        } catch (Exception e) {
            log.warn("Failed to parse record {}", record, e);
            return DruidEvent.NON_VALID;
        }
    }

    private DruidEvent doParse(String record) {
        String[] split = record.split("\t");

        String timestamp = split[0];
        String ip = split[1];
        String query = split[2];
        String queryMeta = split[3];

        Map<String, String> meta = GSON.fromJson(queryMeta, MAP_TYPE);
        long responseTime = Long.parseLong(meta.get("query/time"));
        Instant startTime = Instant.parse(timestamp);

        return DruidEvent.builder()
                .responseTime(responseTime)
                .startTime(startTime)
                .query(query)
                .build();
    }
}
