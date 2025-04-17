-- 中心子句有问题
-- WITH
--     current_day AS (
--         SELECT
--             sd_num
--         FROM
--             dmc_qm.dmcqm_qmemp_cash_daily_mntr_biz_i_d
--         WHERE
--             label IN ('现金贷')
--           AND dt = '2024-12-12'
--     ),
--     preivous_day AS (
--         SELECT
--             sd_num
--         FROM
--             dmc_qm.dmcqm_qmemp_cash_daily_mntr_biz_i_d
--         WHERE
--             label IN ('现金贷')
--           AND dt = date_sub_str ('2024-12-12', 1)
--     )
-- SELECT
--     ROUND(((current_day) / (preivous_day) - 1) * 100, 2) AS "环比"
-- FROM current_day, previous_day;

-- 获取元数据非常慢：生成查询计划需要2分钟+
-- set nereids_timeout_second=180; set enable_sql_cache=true;set enable_external_file_cache=true;
-- explain WITH
--     cmdb AS
--         (
--             SELECT DISTINCT
--                 ip,
--                 server_code,
--                 cluster_name,
--                 nn_one
--             FROM
--                 (
--                     SELECT
--                         ip,
--                         infos['server_code'] server_code,
--                         infos['phy_cluster_code'] AS cluster_name,
--                         group_name AS nn
--                     FROM
--                         gdm.gdm_m99_cmdb_node_info_da
--                             CROSS JOIN unnest(cluster_server_info) ast(infos)
--                     WHERE
--                         dt = '2025-01-04'
--                       AND infos['server_code'] IN('nn', 'onn', 'router')
--                       AND infos['phy_cluster_code'] IN
--                           (
--                               SELECT
--                                   cluster
--                               FROM
--                                   dim.dim_jdr_plat_platdat_hdfs_audit_cluster
--                               WHERE
--                                   online_flag = '1'
--                           )
--                 )
--                     a
--                     CROSS JOIN unnest(SPLIT(nn, ',')) ast(nn_one)
--             WHERE
--                 nn_one LIKE 'ns%'
--         )
--         ,
--     hdfs AS
--         (
--             SELECT DISTINCT
--                 CASE
--                     WHEN cluster IN('tyrande', 'guldan')
--                         THEN 'hope'
--                     WHEN cluster = 'evil'
--                         THEN '10k'
--                     ELSE cluster
--                     END AS cluster,
--                 nn,
--                 nnip,
--                 infotype
--             FROM
--                 app.app_jdr_plat_platdata_hdfs_audit_log_mointor_v2
--             WHERE
--                 CONCAT(dt, hour) IN
--                 (
--                     SELECT
--                         CONCAT(dt, hour) AS dh
--                     FROM
--                         app.app_jdr_plat_platdata_hdfs_audit_log_mointor_v2
--                     GROUP BY
--                         CONCAT(dt, hour)
--                     ORDER BY
--                         CONCAT(dt, hour) DESC limit 24
--     )
--     )
-- SELECT
--     COUNT(1)
-- FROM
--     (
--         SELECT
--             hdfs.*
--         FROM
--             hdfs
--                 LEFT JOIN cmdb
--                           ON
--                               hdfs.nnip = cmdb.ip
--                                   AND hdfs.cluster = cmdb.cluster_name
--                                   AND hdfs.nn = cmdb.nn_one
--                                   AND hdfs.infotype = cmdb.server_code
--         WHERE
--             cmdb.ip IS NULL
--     )
--         finl
-- WHERE
--     infotype = 'nn'
--   AND nn NOT IN('ns10', 'ns1010', 'ns17')




-- hive 表存在key命名的列会报错
-- select "key" from app.app_sfs_c03_fdc_timeseries where "key" = 'salesForecast' limit 1;


-- 查询时间很长
-- select * from dmr_jud.dmrjud_jud_01_risk_ordr_i_d where ( parent_sale_ord_id='307227121162' and sale_ord_dt >='2024-02-01' and sale_ord_dt <='2025-01-31' or parent_sale_ord_id='306136265544' and sale_ord_dt >='2024-02-01' and sale_ord_dt <='2025-01-31' or parent_sale_ord_id='297180568074' and sale_ord_dt >='2024-02-01' and sale_ord_dt <='2025-01-31' or parent_sale_ord_id='289282900207' and sale_ord_dt >='2024-02-01' and sale_ord_dt <='2025-01-31' or parent_sale_ord_id='298899416362' and sale_ord_dt >='2024-02-01' and sale_ord_dt <='2025-01-31' or parent_sale_ord_id='295201527006' and sale_ord_dt >='2024-02-01' and sale_ord_dt <='2025-01-31' or parent_sale_ord_id='293998075702' ) and sale_ord_dt >='2024-02-01' and sale_ord_dt <='2025-01-31' and (( dt >='2024-02-01' and dt <='2025-04-01' ) or dt = '4712-12-31' ) offset 0 limit 10

