package org.jd.stream.constant;

/**
 * @Auther: zy
 * @Date: 2024/04/15 17:45
 * @Description: 数据下载流程中用到的变量
 */
public class DataDownloadConstant {

    public static final String queueKey = "DATA_HUB_DOWNLOAD_JOB_QUEUE";


    public static final String DOWNLOAD_LABEL = "DOWNLOAD";

    public static final char DEFAULT_SPLIT_CHAR = 1;


    public static final char BUFFERED_SIZE = 10000;

    public static final Integer MAX_SIZE = 2000000;


    /**
     * 失败原因截断
     */
    public static final Integer FAIL_REASON_SIZE = 500;


    public static final String DOWNLOAD_FILE_DIR = "/data3/caokaihua1/";
}

