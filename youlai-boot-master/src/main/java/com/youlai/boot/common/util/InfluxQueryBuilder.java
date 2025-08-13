//package com.youlai.boot.common.util;
//
//import org.apache.commons.lang3.StringUtils;
//import org.springframework.lang.NonNull;
//import org.springframework.lang.Nullable;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Objects;
//import java.util.regex.Pattern;
//import java.util.stream.Collectors;
//
///**
// *@Author: way
// *@CreateTime: 2025-06-06  10:55
// *@Description: 封装InfluxDB 查询语句构造工具类
// */
//public class InfluxQueryBuilder {
//    // InfluxDB 支持的时间单位（严格匹配：s-秒/m-分/h-时/d-天/w-周/M-月/y-年）
//    private static final Pattern TIME_UNIT_PATTERN = Pattern.compile("^[0-9]+(s|m|h|d|w|mo|y)$");
//    // 聚合函数白名单（防止非法函数注入）
//    private static final Pattern AGGREGATE_FUNC_PATTERN = Pattern.compile("^[a-zA-Z]+\\(\\)$");
//    // 新增：时间偏移量校验（匹配 InfluxDB 持续时间格式，如 8h、30m、1h30m）
////    private static final Pattern TIME_SHIFT_PATTERN = Pattern.compile("^([0-9]+[smhdwmoy])+$");
//    // 默认偏移量：8h（北京时间 UTC+8）
//    private String timeShiftDuration = "8h";
//
//    private String bucket;               // 必选：存储桶
//    private Long timeAmount;             // 必选：时间量（如 1 小时的 1）
//    private String timeUnit;             // 必选：时间单位（如 "h"）
//    private String measurement;          // 必选：测量点名称
//    private String deviceCode;           // 设备编码过滤值（可选）
//    private String roomId;
//    private final List<String> extraFilters;   // 额外过滤条件（可选）
//    private String aggregateFunction;    // 聚合函数（可选，如 "last()", "mean()"）
//    private boolean pivot;               // 是否透视（默认 false）
//    private String sortColumn = "_time";  // 默认排序字段
//    private String windowDuration; // 窗口时长（如 "1h"）
//    private boolean sortDesc = true;      // 默认降序
//    private String windowFunction; // 窗口聚合函数（如 "mean"）
//    private String windowPeriod;   // 窗口周期（如 "1h"）
//
//    private InfluxQueryBuilder() {
//        this.extraFilters = new ArrayList<>();
//        // 默认不设置聚合函数（用户需显式调用 aggregate() 才会添加）
//        this.aggregateFunction = null;
//    }
//
//    /**
//     * 设置时间偏移量（可选，默认 8h）
//     * @param duration 偏移量（格式示例：8h、30m、120s，需符合 InfluxDB 持续时间规范）
//     */
//    public InfluxQueryBuilder timeShift(@NonNull String duration) {  // <-- 关键改动2
////        if (!TIME_SHIFT_PATTERN.matcher(duration).matches()) {
////            throw new IllegalArgumentException("时间偏移量格式错误（示例：8h、30m、120s）");
////        }
//        this.timeShiftDuration = duration;
//        return this;
//    }
//
//    /**
//     * 创建新构建器实例
//     */
//    public static InfluxQueryBuilder newBuilder() {
//        return new InfluxQueryBuilder();
//    }
//
//    /**
//     * 设置存储桶（必选）
//     */
//    public InfluxQueryBuilder bucket(@NonNull String bucket) {
//        this.bucket = bucket;
//        return this;
//    }
//
//    /**
//     * 设置查询时间范围（最近 N 时间单位，必选）
//     * @param amount 时间量（必须 > 0，如 1）
//     * @param unit 时间单位（严格匹配：s/m/h/d/w/M/y）
//     */
//    public InfluxQueryBuilder last(@NonNull Long amount, @NonNull String unit) {
//        this.timeAmount = amount;
//        this.timeUnit = unit;
//        return this;
//    }
//
//    /**
//     * 设置测量点名称（必选）
//     */
//    public InfluxQueryBuilder measurement(@NonNull String measurement) {
//        this.measurement = measurement;
//        return this;
//    }
//
//    /**
//     * 设置设备编码过滤条件（可选，自动转义引号防注入）
//     */
//    public InfluxQueryBuilder deviceCode(@Nullable String deviceCode) {
//        // 转义双引号，防止 Flux 注入（如 deviceCode = "abc" OR 1=1）
//        this.deviceCode = deviceCode != null ? deviceCode.replace("\"", "\\\"") : null;
//        return this;
//    }
//
//    /**
//     * 设置设备编码过滤条件（可选，自动转义引号防注入）
//     */
//    public InfluxQueryBuilder roomId(@Nullable String roomId) {
//        // 转义双引号，防止 Flux 注入（如 deviceCode = "abc" OR 1=1）
//        this.roomId = roomId != null ? roomId.replace("\"", "\\\"") : null;
//        return this;
//    }
//
//    /**
//     * 添加额外过滤条件（可选，自动转义引号防注入）
//     * @param filter 过滤表达式（如 "r.temperature > 25"）
//     */
//    public InfluxQueryBuilder addFilter(@NonNull String filter) {
////        // 转义双引号，防止 Flux 注入
////        this.extraFilters.add(filter.replace("\"", "\\\""));
////        return this;
//        // 仅转义反斜杠（防止路径注入），保留双引号
//        this.extraFilters.add(filter.replace("\\", "\\\\"));
//        return this;
//    }
//
//    /**
//     * 设置聚合函数（可选，仅允许白名单内的函数）
//     * @param function 聚合函数（如 "last()", "mean()", "sum()"）
//     */
//    public InfluxQueryBuilder aggregate(@NonNull String function) {
//        if (!AGGREGATE_FUNC_PATTERN.matcher(function).matches()) {
//            throw new IllegalArgumentException("聚合函数仅支持字母+()格式（如 last()、mean()）");
//        }
//        this.aggregateFunction = function;
//        return this;
//    }
//
//    /**
//     * 启用透视操作（可选，将窄表转宽表）
//     */
//    public InfluxQueryBuilder pivot() {
//        this.pivot = true;
//        return this;
//    }
//
//    /**
//     * 设置排序（可选，默认按 _time 降序）
//     * @param column 排序字段（默认 "_time"）
//     * @param desc 是否降序（默认 true）
//     */
//    public InfluxQueryBuilder sort(@Nullable String column, boolean desc) {
//        this.sortColumn = column != null ? column : "_time";
//        this.sortDesc = desc;
//        return this;
//    }
//
//    /**
//     * 设置按时间降序排序（默认行为）
//     */
//    public InfluxQueryBuilder sort() {
//        return sort(null, true);
//    }
//
//    /**
//     * 设置时间窗口分组（可选）
//     * @param duration 窗口时长（格式示例：1h、30m）
//     */
//    public InfluxQueryBuilder window(@NonNull String duration) {
////        if (!TIME_SHIFT_PATTERN.matcher(duration).matches()) {
////            throw new IllegalArgumentException("窗口时长格式错误（示例：1h、30m）");
////        }
//        this.windowDuration = duration;
//        return this;
//    }
//
//    /**
//     * 设置聚合窗口（简化版，只保留every和fn）
//     * @param every 窗口周期（如 "1h"）
//     * @param fn 聚合函数（如 "mean", "last"）
//     */
//    public InfluxQueryBuilder aggregateWindow(@NonNull String every, @NonNull String fn) {
////        if (!TIME_SHIFT_PATTERN.matcher(every).matches()) {
////            throw new IllegalArgumentException("窗口周期格式错误（示例：1h、30m）");
////        }
//        this.windowPeriod = every;
//        this.windowFunction = fn;
//        return this;
//    }
//
//    /**
//     * 构建 Flux 查询字符串
//     * @return 符合 InfluxDB 规范的 Flux 查询语句
//     * @throws IllegalArgumentException 参数校验失败时抛出
//     */
//    public String build() {
//        validateParams();
//
//        // 基础查询模板
//        StringBuilder flux = new StringBuilder();
//        flux.append(String.format("from(bucket: \"%s\")\n", bucket));
//        flux.append(String.format("  |> range(start: -%d%s)\n", timeAmount, timeUnit));
//        flux.append(String.format("  |> filter(fn: (r) => r._measurement == \"%s\"", measurement));
//
//        // 添加设备编码过滤（可选）
//        if (StringUtils.isNotBlank(deviceCode)) {
//            flux.append(String.format(" and r.deviceCode == \"%s\"", deviceCode));
//        }
//        if (StringUtils.isNotBlank(roomId)) {
//            flux.append(String.format(" and r.roomId == \"%s\"", roomId));
//        }
//
//        // 添加额外过滤条件（可选）
//        if (!extraFilters.isEmpty()) {
//            String extraFilterStr = extraFilters.stream()
//                    .map(f -> " and " + f)
//                    .collect(Collectors.joining());
//            flux.append(extraFilterStr);
//        }
//        flux.append(")\n");
//        // 窗口聚合逻辑（优先处理）
//        if (windowPeriod != null) {
//            flux.append(String.format("  |> aggregateWindow(every: %s, fn: %s)\n",
//                    windowPeriod, windowFunction));
//        }
//        // 兼容旧版window+aggregate（如有需要）
//        else if (StringUtils.isNotBlank(windowDuration)) {
//            flux.append(String.format("  |> window(every: %s)\n", windowDuration));
//            if (StringUtils.isNotBlank(aggregateFunction)) {
//                flux.append(String.format("  |> %s\n", aggregateFunction));
//            }
//        }
//        // 聚合操作（可选）
//        if (StringUtils.isNotBlank(aggregateFunction)) {
//            flux.append(String.format("  |> %s\n", aggregateFunction));
//        }
//
//        // 透视操作（可选）
//        if (pivot) {
//            flux.append("  |> pivot(rowKey: [\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")\n");
//        }
//        // 新增：时间偏移（UTC -> 北京时间）  <-- 关键改动3
//        flux.append(String.format("  |> timeShift(duration: %s)\n", timeShiftDuration));
//        if (sortColumn != null) {
//            flux.append(String.format("  |> sort(columns: [\"%s\"], desc: %b)\n",
//                    sortColumn, sortDesc));
//        }
//        return flux.toString().trim();
//    }
//
//    /**
//     * 参数校验逻辑（严格确保查询合法性）
//     */
//    private void validateParams() {
//        // 必选参数非空校验
//        Objects.requireNonNull(bucket, "bucket 不能为空");
//        Objects.requireNonNull(timeAmount, "timeAmount 不能为空");
//        Objects.requireNonNull(timeUnit, "timeUnit 不能为空");
//        Objects.requireNonNull(measurement, "measurement 不能为空");
//
//        // 时间量必须大于 0
//        if (timeAmount <= 0) {
//            throw new IllegalArgumentException("timeAmount 必须大于 0");
//        }
//
//        // 时间单位严格校验（仅允许 s/m/h/d/w/M/y）
////        if (!TIME_UNIT_PATTERN.matcher(timeUnit).matches()) {
////            throw new IllegalArgumentException("timeUnit 必须是 Influx 支持的单位（s/m/h/d/w/M/y）");
////        }
//        // 校验时间偏移量格式（新增）
////        if (!TIME_SHIFT_PATTERN.matcher(timeShiftDuration).matches()) {
////            throw new IllegalArgumentException("timeShiftDuration 格式错误（示例：8h、30m、120s）");
////        }
//    }
//}
package com.youlai.boot.common.util;

