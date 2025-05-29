# SQL一致性验证工具

这个工具用于验证Doris集群中SQL查询的结果集行数是否与预期一致。它从CSV文件中读取SQL查询和预期结果行数，在Doris集群中执行这些查询，并将不匹配的SQL保存到单独的文件中。

## 特点

- 从CSV文件读取SQL查询和预期结果行数
- 并发执行SQL查询以提高性能
- 将不匹配的SQL保存到单独的文件
- 详细的日志记录和错误处理

## 项目结构

- `runner.java` - 主类，程序入口
- `SqlValidator.java` - SQL验证器，负责从CSV文件读取SQL并在Doris集群上执行验证
- `SqlRecord.java` - SQL记录类，表示CSV文件中的一行
- `ValidationResult.java` - 验证结果类，表示SQL验证的结果

## CSV文件格式

CSV文件应包含以下列（以制表符分隔）：

```
presto_doris_rel	presto_cnt_result	doris_cnt_result	presto_sql	doris_sql
```

其中：
- `presto_doris_rel`：关系标识符
- `presto_cnt_result`：预期结果集行数
- `doris_cnt_result`：Doris结果集行数（仅供参考，实际验证时不使用）
- `presto_sql`：要执行的SQL查询
- `doris_sql`：Doris SQL查询（仅供参考，实际验证时不使用）

## 使用方法

### 编译

```bash
mvn clean package
```

### 运行

```bash
java -cp target/java_tools-1.0-SNAPSHOT.jar com.consistency.Runner <csv_file_path> [thread_count] [doris_host]
```

参数说明：
- `csv_file_path`：CSV文件路径（必需）
- `thread_count`：并发线程数（可选，默认为10）
- `doris_host`：Doris集群主机名（可选，默认为localhost）

### 示例

```bash
java -cp target/java_tools-1.0-SNAPSHOT.jar com.consistency.Runner src/main/java/com/consistency/sql_data/data.csv 20 doris.example.com
```

## 输出

程序会在当前目录下创建一个`sql_validation_results`文件夹，并在其中生成一个包含不匹配SQL的文件，文件名格式为`mismatched_sql_YYYYMMDD_HHMMSS.sql`。

不匹配的SQL记录格式如下：

```sql
-- Relation: relation_name
-- Expected count: 100
-- Actual count: 95
SELECT * FROM table WHERE condition;
```

对于执行出错的SQL，格式如下：

```sql
-- Relation: relation_name
-- Expected count: 100
-- Error: error_message
SELECT * FROM table WHERE condition;
```
