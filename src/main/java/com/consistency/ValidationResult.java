package com.consistency;

/**
 * 验证结果类，表示SQL验证的结果
 */
public class ValidationResult {
    private final int expectedCount;
    private final int actualCount;
    
    public ValidationResult(int expectedCount, int actualCount) {
        this.expectedCount = expectedCount;
        this.actualCount = actualCount;
    }
    
    public boolean isMatched() {
        return expectedCount == actualCount;
    }
    
    public int getExpectedCount() {
        return expectedCount;
    }
    
    public int getActualCount() {
        return actualCount;
    }
}
