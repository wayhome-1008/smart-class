package com.youlai.boot.common.util;

import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 *@Author: way
 *@CreateTime: 2025-06-06  10:55
 *@Description: TODO
 */
public class InfluxQueryBuilder {
    // InfluxDB 支持的时间单位（严格匹配：s-秒/m-分/h-时/d-天/w-周/M-月/y-年）
    private static final Pattern TIME_UNIT_PATTERN = Pattern.compile("^[smhdwMy]$");
    // 聚合函数白名单（防止非法函数注入）
    private static final Pattern AGGREGATE_FUNC_PATTERN = Pattern.compile("^[a-zA-Z]+\\(\\)$");
    // 新增：时间偏移量校验（匹配 InfluxDB 持续时间格式，如 8h、30m）
    private static final Pattern TIME_SHIFT_PATTERN = Pattern.compile("^[0-9]+[smhdwMy]$");
    // 默认偏移量：8h（北京时间 UTC+8）
    private String timeShiftDuration = "8h";

    private String bucket;               // 必选：存储桶
    private Long timeAmount;             // 必选：时间量（如 1 小时的 1）
    private String timeUnit;             // 必选：时间单位（如 "h"）
    private String measurement;          // 必选：测量点名称
    private String deviceCode;           // 设备编码过滤值（可选）
    private List<String> extraFilters;   // 额外过滤条件（可选）
    private String aggregateFunction;    // 聚合函数（可选，如 "last()", "mean()"）
    private boolean pivot;               // 是否透视（默认 false）
    private String sortColumn = "_time";  // 默认排序字段
    private boolean sortDesc = true;      // 默认降序
    private InfluxQueryBuilder() {
        this.extraFilters = new ArrayList<>();
        // 默认不设置聚合函数（用户需显式调用 aggregate() 才会添加）
        this.aggregateFunction = null;
    }

    /**
     * 设置时间偏移量（可选，默认 8h）
     * @param duration 偏移量（格式示例：8h、30m、120s，需符合 InfluxDB 持续时间规范）
     */
    public InfluxQueryBuilder timeShift(@NonNull String duration) {  // <-- 关键改动2
        if (!TIME_SHIFT_PATTERN.matcher(duration).matches()) {
            throw new IllegalArgumentException("时间偏移量格式错误（示例：8h、30m、120s）");
        }
        this.timeShiftDuration = duration;
        return this;
    }

    /**
     * 创建新的构建器实例
     */
    public static InfluxQueryBuilder newBuilder() {
        return new InfluxQueryBuilder();
    }

    /**
     * 设置存储桶（必选）
     */
    public InfluxQueryBuilder bucket(@NonNull String bucket) {
        this.bucket = bucket;
        return this;
    }

    /**
     * 设置查询时间范围（最近 N 时间单位，必选）
     * @param amount 时间量（必须 > 0，如 1）
     * @param unit 时间单位（严格匹配：s/m/h/d/w/M/y）
     */
    public InfluxQueryBuilder last(@NonNull Long amount, @NonNull String unit) {
        this.timeAmount = amount;
        this.timeUnit = unit;
        return this;
    }

    /**
     * 设置测量点名称（必选）
     */
    public InfluxQueryBuilder measurement(@NonNull String measurement) {
        this.measurement = measurement;
        return this;
    }

    /**
     * 设置设备编码过滤条件（可选，自动转义引号防注入）
     */
    public InfluxQueryBuilder deviceCode(@Nullable String deviceCode) {
        // 转义双引号，防止 Flux 注入（如 deviceCode = "abc" OR 1=1）
        this.deviceCode = deviceCode != null ? deviceCode.replace("\"", "\\\"") : null;
        return this;
    }

    /**
     * 添加额外过滤条件（可选，自动转义引号防注入）
     * @param filter 过滤表达式（如 "r.temperature > 25"）
     */
    public InfluxQueryBuilder addFilter(@NonNull String filter) {
//        // 转义双引号，防止 Flux 注入
//        this.extraFilters.add(filter.replace("\"", "\\\""));
//        return this;
        // 仅转义反斜杠（防止路径注入），保留双引号
        this.extraFilters.add(filter.replace("\\", "\\\\"));
        return this;
    }

