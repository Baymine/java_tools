package com.learn.grammar;

import java.util.HashMap;
import java.util.Map;

public class StreamFilter {

    public static void main(String[] args) {
        Map<String, String> properties = new HashMap<>();

        properties.put("fs.defaultFS", "hdfs://localhost:9000");
        properties.put("dfs.replication", "3");
        properties.put(null, "ThisKeyIsNull");       // This entry should be filtered out
        properties.put("someProperty", null);        // This entry should be filtered out
        properties.put("block.size", "134217728");   // 128MB

        properties.forEach((k, v) -> System.out.println(k + "=" + v));
        System.out.println();

        System.out.println("After filter...");
        properties.entrySet().stream().filter(entry -> entry.getValue() != null && entry.getKey() != null)
                .forEach(entry-> System.out.println(entry.getKey() + "=" + entry.getValue()));
    }
}