-- set enable_profile=true; set enable_sql_cache=false; set parallel_pipeline_task_num=1;SELECT
--     *
-- FROM
--     app.app_jdr_survey_dmp_a_d_d
-- WHERE
--     dt = '2025-02-18'
--   AND agg_type = '0'
--   AND agg_days = '30'
--   AND sex = '1'
--   AND marriage = '1'
--   AND ARRAY_CONTAINS(traffic_sku_set, '100034468527') = true
--   AND ARRAY_CONTAINS(cart_sku_set, '100034468527') = true
--   AND ARRAY_CONTAINS(ord_sku_set, '100034468527') = true limit 1000


-- set enable_profile=true; set enable_sql_cache=false; set query_timeout=18000;set parallel_pipeline_task_num=0; select * from dmr_jud.dmrjud_jud_01_risk_ordr_i_d where ( parent_sale_ord_id='307227121162' and sale_ord_dt >='2024-06-01' and sale_ord_dt <='2025-01-31' or parent_sale_ord_id='306136265544' and sale_ord_dt >='2024-06-01' and sale_ord_dt <='2025-01-31' or parent_sale_ord_id='297180568074' and sale_ord_dt >='2024-06-01' and sale_ord_dt <='2025-01-31' or parent_sale_ord_id='289282900207' and sale_ord_dt >='2024-06-01' and sale_ord_dt <='2025-01-31' or parent_sale_ord_id='298899416362' and sale_ord_dt >='2024-06-01' and sale_ord_dt <='2025-01-31' or parent_sale_ord_id='295201527006' and sale_ord_dt >='2024-06-01' and sale_ord_dt <='2025-01-31' or parent_sale_ord_id='293998075702' ) and sale_ord_dt >='2024-06-01' and sale_ord_dt <='2025-01-31' and (( dt >='2024-06-01' and dt <='2025-01-01' ) or dt = '4712-12-31' ) offset 0 limit 10

-- SELECT exp_info, shunt_info FROM app.app_touchstone_logbook_jrc_exp_log WHERE DAY = '2025-02-20' AND hour = '16' LIMIT 100;

-- use dmc_bc; set erp='jingjianqiang1'; set hadoop_user_name='jingjianqiang1';select creator from dmc_jdt_dmcbc_event_order_main_jdcloud_i_det_d as d where creator in ('bjlijieyw');



-- select * from  app.app_ea_dim_vender_info_map_d where dt >= sysdate(-31) limit 1281;

