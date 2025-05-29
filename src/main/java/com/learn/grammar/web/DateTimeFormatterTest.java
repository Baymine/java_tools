package com.learn.grammar.web;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class DateTimeFormatterTest {
    public static void main(String[] args) {
        // Test with ISO_LOCAL_DATE (incorrect for LocalTime)
        try {
            System.out.println("Testing with ISO_LOCAL_DATE (should fail):");
            String formattedTime = LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            System.out.println("Formatted time: " + formattedTime);
        } catch (Exception e) {
            System.out.println("Error: " + e.getClass().getName());
            System.out.println("Message: " + e.getMessage());
        }
        
        System.out.println("\n-----------------------------------\n");
        
        // Test with ISO_LOCAL_TIME (correct for LocalTime)
        try {
            System.out.println("Testing with ISO_LOCAL_TIME (should succeed):");
            String formattedTime = LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME);
            System.out.println("Formatted time: " + formattedTime);
        } catch (Exception e) {
            System.out.println("Error: " + e.getClass().getName());
            System.out.println("Message: " + e.getMessage());
        }
    }
}