import org.springframework.lang.NonNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 高级InfluxDB Flux查询语句构建器
 * 支持完整的Flux查询语法，提供安全、易用的API
 */
public class InfluxQueryBuilder {
    // 预定义常量
    public static final String SORT_ASC = "asc";
    public static final String SORT_DESC = "desc";

    // 默认值
    private static final String DEFAULT_TIME_SHIFT = "8h";
    private static final String DEFAULT_SORT_COLUMN = "_time";

    // 核心参数
    private String bucket;
    private Long timeAmount;
    private String timeUnit;
    private String measurement;
    private final List<String> filters = new ArrayList<>();
    private final Map<String, String> tagFilters = new HashMap<>();
    private final Map<String, String> fieldFilters = new HashMap<>();
    private final List<String> functions = new ArrayList<>();
    private boolean pivot = false;
    private String sortColumn = DEFAULT_SORT_COLUMN;
    private String sortDirection = SORT_DESC;
    private String windowEvery;
    private String windowFunction;
    private String timeShift = DEFAULT_TIME_SHIFT;
    private Integer limit;
    private Integer offset;
    private boolean useFillPrevious = false;
    private boolean yield = true;
    private boolean keepEmpty = false;
    private boolean useTodayRange = false;

    private InfluxQueryBuilder() {
    }