-- SELECT year_and_week(CAST(t."提测通过时间" AS varchar), '', 6, 4) AS "提测通过时间", COALESCE(TRY(CAST(avg(COALESCE(TRY(CAST(trim(CAST(CAST(trim(CAST(t."测试等待时间" AS varchar)) AS decimal (30, 10)) / CAST(trim(CAST(24 AS varchar)) AS decimal (30, 10)) AS varchar)) AS decimal (30, 10))), NULL)) AS decimal (30, 2))), NULL) AS "t1_测试等待时间（天）", COALESCE(TRY(CAST(avg(COALESCE(TRY(CAST(trim(CAST(CAST(trim(CAST(t."测试执行时间" AS varchar)) AS decimal (30, 10)) / CAST(trim(CAST(24 AS varchar)) AS decimal (30, 10)) AS varchar)) AS decimal (30, 10))), NULL)) AS decimal (30, 2))), NULL) AS "t1_测试执行时间（天）", COALESCE(TRY(CAST(avg(COALESCE(TRY(CAST(trim(CAST(CAST(trim(CAST(t."测试阻塞时间" AS varchar)) AS decimal (30, 10)) / CAST(trim(CAST(24 AS varchar)) AS decimal (30, 10)) AS varchar)) AS decimal (30, 10))), NULL)) AS decimal (30, 2))), NULL) AS "t1_测试阻塞时间（天）", COALESCE(TRY(count(DISTINCT (t."提测单id"))), NULL) AS "t1_提测单数量" FROM (SELECT dt AS "快照日期", proposer_erp AS "提测人erp", proposer_org_name AS "提测人部门", proposer_c1 AS "提测人部门C1", proposer_c2 AS "提测人部门C2", proposer_c3 AS "提测人部门C3", proposer_c4 AS "提测人部门C4", assignee_erp AS "测试受理人erp", assignee_org_name AS "测试受理人部门", assignee_c1 AS "测试受理部门C1", assignee_c2 AS "测试受理部门C2", assignee_c3 AS "测试受理部门C3", assignee_c4 AS "测试受理部门C4", submit_test_code AS "提测编码", submit_test_id AS "提测单id", submit_test_title AS "提测标题", card_name AS "卡片名称", state_name AS "状态名称", update_time AS "更新时间", create_time AS "提测时间", start_at AS "提测受理时间", IF(COALESCE(passed_at, '') = '', '1970-01-01', passed_at) AS "提测通过时间", space_name AS "团队空间", sprint_name AS "迭代名称", waiting_time AS "测试等待时间", blocked_time AS "测试阻塞时间", execution_time AS "测试执行时间", diff_day AS "测试等待时间（天）", CASE WHEN diff_day <= 7 THEN '<=7天' WHEN diff_day <= 14 AND diff_day > 7 THEN '7-14天' WHEN diff_day <= 30 AND diff_day > 14 THEN '14-30天' WHEN diff_day > 30 AND diff_day < 99999 THEN '>30天' ELSE 'unknown' END AS "测试等待时间范围" FROM (SELECT *, to_milliseconds((CAST(concat(dt, ' 23:59:59') AS TIMESTAMP) - CAST(create_time AS TIMESTAMP))) / 1000 / 60 / 60 / 24 AS diff_day FROM (SELECT dt, proposer_erp, proposer_org_name, SPLIT(proposer_org_name, '-')[3] AS proposer_c1, SPLIT(proposer_org_name, '-')[4] AS proposer_c2, SPLIT(proposer_org_name, '-')[5] AS proposer_c3, IF(LENGTH(proposer_org_name) - LENGTH(REPLACE(proposer_org_name, '-', '')) >= 5, SPLIT(proposer_org_name, '-')[6], NULL) AS proposer_c4, assignee_erp, assignee_org_name, IF(LENGTH(assignee_org_name) - LENGTH(REPLACE(assignee_org_name, '-', '')) >= 2, SPLIT(assignee_org_name, '-')[3], NULL) AS assignee_c1, IF(LENGTH(assignee_org_name) - LENGTH(REPLACE(assignee_org_name, '-', '')) >= 3, SPLIT(assignee_org_name, '-')[4], NULL) AS assignee_c2, IF(LENGTH(assignee_org_name) - LENGTH(REPLACE(assignee_org_name, '-', '')) >= 4, SPLIT(assignee_org_name, '-')[5], NULL) AS assignee_c3, IF(LENGTH(assignee_org_name) - LENGTH(REPLACE(assignee_org_name, '-', '')) >= 5, SPLIT(assignee_org_name, '-')[6], NULL) AS assignee_c4, submit_test_code, submit_test_id, submit_test_title, card_name, state_name, update_time, SUBSTR(create_time, 1, 19) create_time, start_at, SUBSTR(passed_at, 1, 19) AS passed_at, space_name, sprint_name, waiting_time, blocked_time, execution_time FROM app.app_defect_submit_test_mang_dtl_s_d_00008987 WHERE proposer_org_code LIKE '%/00000000/00008987/00046966/00103808%' AND dt >= sysdate(-100) AND v_deleted = '0') t) t) t WHERE (1 = 1 AND 1 = 1 AND (1 = 1 AND CAST(t."测试受理部门C3" AS varchar) IN ('成都研发部') AND (1 = 1 AND format_datetime(CAST(date_supplement(CAST(t."提测通过时间" AS varchar)) AS timestamp), 'yyyy-MM') >= '2024-11' AND format_datetime(CAST(date_supplement(CAST(t."提测通过时间" AS varchar)) AS timestamp), 'yyyy-MM') <= '2025-01')) AND (1 = 1 AND CAST(t."状态名称" AS varchar) IN ('通过') AND CAST(t."测试受理部门C2" AS varchar) IN ('金融科技研发部') AND (1 = 1 AND format_datetime(CAST(date_supplement(CAST(t."提测时间" AS varchar)) AS timestamp), 'yyyy-MM-dd') >= '2024-01-01' AND format_datetime(CAST(date_supplement(CAST(t."提测时间" AS varchar)) AS timestamp), 'yyyy-MM-dd') <= '2025-01-17') AND format_datetime(CAST(date_supplement(CAST(t."快照日期" AS varchar)) AS timestamp), 'yyyy-MM-dd') = '2025-01-17')) GROUP BY year_and_week(CAST(t."提测通过时间" AS varchar), '', 6, 4) ORDER BY "提测通过时间" limit 10000
--
--
-- select year_and_week(t.complete_time) from dmc_oa.dmc_jdt_dmcoa_material_damage_workflow_s_det_d t where
--     CAST(
--             YEAR_AND_WEEK (CAST(t.`complete_time` AS VARCHAR), '', 2, 4) AS VARCHAR
--     ) = '2024年第46周' limit 20;

