package org.jd.stream.mapper;

import org.jd.stream.dto.Record;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class RecordMapper {
    public static Record createRecord(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        Map<String, Object> columns = new HashMap<>(columnCount);

        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnLabel(i);
            Object value = resultSet.getObject(i);
            columns.put(columnName, value);
        }

        return new Record(columns);
    }
}
