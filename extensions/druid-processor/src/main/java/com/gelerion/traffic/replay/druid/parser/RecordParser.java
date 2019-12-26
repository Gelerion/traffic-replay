package com.gelerion.traffic.replay.druid.parser;

import com.gelerion.traffic.replay.druid.model.DruidEvent;

//generify / dynamic population by name
public interface RecordParser {
    DruidEvent parse(String record);
}