-- +------+-----------+
-- | type | user_cnt |
-- +------+-----------+
-- | 未覆盖 | 136266389 |
-- | 覆盖 | 581716112 |
-- +------+-----------+
-- SELECT
--     CASE
--         WHEN profile_24 IS NOT NULL
--         THEN '覆盖'
--         ELSE '未覆盖'
--         END AS type,
--     COUNT(DISTINCT user_id) AS user_cnt
-- FROM
--     app.app_jdr_global_user_profile_pin_data_a_d_d
-- WHERE
--     dt = '2025-02-01'
-- group by 1
--     limit 1000;

-- 581716112 ()
-- SELECT COUNT(DISTINCT user_id) FROM app.app_jdr_global_user_profile_pin_data_a_d_d WHERE dt = '2025-02-01' and profile_24 is not null;

-- 4219492024
-- SELECT COUNT(DISTINCT user_id) FROM app.app_jdr_global_user_profile_pin_data_a_d_d WHERE dt = '2025-02-01' and profile_24 is null;

-- 4801208136
-- SELECT COUNT(DISTINCT user_id) FROM app.app_jdr_global_user_profile_pin_data_a_d_d WHERE dt = '2025-02-01';

-- select COALESCE(TRY(substr( t."etl_time"  ,  6 )), null) as "t1_观测时间",t."dept_name_3" as "dept_name_3",COALESCE(TRY( '李超' ), null) as "t1_负责人",cast(trim(cast(t."repay_amount" as varchar)) as decimal(30,2)) as "repay_amount",COALESCE(TRY(cast(trim(cast(cast(trim(cast(  t."repay_amount"  as varchar)) as decimal(30,10)) / cast(trim(cast(  1959678  as varchar)) as decimal(30,10)) as varchar)) as decimal(30,10))), null) as "t1_当日目标达成率" from (SELECT   etl_time as etl_time,   'C2' as dept_name_3,   sum(repay_amount) as repay_amount from   dmc_ll.dmc_jdt_dmcll_real_time_jtloan_btpin_coller_i_sum_d where   repay_date = date_add('{TX_DATE}', 1)   and org_name_1 in('法催_宿迁中心', '宿迁中心')   and case_type in('BH6', 'B5_RLWB', 'BH4', 'BH5') group by   etl_time,   'C2'  ) t  limit 10000

-- SELECT   COUNT(1) AS valueColumn FROM app.app_hudi_aigc_customer_log AS app_hudi_aigc_customer_log WHERE   dt = CURRENT_DATE()   AND event_id = 'MAigc_Main_ProductCardExpo'   AND JSON_EXTRACT_STRING(json_param, '$.pv_round') = 0 ;

-- SELECT * FROM adm.adm_jdr_sch_d10_csas_user_behavior_track1_di where dt ='2025-03-20' LIMIT 10


-- select dou as origin, cast(dou as varchar) as cast_var from hive.tmp.hive_decimal_double_test_20250327;


