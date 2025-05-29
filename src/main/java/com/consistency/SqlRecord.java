package com.consistency;

/**
 * SQL记录类，表示CSV文件中的一行
 */
public class SqlRecord {
    private final String prestoDorisRel;
    private final int prestoCountResult;
    private final int dorisCountResult;
    private final String prestoSql;
    private final String dorisSql;
    
    public SqlRecord(String prestoDorisRel, int prestoCountResult, int dorisCountResult, String prestoSql, String dorisSql) {
        this.prestoDorisRel = prestoDorisRel;
        this.prestoCountResult = prestoCountResult;
        this.dorisCountResult = dorisCountResult;
        this.prestoSql = prestoSql;
        this.dorisSql = dorisSql;
    }
    
    public String getPrestoDorisRel() {
        return prestoDorisRel;
    }
    
    public int getPrestoCountResult() {
        return prestoCountResult;
    }
    
    public int getDorisCountResult() {
        return dorisCountResult;
    }
    
    public String getPrestoSql() {
        return prestoSql;
    }
    
    public String getDorisSql() {
        return dorisSql;
    }
}