    /**
     * 设置聚合函数（可选，仅允许白名单内的函数）
     * @param function 聚合函数（如 "last()", "mean()", "sum()"）
     */
    public InfluxQueryBuilder aggregate(@NonNull String function) {
        if (!AGGREGATE_FUNC_PATTERN.matcher(function).matches()) {
            throw new IllegalArgumentException("聚合函数仅支持字母+()格式（如 last()、mean()）");
        }
        this.aggregateFunction = function;
        return this;
    }

    /**
     * 启用透视操作（可选，将窄表转宽表）
     */
    public InfluxQueryBuilder pivot() {
        this.pivot = true;
        return this;
    }
    /**
     * 设置排序（可选，默认按 _time 降序）
     * @param column 排序字段（默认 "_time"）
     * @param desc 是否降序（默认 true）
     */
    public InfluxQueryBuilder sort(@Nullable String column, boolean desc) {
        this.sortColumn = column != null ? column : "_time";
        this.sortDesc = desc;
        return this;
    }

    /**
     * 设置按时间降序排序（默认行为）
     */
    public InfluxQueryBuilder sort() {
        return sort(null, true);
    }
    /**
     * 构建 Flux 查询字符串
     * @return 符合 InfluxDB 规范的 Flux 查询语句
     * @throws IllegalArgumentException 参数校验失败时抛出
     */
    public String build() {
        validateParams();

        // 基础查询模板
        StringBuilder flux = new StringBuilder();
        flux.append(String.format("from(bucket: \"%s\")\n", bucket));
        flux.append(String.format("  |> range(start: -%d%s)\n", timeAmount, timeUnit));
        flux.append(String.format("  |> filter(fn: (r) => r._measurement == \"%s\"", measurement));

        // 添加设备编码过滤（可选）
        if (StringUtils.isNotBlank(deviceCode)) {
            flux.append(String.format(" and r.deviceCode == \"%s\"", deviceCode));
        }

        // 添加额外过滤条件（可选）
        if (!extraFilters.isEmpty()) {
            String extraFilterStr = extraFilters.stream()
                    .map(f -> " and " + f)
                    .collect(Collectors.joining());
            flux.append(extraFilterStr);
        }
        flux.append(")\n");

        // 聚合操作（可选）
        if (StringUtils.isNotBlank(aggregateFunction)) {
            flux.append(String.format("  |> %s\n", aggregateFunction));
        }

        // 透视操作（可选）
        if (pivot) {
            flux.append("  |> pivot(rowKey: [\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")\n");
        }
        // 新增：时间偏移（UTC -> 北京时间）  <-- 关键改动3
        flux.append(String.format("  |> timeShift(duration: %s)\n", timeShiftDuration));
        if (sortColumn != null) {
            flux.append(String.format("  |> sort(columns: [\"%s\"], desc: %b)\n",
                    sortColumn, sortDesc));
        }
        return flux.toString().trim();
    }

    /**
     * 参数校验逻辑（严格确保查询合法性）
     */
    private void validateParams() {
        // 必选参数非空校验
        Objects.requireNonNull(bucket, "bucket 不能为空");
        Objects.requireNonNull(timeAmount, "timeAmount 不能为空");
        Objects.requireNonNull(timeUnit, "timeUnit 不能为空");
        Objects.requireNonNull(measurement, "measurement 不能为空");

        // 时间量必须大于 0
        if (timeAmount <= 0) {
            throw new IllegalArgumentException("timeAmount 必须大于 0");
        }

        // 时间单位严格校验（仅允许 s/m/h/d/w/M/y）
        if (!TIME_UNIT_PATTERN.matcher(timeUnit).matches()) {
            throw new IllegalArgumentException("timeUnit 必须是 Influx 支持的单位（s/m/h/d/w/M/y）");
        }
        // 校验时间偏移量格式（新增）
        if (!TIME_SHIFT_PATTERN.matcher(timeShiftDuration).matches()) {
            throw new IllegalArgumentException("timeShiftDuration 格式错误（示例：8h、30m、120s）");
        }
    }
}