-- finebi
-- set orc_max_merge_distance = 1;set orc_once_max_read_size = 1;set enable_sql_cache=false;set enable_profile=true;SELECT
--                                     dt,
--                                     vender_brand_sec,
--                                     ka_brand2,
--                                     CASE
--                                         WHEN vender_brand_sec IN('汤臣倍健', '合生元', '荣耀', '徐福记', '伽蓝', '维他奶', '京东酒世界')
--                                             THEN '消费品南区业务部'
--                                         WHEN vender_brand_sec IN('达能集团', '雀巢', '北大荒', '物产中大', '小米', '联合利华', '无印良品', '华为', '伊利', '君乐宝', '雪花啤酒')
--                                             THEN '消费品北区业务部'
--                                         WHEN vender_brand_sec IN('绫致时装', '安踏')
--                                             THEN '时尚业务部'
--                                         WHEN vender_brand_sec IN('三一重工', '蔚来汽车', '比亚迪', '奇瑞', '小鹏汽车', '吉利')
--                                             THEN '汽车业务部'
--                                         WHEN vender_brand_sec IN('美的', '海信', '慕思床垫', '戴森', '格力', '海尔', 'TCL', '创维', '老板', '志高')
--                                             THEN '家电家居业务部'
--                                         WHEN vender_brand_sec IN('OPPO（含REALME和一加）', '晶澳', '隆基', '大华', '海康威视', '大疆')
--                                             THEN '3C业务部'
--                                         ELSE kaohe_belong_province_name
--                                         END AS kaohe_belong_name,
--                                     productl1name_new AS "产品",
--                                     CASE
--                                         WHEN productl3name_new IN('整车直达', '到仓-整车专送', '冷链整车', '医药整车')
--                                             THEN '是'
--                                         ELSE '否'
--                                         END AS "是否整车",
--                                     CASE
--                                         WHEN productl1name_new IN('快递', '快运', 'BPO')
--                                             THEN '快快收入'
--                                         WHEN productl1name_new IN('供应链', 'B2B供应链', '到仓服务', '大件', '冷链', '医药', '服务+', '京喜达', '云箱', '大宗业务')
--                                             THEN '供应链收入'
--                                         ELSE productl1name_new
--                                         END AS "类型",
--                                     SUM(total_amount_no_tax) income
--                                 FROM
--                                     app.app_ea_egr_vendor_prodcode_sum_m
--                                 WHERE
--                                     kaohe_second_dept_name = '行业大客户部'
--                                   AND COALESCE(total_amount_no_tax, 0) != 0
-- 	AND dt in ( '2024-01-01', '2024-02-01', '2025-01-01', '2025-02-01')
-- 	AND COALESCE(ord_type_name, '') <> 'FCS'
-- 	AND COALESCE(productl3name_new, '空') NOT IN('整车直达', '到仓-整车专送', '冷链整车', '医药整车')
--                                 GROUP BY
--                                     dt,
--                                     vender_brand_sec,
--                                     ka_brand2,
--                                     CASE
--                                     WHEN vender_brand_sec IN('汤臣倍健', '合生元', '荣耀', '徐福记', '伽蓝', '维他奶', '京东酒世界')
--                                     THEN '消费品南区业务部'
--                                     WHEN vender_brand_sec IN('达能集团', '雀巢', '北大荒', '物产中大', '小米', '联合利华', '无印良品', '华为', '伊利', '君乐宝', '雪花啤酒')
--                                     THEN '消费品北区业务部'
--                                     WHEN vender_brand_sec IN('绫致时装', '安踏')
--                                     THEN '时尚业务部'
--                                     WHEN vender_brand_sec IN('三一重工', '蔚来汽车', '比亚迪', '奇瑞', '小鹏汽车', '吉利')
--                                     THEN '汽车业务部'
--                                     WHEN vender_brand_sec IN('美的', '海信', '慕思床垫', '戴森', '格力', '海尔', 'TCL', '创维', '老板', '志高')
--                                     THEN '家电家居业务部'
--                                     WHEN vender_brand_sec IN('OPPO（含REALME和一加）', '晶澳', '隆基', '大华', '海康威视', '大疆')
--                                     THEN '3C业务部'
--                                     ELSE kaohe_belong_province_name
-- END,
-- 	productl1name_new,
-- 	CASE
-- 		WHEN productl3name_new IN('整车直达', '到仓-整车专送', '冷链整车', '医药整车')
-- 		THEN '是'
-- 		ELSE '否'
-- END,
-- 	CASE
-- 		WHEN productl1name_new IN('快递', '快运', 'BPO')
-- 		THEN '快快收入'
-- 		WHEN productl1name_new IN('供应链', 'B2B供应链', '到仓服务', '大件', '冷链', '医药', '服务+', '京喜达', '云箱', '大宗业务')
-- 		THEN '供应链收入'
-- 		ELSE productl1name_new
-- END
--
-- UNION ALL
--
-- SELECT * FROM dev.dev_HYDKH1_cwf  WHERE   dt in ( '2024-01-01', '2024-02-01', '2025-01-01', '2025-02-01')


