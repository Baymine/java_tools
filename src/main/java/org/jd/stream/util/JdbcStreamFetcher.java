package org.jd.stream.util;

import org.jd.stream.dto.Record;
import org.jd.stream.mapper.RecordMapper;

import java.sql.*;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class JdbcStreamFetcher {
    private static final int FETCH_SIZE = 5000;

    /**
     * Creates a Statement object configured for streaming result sets.
     *
     * @param connection The database connection.
     * @return A streaming-configured Statement.
     * @throws SQLException If a database access error occurs.
     */
    static Statement createStreamingStatement(Connection connection) throws SQLException {
        connection.setAutoCommit(false);

        Statement stmt = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

        stmt.setFetchSize(Integer.MIN_VALUE);

        return stmt;
    }

    public static Stream<Record> tableAsStream(Connection connection, String table, int limit) throws SQLException{
        UncheckedCloseable close = null;

        close = UncheckedCloseable.wrap(connection);
        connection.setAutoCommit(false);

        try {
            String sql = "SELECT * FROM hive.dev.app_cs_cse_laga_all_process_sum_d LIMIT 2000000";
            System.out.println("Executing SQL: " + sql);

            connection.setAutoCommit(false);

            Statement statement = createStreamingStatement(connection);
            close = close.nest(statement);

            ResultSet resultSet = statement.executeQuery(sql);
            close = close.nest(resultSet);
            Stream<Record> stream = StreamSupport.stream(new Spliterators.AbstractSpliterator<Record>(Long.MAX_VALUE, Spliterator.ORDERED) {
                @Override
                public boolean tryAdvance(Consumer<? super Record> action) {
                    try {
                        if (!resultSet.next()) {
                            return false;
                        }
                        Record record = RecordMapper.createRecord(resultSet);
                        action.accept(record);
                        return true;
                    } catch (SQLException sqlException) {
                        throw new RuntimeException(sqlException);
                    }
                }
            }, false).onClose(close);
            return stream;
        } catch (SQLException sqlException) {
            if (close != null) {
                try {
                    close.close();
                } catch (Exception exception) {
                    sqlException.addSuppressed(exception);
                }
            }
            throw sqlException;
        }
    }
}
