package com.learn.grammar;

public class RandomTest {
    private static int findPointZeroIndex(String str) {
        int pointIndex = -1;
        for (int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            if (pointIndex > 0 && c != '0') {
                return -1;
            } else if (pointIndex == -1 && c == '.') {
                pointIndex = i;
            }
        }
        return pointIndex;
    }

    public static void main(String[] args) {
        String str = "123.00";
        int pointIndex = findPointZeroIndex(str);
        System.out.println(pointIndex);
    }
}