-- set sql_dialect=doris;
-- WITH
--     base_detail AS (
--         SELECT
--             `dt`,
--             `uv`
--         FROM
--             (
--                 SELECT
--                     '3531' bu_id,
--                     CASE
--                         WHEN lvl IN ('total', 'hc_total', 'hc_word') THEN '事业部整体'
--                         ELSE dept_id_1
--                         END 一级部门编号,
--                     CASE
--                         WHEN lvl IN ('total', 'hc_total', 'hc_word') THEN '事业部整体'
--                         ELSE dept_name_1
--                         END 一级部门名称,
--                     CASE
--                         WHEN lvl IN ('total', 'hc_total', 'hc_word') THEN '事业部整体'
--                         WHEN lvl IN ('hc_dept1') THEN '一级部门整体'
--                         ELSE dept_id_2
--                         END 二级部门编号,
--                     CASE
--                         WHEN lvl IN ('total', 'hc_total', 'hc_word') THEN '事业部整体'
--                         WHEN lvl IN ('hc_dept1') THEN '一级部门整体'
--                         ELSE dept_name_2
--                         END 二级部门名称,
--                     STATUS 词状态,
--                     t1.key_word 关键词,
--                     t2.loss_reason 流失原因,
--                     mtd_days 月至今天数_计算日均,
--                     expo_num 曝光件次,
--                     uv,
--                     search_num 搜索次数,
--                     gmv_cj,
--                     order_line_cj,
--                     `dt`,
--                     deal_user_num 成交用户数,
--                     CASE
--                         WHEN lvl = 'total' THEN '事业部x搜索词整体'
--                         WHEN lvl = 'hc_total' THEN '事业部x高相关搜索词整体'
--                         WHEN lvl = 'hc_dept1' THEN '一级部门x高相关搜索词整体'
--                         WHEN lvl = 'hc_dept2' THEN '二级部门x高相关搜索词整体'
--                         WHEN lvl = 'hc_word' THEN '事业部x搜索词'
--                         ELSE '未知'
--                         END AS 汇总层级,
--                     CASE
--                         WHEN lvl = 'hc_total' THEN 1
--                         WHEN lvl = 'hc_dept1' THEN 2
--                         WHEN lvl = 'hc_dept2' THEN 3
--                         ELSE 4
--                         END AS lvl_rk,
--                     CASE
--                         WHEN dept_name_1 = '线上车品业务部' THEN 1
--                         WHEN dept_name_1 = '二轮出行业务部' THEN 2
--                         WHEN dept_name_1 = '养车线上业务部' THEN 3
--                         WHEN dept_name_1 = '维修改装业务部' THEN 4
--                         WHEN dept_name_1 = '贴膜全渠道业务部' THEN 5
--                         WHEN dept_name_1 = '养车连锁业务部' THEN 6
--                         WHEN dept_name_1 = '政企创新业务部' THEN 7
--                         WHEN dept_name_1 = '服务创新部' THEN 8
--                         ELSE 9
--                         END AS dept1_rk
--                 FROM
--                     (
--                         SELECT
--                             lvl,
--                             dept_id_1,
--                             dept_name_1,
--                             dept_id_2,
--                             dept_name_2,
--                             STATUS,
--                             key_word,
--                             mtd_days,
--                             expo_num,
--                             uv,
--                             search_num,
--                             gmv_cj,
--                             order_line_cj,
--                             deal_user_num,
--                             `dt`
--                         FROM
--                             app.app_jdr_jdc_search_mind_hc_word_index_i_s_d
--                         WHERE
--                             1 = 1
--                           AND (
--                             `dt` >= '2025-02-03'
--                                 AND `dt` <= '2025-04-07'
--                             )
--                           AND lvl IN (
--                                       'total',
--                                       'hc_total',
--                                       'hc_dept1',
--                                       'hc_dept2',
--                                       'hc_word'
--                             )
--                           AND CASE
--                                   WHEN lvl IN ('total', 'hc_total', 'hc_word') THEN 1 = 1
--                                   ELSE dept_name_1 IS NOT NULL
--                             END
--                           AND CASE
--                                   WHEN lvl IN ('total', 'hc_total', 'hc_word') THEN 1 = 1
--                                   ELSE dept_name_1 IS NOT NULL
--                             END
--                           AND 1 = 1
--                           AND 1 = 1
--                           AND 1 = 1
--                           AND 1 = 1
--                           AND 1 = 1
--                     ) t1
--                         LEFT JOIN (
--                         SELECT
--                             key_word,
--                             CASE
--                                 WHEN loss_reason = 0 THEN '非流失词'
--                                 WHEN loss_reason = 1 THEN '本月无该词'
--                                 WHEN loss_reason = 2 THEN '曝光件次占比小于等于50%'
--                                 WHEN loss_reason = 3 THEN '曝光件次占比大于50%,但是日均曝光件次小于等于1000'
--                                 ELSE '未知'
--                                 END AS loss_reason
--                         FROM
--                             app.app_jdr_jdc_search_mind_hc_word_status_i_d_d
--                         WHERE
--                             `dt` = SYSDATE(-2)
--                     ) t2 ON t1.lvl = 'hc_word'
--                         AND t1.status = '流失'
--                         AND t1.key_word = t2.key_word
--             ) dataset_tb
--         WHERE
--             (
--                 (`汇总层级` IN ('事业部x高相关搜索词整体'))
--                     AND (
--                     (
--                         (一级部门编号 IN ('3533', ''))
--                             OR (二级部门编号 IN (''))
--                             OR (bu_id IN ('', '3531'))
--                         )
--                     )
--                 )
--     ),
--     base_atom AS (
--         SELECT
--             COUNT(`uv`) AS `COUNT_6349e8d6283bf6155a2e70a35495a168`,
--             SUM(`uv`)   AS `SUM_6349e8d6283bf6155a2e70a35495a168`,
--             CONCAT(
--                     CASE
--                         WHEN WEEKDAY(`dt`) + 1 >= 1 THEN YEARWEEK(`dt`, 1)
--                         ELSE YEARWEEK(DATE_SUB(`dt`, INTERVAL 7 DAY), 1)
--                         END,
--                     '(',
--                     DATE_FORMAT(
--                             DATE_SUB(
--                                     `dt`,
--                                     INTERVAL((WEEKDAY(`dt`) + 1 - 1 + 7) % 7) DAY
--           ),
--                             '%Y-%m-%d'
--                     ),
--                     '~',
--                     DATE_FORMAT(
--                             DATE_ADD(
--                                     `dt`,
--                                     INTERVAL(6 - ((WEEKDAY(`dt`) + 1 - 1 + 7) % 7)) DAY
--           ),
--                             '%Y-%m-%d'
--                     ),
--                     ')'
--             ) AS `dt_week_saZmt8nE`
--         FROM
--             base_detail
--         GROUP BY
--             CONCAT(
--                     CASE
--                         WHEN WEEKDAY(`dt`) + 1 >= 1 THEN YEARWEEK(`dt`, 1)
--                         ELSE YEARWEEK(DATE_SUB(`dt`, INTERVAL 7 DAY), 1)
--                         END,
--                     '(',
--                     DATE_FORMAT(
--                             DATE_SUB(
--                                     `dt`,
--                                     INTERVAL((WEEKDAY(`dt`) + 1 - 1 + 7) % 7) DAY
--           ),
--                             '%Y-%m-%d'
--                     ),
--                     '~',
--                     DATE_FORMAT(
--                             DATE_ADD(
--                                     `dt`,
--                                     INTERVAL(6 - ((WEEKDAY(`dt`) + 1 - 1 + 7) % 7)) DAY
--           ),
--                             '%Y-%m-%d'
--                     ),
--                     ')'
--             )
--     )
-- with result as (
--     SELECT
--         *
--     FROM
--         base_atom
-- )
-- select * from result order by dt_week_saZmt8nE