    /**
     * 创建新构建器实例
     */
    public static InfluxQueryBuilder newBuilder() {
        return new InfluxQueryBuilder();
    }

    // ========== 基础参数设置 ==========

    public InfluxQueryBuilder bucket(@NonNull String bucket) {
        this.bucket = bucket;
        return this;
    }

    public InfluxQueryBuilder range(long amount, @NonNull String unit) {
        this.timeAmount = amount;
        this.timeUnit = unit;
        return this;
    }

    /**
     * 设置按当前小时查询
     */
    public InfluxQueryBuilder currentHour() {
        this.timeAmount = 1L;
        this.timeUnit = "h";
        return this;
    }

    /**
     * 设置按当周查询（从周一到周日）
     */
    public InfluxQueryBuilder currentWeek() {
        this.timeAmount = 1L;
        this.timeUnit = "w";
        return this;
    }

    /**
     * 设置按当月查询
     */
    public InfluxQueryBuilder currentMonth() {
        this.timeAmount = 1L;
        this.timeUnit = "mo";
        return this;
    }

    /**
     * 设置按当年查询
     */
    public InfluxQueryBuilder currentYear() {
        this.timeAmount = 1L;
        this.timeUnit = "y";
        return this;
    }

    /**
     * 设置按当天查询
     */
    public InfluxQueryBuilder today() {
        this.useTodayRange = true;
        return this;
    }

