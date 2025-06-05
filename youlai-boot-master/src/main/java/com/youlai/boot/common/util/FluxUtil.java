package com.youlai.boot.common.util;

import com.influxdb.annotations.Measurement;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 *@Author: way
 *@CreateTime: 2025-06-05  10:37
 *@Description: TODO
 */
public class FluxUtil {
    private static final String DEFAULT_START_TIME = "1970-01-01T00:00:00.000Z";

    /**
     * 获取实体类对应的InfluxDB measurement名称
     *
     * @param clazz 实体类
     * @return 测量名称
     */
    public static String getTableName(@NonNull Class<?> clazz) {
        Objects.requireNonNull(clazz, "clazz不能为null");
        Measurement measurement = clazz.getAnnotation(Measurement.class);
        return measurement != null ? measurement.name() : null;
    }

    /**
     * 构建基础查询语句
     *
     * @param builder   StringBuilder对象
     * @param bucketName 存储桶名称
     * @param tableName 表名
     * @param start 开始时间
     * @param stop 结束时间
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendCommonFlux(@NonNull StringBuilder builder,
                                                 @NonNull String bucketName,
                                                 @NonNull String tableName,
                                                 String start, String stop) {
        return appendBucketFlux(builder, bucketName)
                .append(" ")
                .append(appendTimeRangeFlux(builder, start, stop))
                .append(" ")
                .append(appendTableFlux(builder, tableName));
    }


    /**
     * 添加from(bucket: "...")语句
     *
     * @param builder   StringBuilder对象
     * @param bucketName 存储桶名称
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendBucketFlux(@NonNull StringBuilder builder, @NonNull String bucketName) {
        return builder.append("from(bucket: \"").append(bucketName).append("\")");
    }

    /**
     * 添加filter(_measurement == "...")语句
     *
     * @param builder   StringBuilder对象
     * @param tableName 表名
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendTableFlux(@NonNull StringBuilder builder, @NonNull String tableName) {
        return builder.append("|> filter(fn: (r) => r._measurement == \"").append(tableName).append("\")");
    }

    /**
     * 添加contains过滤条件
     *
     * @param builder   StringBuilder对象
     * @param fieldName 字段名
     * @param list      包含字段值列表
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendContainsFlux(@NonNull StringBuilder builder,
                                                   @NonNull String fieldName,
                                                   List<String> list) {
        if (!CollectionUtils.isEmpty(list)) {
            builder.append("|> filter(fn: (r) => contains(value: r[\"")
                    .append(fieldName)
                    .append("\"], set: [");
            appendListAsFluxArray(builder, list);
            builder.append("]))");
        }
        return builder;
    }

    public static void appendTagField(@NonNull StringBuilder builder, @NonNull String field) {
        builder.append("|> filter(fn: (r) => r._field == \"").append(field).append("\") ");
    }

    /**
     * 添加时间范围查询
     *
     * @param builder StringBuilder对象
     * @param start 开始时间
     * @param stop  结束时间
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendTimeRangeFlux(@NonNull StringBuilder builder, String start, String stop) {
        if (StringUtils.isBlank(start)) {
            start = DEFAULT_START_TIME;
        }
        if (StringUtils.isBlank(stop)) {
            return builder.append("|> range(start: ").append(start).append(")");
        } else {
            return builder.append("|> range(start: ").append(start).append(", stop: ").append(stop).append(")");
        }
    }

    public static void appendTimeRangeLastFlux(@NonNull StringBuilder builder, int time, String unit) {
        builder.append("|> range(start: -").append(time).append(unit).append(" )");
    }

    /**
     * 添加drop(columns: [...])语句
     *
     * @param builder StringBuilder对象
     * @param args    要删除的列名
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendDropFlux(StringBuilder builder, String... args) {
        if (args.length == 0) {
            return builder.append("|> drop(columns: [\"host\"])");
        }
        builder.append("|> drop(columns: [")
                .append(String.join(",", args))
                .append("])");
        return builder;
    }

    /**
     * 添加keep(columns: [...])语句
     *
     * @param builder StringBuilder对象
     * @param args    要保留的列名
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendKeepFlux(StringBuilder builder, String... args) {
        if (args.length == 0) {
            return builder;
        }
        builder.append("|> keep(columns: [")
                .append(String.join(",", args))
                .append("])");
        return builder;
    }

    /**
     * 添加duplicate(column: "...", as: "...")语句
     *
     * @param builder   StringBuilder对象
     * @param oldField  原字段名
     * @param newField  新字段名
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendDuplicateFlux(StringBuilder builder, String oldField, String newField) {
        return builder.append("|> duplicate(column: \"").append(oldField).append("\", as: \"").append(newField).append("\")");
    }

    /**
     * 添加rename(columns: {...})语句
     *
     * @param builder   StringBuilder对象
     * @param oldField  原字段名
     * @param newField  新字段名
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendRenameFlux(StringBuilder builder, String oldField, String newField) {
        return builder.append(" |> rename(columns: {\"").append(oldField).append("\": \"").append(newField).append("\"}) ");
    }


    /**
     * 添加first()函数
     *
     * @param builder StringBuilder对象
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendFirstFlux(StringBuilder builder) {
        return builder.append("|> first()");
    }

    /**
     * 添加last()函数
     *
     * @param builder StringBuilder对象
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendLastFlux(StringBuilder builder) {
        return builder.append("|> last()");
    }

    /**
     * 添加limit(n:..., offset:...)语句
     *
     * @param builder StringBuilder对象
     * @param n       返回记录条数
     * @param offset  偏移量
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendLimitFlux(@NonNull StringBuilder builder, int n, int offset) {
        Objects.requireNonNull(builder, "builder不能为null");
        if (n < 0) {
            throw new IllegalArgumentException("n必须大于等于0");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset必须大于等于0");
        }

        return builder.append("|> limit(n:").append(n).append(", offset: ").append(offset).append(")");
    }


    /**
     * 添加group(columns: [...])语句
     *
     * @param builder StringBuilder对象
     * @param columns 分组字段列表
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendGroupFlux(@NonNull StringBuilder builder, String... columns) {
        Objects.requireNonNull(builder, "builder不能为null");

        if (columns == null || columns.length == 0) {
            return builder.append("|> group()");
        }

        builder.append("|> group(columns:[");
        for (int i = 0; i < columns.length; i++) {
            if (i != 0) builder.append(",");
            builder.append("\"").append(columns[i]).append("\"");
        }
        return builder.append("])");
    }


    /**
     * 添加distinct操作
     *
     * @param builder StringBuilder对象
     * @param columns 分组字段（仅第一个有效）
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendDistinctFlux(StringBuilder builder, String... columns) {
        if (columns.length == 0) {
            return builder.append("|> distinct()");
        }
        return builder.append("|> distinct(column:\"").append(columns[0]).append("\")");
    }


    /**
     * 添加count()函数
     *
     * @param builder StringBuilder对象
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendCountFlux(StringBuilder builder) {
        return builder.append("|> count()");
    }


    /**
     * 添加count(column: "...")函数
     *
     * @param builder   StringBuilder对象
     * @param fieldName 字段名
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendCountFlux(StringBuilder builder, String fieldName) {
        return builder.append("|> count(column: \"").append(fieldName).append("\")");
    }


    /**
     * 添加top(n:...)函数
     *
     * @param builder StringBuilder对象
     * @param n       取前n个
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendTopFlux(StringBuilder builder, int n) {
        return builder.append("|> top(n:").append(n).append(")");
    }


    /**
     * 添加bottom(n:...)函数
     *
     * @param builder StringBuilder对象
     * @param n       取后n个
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendBottomFlux(StringBuilder builder, int n) {
        return builder.append("|> bottom(n:").append(n).append(")");
    }


    /**
     * 添加sort排序操作
     *
     * @param builder StringBuilder对象
     * @param descFlag 是否降序排序
     * @param columns 排序字段列表
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendSortFlux(
            @NonNull StringBuilder builder,
            boolean descFlag,
            String... columns) {

        Objects.requireNonNull(builder, "builder不能为null");

        if (columns == null || columns.length == 0) {
            return builder.append("|> sort(columns: [\"_value\"], desc: ").append(descFlag).append(")");
        }

        builder.append("|> sort(columns: [\"");
        builder.append(String.join("\", \"", columns));
        return builder.append("\"], desc: ").append(descFlag).append(")");
    }


    /**
     * 添加timeShift(duration: ...)函数
     *
     * @param builder StringBuilder对象
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendTimeShiftFlux(StringBuilder builder) {
        return builder.append("|> timeShift(duration: 8h)");
    }


    /**
     * 添加filter操作，支持多个字段匹配
     *
     * @param builder   StringBuilder对象
     * @param list      字段值列表
     * @param operator  操作符（如==, !=）
     * @param join      条件连接符（如or, and）
     * @param fieldName 字段名
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendFilterFlux(
            @NonNull StringBuilder builder,
            List<String> list,
            @NonNull String operator,
            @NonNull String join,
            @NonNull String fieldName) {

        if (CollectionUtils.isEmpty(list)) return builder;

        for (int i = 0; i < list.size(); i++) {
            if (i != 0) builder.append(join);
            builder.append(" r.").append(fieldName)
                    .append(" ").append(operator)
                    .append(" \"").append(list.get(i))
                    .append("\" ");
        }
        return builder.append(")");
    }


    /**
     * 添加filter操作，基于Map过滤条件
     *
     * @param builder   StringBuilder对象
     * @param map       过滤字段和值
     * @param operator  操作符（如==, !=）
     * @param join      条件连接符（如or, and）
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendFilterFlux(
            @NonNull StringBuilder builder,
            @NonNull Map<String, Object> map,
            @NonNull String operator,
            @NonNull String join) {

        Objects.requireNonNull(builder, "builder不能为null");
        if (map.isEmpty()) return builder;

        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (first) {
                builder.append("|> filter(fn: (r) =>");
                first = false;
            } else {
                builder.append(join);
            }
            builder.append(" r.")
                    .append(entry.getKey())
                    .append(" ").append(operator)
                    .append(" \"").append(entry.getValue())
                    .append("\" ");
        }
        return builder.append(")");
    }


    /**
     * 添加多组filter嵌套条件
     *
     * @param builder   StringBuilder对象
     * @param list      包含多组filter条件的Map列表
     * @param innerJoin 内部连接符
     * @param operator  操作符
     * @param outerJoin 外部连接符
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendMulFilterFlux(
            @NonNull StringBuilder builder,
            @NonNull List<Map<String, Object>> list,
            @NonNull String innerJoin,
            @NonNull String operator,
            @NonNull String outerJoin) {

        Objects.requireNonNull(builder, "builder不能为null");

        if (list.isEmpty()) {
            return builder;
        }

        builder.append("|> filter(fn: (r) => ");

        for (int i = 0; i < list.size(); i++) {
            Map<String, Object> map = list.get(i);
            boolean firstInner = true;

            if (i > 0) {
                builder.append(outerJoin);
            }
            builder.append("( ");

            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!firstInner) {
                    builder.append(innerJoin);
                }
                builder.append("r.")
                        .append(entry.getKey())
                        .append(" ").append(operator)
                        .append(" \"").append(entry.getValue())
                        .append("\" ");
                firstInner = false;
            }

            builder.append(") ");
        }

        return builder.append(")");
    }


    /**
     * 添加aggregateWindow操作
     *
     * @param builder StringBuilder对象
     * @param step    时间窗口大小（如"1h"）
     * @param aggType 聚合函数名（如mean, sum）
     * @param createEmpty 是否创建空结果
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendAggregateWindowFlux(StringBuilder builder, String step, String aggType, boolean createEmpty) {
        return builder.append("|> aggregateWindow(every: ").append(step)
                .append(", fn: ").append(aggType)
                .append(", createEmpty: ").append(createEmpty).append(")");
    }


    /**
     * 添加window操作
     *
     * @param builder StringBuilder对象
     * @param step    时间窗口大小（如"1h"）
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendWindowFlux(StringBuilder builder, String step) {
        return builder.append("|> window(every: ").append(step).append(")");
    }


    /**
     * 添加聚合操作
     *
     * @param builder StringBuilder对象
     * @param aggType 聚合函数名（如mean, sum）
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendAggregateFlux(StringBuilder builder, String aggType) {
        return builder.append("|> ").append(aggType).append("()");
    }


    /**
     * 添加yield操作
     *
     * @param builder StringBuilder对象
     * @param name    查询名称
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendYieldFlux(StringBuilder builder, String name) {
        return builder.append("|> yield(name: \"").append(name).append("\")");
    }


    /**
     * 添加truncateTimeColumn操作
     *
     * @param builder StringBuilder对象
     * @param step    时间单位（如1h, 1d）
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendTruncateTimeColumn(StringBuilder builder, String step) {
        return builder.append("|> truncateTimeColumn(unit: ").append(step).append(")");
    }


    /**
     * 添加import语句
     *
     * @param builder StringBuilder对象
     * @param name    模块名称
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendImportFlux(StringBuilder builder, String name) {
        return builder.append("import \"").append(name).append("\"");
    }


    /**
     * 添加exists过滤条件
     *
     * @param builder StringBuilder对象
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendExistsFlux(StringBuilder builder) {
        return builder.append("|> filter(fn: (r) => exists r._value)");
    }


    /**
     * 添加过滤大于0的条件
     *
     * @param builder StringBuilder对象
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendZeroFlux(StringBuilder builder) {
        return builder.append("|> filter(fn: (r) => r._value > 0)");
    }


    /**
     * 添加pivot转换操作
     *
     * @param builder StringBuilder对象
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendPivotFlux(StringBuilder builder) {
        return builder.append("|> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")");
    }


    /**
     * 添加pivot转换操作
     *
     * @param builder StringBuilder对象
     * @param rowKeys 行键字段列表
     * @param columnKeys 列键字段列表
     * @param valueColumn 值字段名
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendPivotFlux(StringBuilder builder, List<String> rowKeys, List<String> columnKeys, String valueColumn) {
        Objects.requireNonNull(builder, "builder不能为null");
        Objects.requireNonNull(rowKeys, "rowKeys不能为null");
        Objects.requireNonNull(columnKeys, "columnKeys不能为null");
        Objects.requireNonNull(valueColumn, "valueColumn不能为null");

        builder.append("|> pivot(rowKey: [");
        appendListAsFluxArray(builder, rowKeys);
        builder.append("], columnKey: [");
        appendListAsFluxArray(builder, columnKeys);
        return builder.append("], valueColumn: \"").append(valueColumn).append("\")");
    }


    /**
     * 将字符串列表转为Flux数组格式 ["val1", "val2"]
     *
     * @param builder StringBuilder对象
     * @param list 字符串列表
     */
    private static void appendListAsFluxArray(StringBuilder builder, List<String> list) {
        if (list == null || list.isEmpty()) return;

        builder.append("\"").append(list.get(0)).append("\"");

        for (int i = 1; i < list.size(); i++) {
            builder.append(", \"").append(list.get(i)).append("\"");
        }
    }


    /**
     * 添加map操作，用于对字段进行函数转换
     *
     * @param builder     StringBuilder对象
     * @param fieldName   字段名
     * @param functionName 函数名（如"int", "float"）
     * @return 返回当前builder以支持链式调用
     */
    public static StringBuilder appendMapFlux(StringBuilder builder, String fieldName, String functionName) {
        return builder.append("|> map(fn: (r) => { r[\"")
                .append(fieldName)
                .append("\"] = ")
                .append(functionName)
                .append("(r[\"")
                .append(fieldName)
                .append("\"]); return r; })");
    }

}