-- set sql_dialect = doris;
-- WITH base_detail AS (
--     SELECT '2025-02-20' AS dt, 997784 AS uv UNION ALL
--     SELECT '2025-03-20', 1014156 UNION ALL
--     SELECT '2025-03-14', 916048 UNION ALL
--     SELECT '2025-03-17', 976310 UNION ALL
--     SELECT '2025-02-14', 959375 UNION ALL
--     SELECT '2025-02-12', 913905 UNION ALL
--     SELECT '2025-03-21', 961811 UNION ALL
--     SELECT '2025-03-09', 1031846 UNION ALL
--     SELECT '2025-03-11', 981961 UNION ALL
--     SELECT '2025-02-21', 966182 UNION ALL
--     SELECT '2025-02-27', 990545 UNION ALL
--     SELECT '2025-02-03', 732386 UNION ALL
--     SELECT '2025-02-07', 940725 UNION ALL
--     SELECT '2025-02-23', 1027561
-- )
-- SELECT
--     CONCAT(
--             CASE
--                 WHEN WEEKDAY(`dt`) + 1 >= 1 THEN YEARWEEK(`dt`, 1)
--                 ELSE YEARWEEK(DATE_SUB(`dt`, INTERVAL 7 DAY), 1)
--                 END,
--             '(',
--             DATE_FORMAT(
--                     DATE_SUB(
--                             `dt`,
--                             INTERVAL((WEEKDAY(`dt`) + 1 - 1 + 7) % 7) DAY
--     ),
--                     '%Y-%m-%d'
--             ),
--             '~',
--             DATE_FORMAT(
--                     DATE_ADD(
--                             `dt`,
--                             INTERVAL(6 - ((WEEKDAY(`dt`) + 1 - 1 + 7) % 7)) DAY
--   ),
--                     '%Y-%m-%d'
--             ),
--             ')'
--     ) AS `dt_week_saZmt8nE`
-- FROM
--     base_detail
-- GROUP BY
--     dt_week_saZmt8nE