    public InfluxQueryBuilder measurement(@NonNull String measurement) {
        this.measurement = measurement;
        return this;
    }

    // ========== 过滤条件 ==========

    public InfluxQueryBuilder filter(@NonNull String filterExpr) {
        this.filters.add(filterExpr);
        return this;
    }

    public InfluxQueryBuilder tag(@NonNull String tagName, @NonNull String tagValue) {
        this.tagFilters.put(tagName, tagValue);
        return this;
    }



    public InfluxQueryBuilder fields(@NonNull String... fields) {
        for (String field : fields) {
            this.fieldFilters.put(field, null);
        }
        return this;
    }

    public InfluxQueryBuilder fieldFilter(@NonNull String field, @NonNull String condition) {
        this.fieldFilters.put(field, condition);
        return this;
    }

    // ========== 数据处理 ==========

    public InfluxQueryBuilder window(@NonNull String every, @NonNull String fn) {
        this.windowEvery = every;
        this.windowFunction = fn;
        return this;
    }

    public InfluxQueryBuilder aggregate(@NonNull String fn) {
        this.functions.add(fn);
        return this;
    }

    public InfluxQueryBuilder pivot() {
        this.pivot = true;
        return this;
    }
    public InfluxQueryBuilder sum() {
        this.functions.add("sum()");
        return this;
    }
    /**
     * 启用使用上一个值填充
     */
    public InfluxQueryBuilder fill() {
        this.useFillPrevious = true;
        return this;
    }

    // ========== 结果处理 ==========

    public InfluxQueryBuilder sort(@NonNull String column, @NonNull String direction) {
        this.sortColumn = column;
        this.sortDirection = direction;
        return this;
    }

    public InfluxQueryBuilder limit(int n) {
        this.limit = n;
        return this;
    }

    public InfluxQueryBuilder offset(int n) {
        this.offset = n;
        return this;
    }

    public InfluxQueryBuilder timeShift(@NonNull String duration) {
        this.timeShift = duration;
        return this;
    }

    public InfluxQueryBuilder yield(boolean enable) {
        this.yield = enable;
        return this;
    }

    public InfluxQueryBuilder keepEmpty(boolean keep) {
        this.keepEmpty = keep;
        return this;
    }
// 在 InfluxQueryBuilder 类中添加以下方法

