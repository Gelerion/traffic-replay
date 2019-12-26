package com.gelerion.traffic.replay.druid.parser;


import java.util.Map;
import java.util.Optional;

public class RecordParserFactory {
    private static final Map<String, RecordParser> parsers = Map.of(QueryLogRecordParser.NAME, new QueryLogRecordParser());

    public static RecordParser getByName(String name) {
        return Optional.ofNullable(parsers.get(name))
                .orElseThrow(() -> new RuntimeException("Parser " + name + "not found!"));
    }

}