-- SELECT SUM("入驻门店数") "入驻门店数", SUM("营业门店数") "营业门店数", SUM("门店动销数_fnc") "门店动销数_fnc", SUM("订单量_fnc") "订单量_fnc", SUM("日均订单量") "日均订单量", SUM("实付gov") "实付gov", SUM("单均毛利") "单均毛利", SUM("单均商品收入1") "单均商品收入1", SUM("单均平台商品补贴") "单均平台商品补贴", SUM("单均运费收入1") "单均运费收入1", SUM("单均平台运费
-- 补贴1") "单均平台运费补贴1", SUM("单均运费支出") "单均运费支出", SUM("单均配送运费支出") "单均配送运费支出", SUM("单均平台运费补贴") "单均平台运费补贴", SUM("毛利") "毛利", SUM("商品收入1") "商品收入1", SUM("平台商品补贴") "平台商品补贴", SUM("平台商品补贴cmc") "平台商品补贴cmc", SUM("运费收入1") "运费收入1", SUM("平台运费补贴1") "平台运费补贴1", SUM("运费支出") "运费支出", SUM("配送运费支出") "配送运费支出", SUM("平台运费补贴") "平台运费补贴" FROM (/* 这里是代码的注释内容 使用提示： 1、可以在左侧的列表中搜索表格名称，再将表格名称拖入画布中 2、按
-- 下 ctrl 键进行自动补全 3、SQL编辑完成后，请先点击下方【校验】，再点击【运行】，之后点击上方的【下一步】 */ select * from dj_waimairibao_2025 /*where dt = '2025-04-09' /* 这是一个宏
-- 变量，可以动态获取“昨天”的日期 */) WHERE (( ( "时间维度" = 'by周' ) ) AND ( ( "日期" = '2025W14' ) ) AND ( ( "city_name" like '%汇总%' ) ) AND ( ( "订单分层_fnc" = '汇总' ) )) limit 1 offset 0;


-- set erp=zhangdongdong92;set businessline=jcw_dmf;set hadoop_user_name=zhangdongdong92;show databases;
-- SELECT
--     a.field AS fieldKey,
--     COUNT(DISTINCT a.user_pin) AS fieldValue
-- FROM
--     (
--         SELECT
--             field,
--             user_pin
--         FROM
--             app.app_xz_user_tags_follow_view_v2
--         WHERE
--             dt = sysdate( - 1)
--           AND type = 3
--           AND follow_90d > 0
--     )
--         a
--         LEFT JOIN
--     (
--         SELECT
--             user_pin
--         FROM
--             app.app_xz_smp_dts_input_info
--         WHERE
--             task_id = '6a8072f2-a7af-4662-ad51-94585a215c12'
--     )
--         b
--     ON
--         a.user_pin = b.user_pin
-- WHERE
--     b.user_pin IS NOT NULL
-- GROUP BY
--     a.field
-- ORDER BY
--     fieldValue DESC LIMIT 10

set enable_sql_cache=false;
WITH FilteredInstances AS (
    SELECT
        etl_dt,
        rt_meta,
        id,
        instance_id,
        pin,
        out_order_id,
        instance_status,
        channel_code,
        num,
        amount,
        issue_source,
        unique_key,
        ext,
        created_date,
        modified_date,
        channel_type,
        bean_pool_id,
        activity_id,
        creator,
        modifier,
        business_lines,
        dt,
        trade_status,
        props_status,
        ROW_NUMBER() OVER (
            PARTITION BY instance_id
            ORDER BY FIELD(instance_status, 'RSF', 'RSS', 'RSI', 'RF', 'RS', 'RI', 'KF', 'KS', 'KI', 'HF', 'HS', 'HI', 'SF', 'SS', 'SI')
        ) AS rn
    FROM
        odm.odm_jdt_cmp_yxpt_market_kjjdou_mkt_jdou_instance_new_i
)
SELECT
    etl_dt,
    rt_meta,
    id,
    instance_id,
    pin,
    out_order_id,
    instance_status,
    channel_code,
    num,
    amount,
    issue_source,
    unique_key,
    ext,
    created_date,
    amount,
    issue_source,
    unique_key,
    ext,
    modified_date,
    channel_type,
    bean_pool_id,
    activity_id,
    creator,
    modifier,
    business_lines,
    trade_status,
    props_status
FROM
    FilteredInstances
WHERE
    rn = 1
  AND (dt = 'R2025-04-15' OR dt < '2025-04-15')
  AND instance_status IS NOT NULL
  AND channel_type = 'WRITE_OFF'
ORDER BY
    created_date DESC;