    /**
     * 设置查询为计数模式（返回记录总数）
     */
    public InfluxQueryBuilder count() {
        this.functions.add("count()");
        return this;
    }


    // ========== 构建方法 ==========

    public String build() {
        validateParams();

        StringBuilder flux = new StringBuilder();

        // 基础查询
        flux.append(String.format("from(bucket: \"%s\")\n", bucket));
        if (useTodayRange) {
            flux.append("  |> range(start: today())\n");
        } else {
            flux.append(String.format("  |> range(start: -%d%s)\n", timeAmount, timeUnit));
        }

        // 构建过滤条件
        buildFilters(flux);

        // 窗口聚合
        if (windowEvery != null) {
            flux.append(String.format("  |> aggregateWindow(every: %s, fn: %s)\n",
                    windowEvery, windowFunction));

            // 在窗口聚合后添加fill
            if (useFillPrevious) {
                flux.append("  |> fill(usePrevious: true)\n");
            }
        }

        // 其他聚合函数
        functions.forEach(fn -> flux.append(String.format("  |> %s\n", fn)));

        // 透视
        if (pivot) {
            flux.append("  |> pivot(rowKey: [\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")\n");
        }

        // 时间偏移
        flux.append(String.format("  |> timeShift(duration: %s)\n", timeShift));

        // 排序
        flux.append(String.format("  |> sort(columns: [\"%s\"], desc: %s)\n",
                sortColumn, SORT_DESC.equals(sortDirection)));

        // 分页
        if (limit != null) {
            flux.append(String.format("  |> limit(n: %d", limit));
            if (offset != null) {
                flux.append(String.format(", offset: %d", offset));
            }
            flux.append(")\n");
        }

        // 是否保留空结果
        if (keepEmpty) {
            flux.append("  |> keep(columns: [\"_time\"])\n");
        }

        // 是否生成结果
        if (yield) {
            flux.append("  |> yield()\n");
        }

        return flux.toString().trim();
    }
    /**
     * 专用于构建计数查询的快捷方法
     * @return 仅包含count结果的Flux查询语句
     */
    public String buildCountQuery() {
        validateParams();

        StringBuilder flux = new StringBuilder();
        flux.append(String.format("from(bucket: \"%s\")\n", bucket));

        if (useTodayRange) {
            flux.append("  |> range(start: today())\n");
        } else {
            flux.append(String.format("  |> range(start: -%d%s)\n", timeAmount, timeUnit));
        }

        // 构建过滤条件
        buildFilters(flux);

        // 添加计数
        flux.append("  |> count()\n");

        // 时间偏移（如需要）
        if (timeShift != null) {
            flux.append(String.format("  |> timeShift(duration: %s)\n", timeShift));
        }

        return flux.toString().trim();
    }

    private void buildFilters(StringBuilder flux) {
        flux.append(String.format("  |> filter(fn: (r) => r._measurement == \"%s\"", measurement));

        // 添加tag过滤
        tagFilters.forEach((k, v) ->
                flux.append(String.format(" and r.%s == \"%s\"", k, v)));

        // 添加字段过滤
        if (!fieldFilters.isEmpty()) {
            String fieldConditions = fieldFilters.entrySet().stream()
                    .map(e -> {
                        if (e.getValue() == null) {
                            return String.format("r._field == \"%s\"", e.getKey());
                        } else {
                            return String.format("(r._field == \"%s\" and %s)", e.getKey(), e.getValue());
                        }
                    })
                    .collect(Collectors.joining(" or "));
            flux.append(" and (").append(fieldConditions).append(")");
        }

        // 添加额外过滤条件
        if (!filters.isEmpty()) {
            flux.append(" and ").append(String.join(" and ", filters));
        }

        flux.append(")\n");
    }

    private void validateParams() {
        Objects.requireNonNull(bucket, "bucket不能为空");
        if (!useTodayRange) {
            Objects.requireNonNull(timeAmount, "timeAmount不能为空");
            Objects.requireNonNull(timeUnit, "timeUnit不能为空");
        }
        Objects.requireNonNull(measurement, "measurement不能为空");
        if (!useTodayRange) {
            if (timeAmount <= 0) {
                throw new IllegalArgumentException("timeAmount必须大于0");
            }
        }

    }
}

