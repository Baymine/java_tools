-- set enable_sql_cache=false;explain select
--     SUM(CAST(ped_all_income AS DOUBLE)) as ped_all_income
-- from
--     app.app_ea_whole_income_sum_d
-- where
--     1 = 1
--   and dt >= '2025-03-01'
--   and dt <= '2025-03-30'
--   and kaohe_second_dept_name in ('快递快运事业部')
--   and kaohe_belong_province_name in ('上海区')
--   and ((kaohe_second_dept_name = '快递快运事业部'))


-- set enable_sql_cache=false;select
--     date_id,
--     sku_id,
--     sku_name,
--     cast(bj_qtty as INTEGER) as bj_qtty,
--     cast(sh_qtty as INTEGER) as sh_qtty,
--     cast(gz_qtty as INTEGER) as gz_qtty,
--     cast(cd_qtty as INTEGER) as cd_qtty,
--     cast(wh_qtty as INTEGER) as wh_qtty,
--     cast(sy_qtty as INTEGER) as sy_qtty,
--     cast(xan_qtty as INTEGER) as xan_qtty,
--     cast(bj_qtty + sh_qtty + gz_qtty + cd_qtty + wh_qtty + sy_qtty + xan_qtty as INTEGER) as sum_qtty,
--     cast(bj_stock_qtty as INTEGER) as bj_stock_qtty,
--     cast(sh_stock_qtty as INTEGER) as sh_stock_qtty,
--     cast(gz_stock_qtty as INTEGER) as gz_stock_qtty,
--     cast(cd_stock_qtty as INTEGER) as cd_stock_qtty,
--     cast(wh_stock_qtty as INTEGER) as wh_stock_qtty,
--     cast(sy_stock_qtty as INTEGER) as sy_stock_qtty,
--     cast(xan_stock_qtty as INTEGER) as xan_stock_qtty,
--     cast(bj_stock_qtty + sh_stock_qtty + gz_stock_qtty + cd_stock_qtty + wh_stock_qtty + sy_stock_qtty + xan_stock_qtty as INTEGER) as sum_stock_qtty
-- from
--     app.app_book_sales_qtty_day_da_new_orc
-- where
--     dt >= '2025-04-01'
--   and dt <= '2025-05-20'
--   and sku_id in ('14603866') limit 1000

-- set enable_sql_cache=false;SELECT
--     alert_source,
--     client_ip,
--     '94.156.177.41' AS ioc,
--     ts,
--     'ip' AS type
-- FROM
--     cwfx.jd_intel_agg_data
-- WHERE
--     dt = '0-2025-05-06'
--   AND alert_source IN ('flink_oa_nat','flink_oa_flow','sysmon_flow','flink_nat','flink_idc_network','rasp_network')
--   AND contains(split(iocs, ','), '94.156.177.41')

set enable_sql_cache=false;
SELECT *
from app.app_jdr_abtest_shunt_dedup_i_d_d
where dt >= '2025-04-21'
  and exp_id = 'zzcdtx_76279'
  and shunt_key = 'pin'
  and shunt_value = 'jd_695bac6790e08'