package org.jd.stream.dto;

import java.util.Collections;
import java.util.Map;

public class Record {
    private final Map<String, Object> data;


    public Record(Map<String, Object> data) {
        this.data = Collections.unmodifiableMap(data);
    }

    public Object get(String column) {
        return data.get(column);
    }

    public Map<String, Object> getData() {
        return data;
    }

    @Override
    public String toString() {
        return "Record{" + "data=" + data + '}';
    }
}
