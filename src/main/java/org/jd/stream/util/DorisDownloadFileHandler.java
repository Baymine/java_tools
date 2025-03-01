package org.jd.stream.util;


import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.text.StrBuilder;
import org.apache.logging.log4j.util.Strings;
import org.jd.stream.constant.DataDownloadConstant;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.ColumnMapRowMapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @Package com.jd.datahub.service.engine.query.datasource.db.doris
 * @Auther: zy
 * @Email: zhangyue99@jd.com
 * @Date: 2024/11/15 15:29
 * @Description:
 */
@Data
@AllArgsConstructor
@Slf4j
public class DorisDownloadFileHandler {


    private RowMapper<Map<String, Object>> rowMapper;

    private PrintWriter printWriter;

    private File txtFile;

    private Integer count;

    private Long fileSize;

    private List<String> titleIndex;

    public DorisDownloadFileHandler(File txtFile) throws FileNotFoundException {
        this.txtFile = txtFile;
        this.count = 0;
        this.rowMapper = new ColumnMapRowMapper();
        this.printWriter = new PrintWriter(new OutputStreamWriter(new FileOutputStream(txtFile), StandardCharsets.UTF_8), false);
    }

    public void finish() {
        printWriter.flush();
    }

    public void close() {
        printWriter.close();
    }

    public void processRow(ResultSet rs) throws SQLException {
        log.debug("当前行:{}", count++);
        Map<String, Object> data = rowMapper.mapRow(rs, count);
        handleRow(data);
        log.debug("当前文件大小:{}", txtFile.length());
    }

    /**
     * 处理一行
     * @param data
     */
    private void handleRow(Map<String, Object> data) {

        // 开始时间
        long startTime = System.currentTimeMillis();
        try {
            // 处理第一行
            if (count == 1) {
                titleIndex = Lists.newArrayList();

                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    titleIndex.add(entry.getKey());
                }
                printWriter.println(Strings.join(titleIndex, DataDownloadConstant.DEFAULT_SPLIT_CHAR));
            }

            // 正文
            printWriter.println(getDataLine(data, titleIndex));

            // 每个BUFFERED_SIZE刷入磁盘一次
            if (count % DataDownloadConstant.BUFFERED_SIZE == 0) {
                log.debug("钩子 flush data: {}", System.currentTimeMillis() - startTime);
                printWriter.flush();
            }
        } catch (Exception e) {
            log.error("DorisDownloadFileHandler生成txt文件失败", e);

            throw new RuntimeException(e.getMessage());
        } finally {
            long endTime = System.currentTimeMillis();
            log.debug("DorisDownloadFileHandler file path: {}", txtFile.getAbsolutePath());
        }
    }


    /**
     * 获取值行  北京#%v@100
     *
     * @param data
     * @param sortedTitle
     * @return
     */
    private String getDataLine(Map<String, Object> data, List<String> sortedTitle) {

        Iterator<String> iterator = sortedTitle.iterator();
        Object first = String.valueOf(data.get(iterator.next()));
        if (!iterator.hasNext()) {
            return ObjectUtils.toString(first);
        } else {
            StrBuilder buf = new StrBuilder(256);
            if (first != null) {
                buf.append(first);
            }

            while (iterator.hasNext()) {
                buf.append(DataDownloadConstant.DEFAULT_SPLIT_CHAR)
                        .append(String.valueOf(data.get(iterator.next())));
            }

            return buf.toString();
        }
    }
}

