package org.jd.stream.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HeapMemoryMonitor {

    public static void monitorHeapUsage(int rowCount, BufferedWriter writer, boolean enableGC) {
        Runtime runtime = Runtime.getRuntime();


        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        String timestamp = new SimpleDateFormat("yyy-MM-dd HH:mm:ss").format(new Date());

        String logEntry = String.format("%s,%d,%d,%d,%d%n",
                timestamp,
                rowCount,
                totalMemory / (1024 * 1024),
                usedMemory / (1024 * 1024),
                freeMemory / (1024 * 1024));

        if(enableGC){
            runtime.gc();
        }

        if (writer != null) {
            try {
                writer.write(logEntry);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Used memory: " + usedMemory / (1024 * 1024) + "/" + totalMemory / (1024 * 1024));
        }
    }
}
