package com.youlai.boot.device.schedule;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.youlai.boot.common.constant.RedisConstants;
import com.youlai.boot.device.model.entity.Device;
import com.youlai.boot.device.model.influx.InfluxMqttPlug;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *@Author: way
 *@CreateTime: 2025-10-10  09:56
 *@Description: 定时填充InfluxDB空白数据
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WriteInfluxData {
    private final InfluxDBClient influxDBClient;
    private final RedisTemplate<String, Object> redisTemplate;
    // InfluxDB数据库和保留策略
    private static final String BUCKET = "smartClass";
    private static final String ORG = "zjtc";

    @Scheduled(cron = "0 59 11 * * ?")
    public void writeInfluxData() {
        log.info("[===========定时写入数据============]");
        //1.查询出所有设备最后存在的数据
        Map<Object, Object> deviceMap = redisTemplate.opsForHash().entries(RedisConstants.Device.DEVICE);
        for (Map.Entry<Object, Object> entry : deviceMap.entrySet()) {
            Device device = (Device) entry.getValue();
            if (device.getDeviceTypeId() == 4 || device.getDeviceTypeId() == 8) {
                // 检测并填充数据空白期
                detectAndFillDataGaps(device);
            }
        }
    }

    /**
     * 检测并填充设备数据空白期
     * @param device 设备信息
     */
    private void detectAndFillDataGaps(Device device) {
        // 1. 查询最近7天的数据（包括空白时间段）
        List<FluxRecord> records = queryDeviceDataWithGaps(device, -30);

        // 2. 检查是否存在数据空白期区间
        List<GapPeriod> gapPeriods = findGapPeriods(records);

        if (!gapPeriods.isEmpty()) {
            for (GapPeriod gapPeriod : gapPeriods) {
                log.info("设备 {} 检测到数据空白期：{} - {}", device.getDeviceCode(), gapPeriod.getStart(), gapPeriod.getEnd());
                // 3. 查找每个空白期前的最后有效数据
                InfluxMqttPlug lastValidData = findLastValidDataBeforeGap(device, gapPeriod.getStart());
                log.info("设备 {} 找到最后有效数据：{}", device.getDeviceCode(), lastValidData);
                if (lastValidData != null) {
                    // 4. 填充空白期数据
                    fillGapPeriod(device, lastValidData, gapPeriod);
                }
            }
        } else {
            log.info("设备 {} 没有数据空白期", device.getDeviceCode());
        }
    }

    /**
     * 查询设备数据（包括空白时间段）
     * @param device 设备信息
     * @param rangeStart 查询起始时间（天）
     * @return 数据记录列表
     */
    private List<FluxRecord> queryDeviceDataWithGaps(Device device, Integer rangeStart) {
        QueryApi queryApi = influxDBClient.getQueryApi();

        // 计算绝对时间范围
        Instant stopTime = Instant.now();
        Instant startTime = stopTime.plusSeconds(rangeStart * 24 * 3600L); // rangeStart为负数，表示往前推几天

        String startTimestamp = startTime.toString();
        String stopTimestamp = stopTime.toString();

        String flux = String.format(
                "from(bucket: \"%s\") "
                        + "|> range(start: %s, stop: %s) "
                        + "|> filter(fn: (r) => r._measurement == \"device\"" +
                        " and r.deviceCode == \"%s\" " +
                        "and r.categoryId == \"%s\"" +
                        " and r.roomId == \"%s\"" +
                        " and r._field == \"Total\") " +
                        "|> aggregateWindow(every: 1h, fn: last, createEmpty: true) " +
                        "|> fill(column: \"_value\", value: 0.0) " +
                        "|> sort(columns: [\"_time\"])",
                BUCKET,
                startTimestamp,
                stopTimestamp,
                device.getDeviceCode(),
                device.getCategoryId() != null ? device.getCategoryId().toString() : "",
                device.getDeviceRoom() != null ? device.getDeviceRoom().toString() : "");

        log.info("查询设备 {} 数据的Flux语句: {}", device.getDeviceCode(), flux);

        List<FluxTable> tables = queryApi.query(flux, ORG);
        if (!tables.isEmpty()) {
            //将返回的数据0索引打印日志
            log.info("查询设备 {} 数据: {}", device.getDeviceCode(), tables.get(0).getRecords().get(0));
            return tables.get(0).getRecords();
        }
        return new ArrayList<>();
    }


    /**
     * 查找数据空白期区间（值为0的天，排除当天）
     *
     * @param records 数据记录
     * @return 空白期区间列表
     */
    private List<GapPeriod> findGapPeriods(List<FluxRecord> records) {
        List<GapPeriod> gapPeriods = new ArrayList<>();
        // 获取今天的开始时间（00:00:00）
        Instant todayStart = Instant.now().atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();

        Instant periodStart = null;
        for (FluxRecord record : records) {
            Object value = record.getValueByKey("_value");
            Instant recordTime = record.getTime();

            // 排除当天的数据
            // 如果值为0，认为是空白期
            if (value instanceof Number && ((Number) value).doubleValue() == 0) {
                if (periodStart == null) {
                    periodStart = recordTime;
                }
            } else {
                if (periodStart != null) {
                    GapPeriod gapPeriod = new GapPeriod(periodStart, recordTime);
                    gapPeriods.add(gapPeriod);
                    periodStart = null;
                }
            }
        }

        // 处理最后一个空白期直到今天的情况
        if (periodStart != null) {
            GapPeriod gapPeriod = new GapPeriod(periodStart, todayStart);
            gapPeriods.add(gapPeriod);
            log.info("检测到空白期区间: {} 至 {}", gapPeriod.getStart(), gapPeriod.getEnd());
        }

        if (gapPeriods.isEmpty()) {
            log.info("未检测到数据空白期区间");
        } else {
            gapPeriods.forEach(gapPeriod -> log.info("检测到空白期区间: {} 至 {}", gapPeriod.getStart(), gapPeriod.getEnd()));
        }

        return gapPeriods;
    }


    private InfluxMqttPlug findLastValidDataBeforeGap(Device device, Instant gapStartTime) {
        QueryApi queryApi = influxDBClient.getQueryApi();

        String flux = String.format(
                "from(bucket: \"%s\") " +
                        "|> range(start: -365d, stop: %s) " +
                        "|> filter(fn: (r) => r._measurement == \"device\" and r.deviceCode == \"%s\" and r.categoryId == \"%s\" and r.roomId == \"%s\" and r._field == \"Total\") " +
                        "|> filter(fn: (r) => exists r._value and r._value != 0.0) " +  // 过滤掉值为0的记录
                        "|> last()",
                BUCKET,
                gapStartTime.toString(),
                device.getDeviceCode(),
                device.getCategoryId() != null ? device.getCategoryId().toString() : "",
                device.getDeviceRoom() != null ? device.getDeviceRoom().toString() : ""
        );

        log.info("设备 {} 查询空白期前最后有效数据的Flux语句: {}", device.getDeviceCode(), flux);
        List<FluxTable> tables = queryApi.query(flux, ORG);

        if (!tables.isEmpty() && !tables.get(0).getRecords().isEmpty()) {
            FluxRecord record = tables.get(0).getRecords().get(0);
            InfluxMqttPlug plug = new InfluxMqttPlug();
            plug.setDeviceCode(device.getDeviceCode());
            plug.setTime(record.getTime());
            plug.setTotal(getDoubleValue(record, "_value"));
            log.info("设备 {} 找到最后有效数据: 时间={}, Total值={}",
                    device.getDeviceCode(), record.getTime(), plug.getTotal());
            return plug;
        } else {
            log.info("设备 {} 在空白期前未找到有效数据", device.getDeviceCode());
            return null;
        }
    }


    /**
     * 填充空白期数据（按整点和半点填充）
     *
     * @param device      设备信息
     * @param validData   有效数据点
     * @param gapPeriod   空白期时间段
     */
    private void fillGapPeriod(Device device, InfluxMqttPlug validData, GapPeriod gapPeriod) {
        WriteApi writeApi = influxDBClient.getWriteApi();
        int totalFilledCount = 0;

        // 获取当前时间，用于避免写入未来数据
        Instant now = Instant.now();

        // 从空白期开始时间到结束时间，按小时遍历
        Instant currentTime = gapPeriod.getStart();
        while (!currentTime.isAfter(gapPeriod.getEnd())) {
            // 跳过未来时间点的数据写入
            if (currentTime.isAfter(now)) {
                log.info("设备 {} 跳过未来时间点 {} 的数据填充", device.getDeviceCode(), currentTime);
                break;
            }
            // 填充整点数据
            InfluxMqttPlug fillDataHour = createFillData(validData, currentTime, device);
            Point pointHour = Point.measurement("device")
                    .addTag("deviceCode", fillDataHour.getDeviceCode())
                    .addTag("roomId", fillDataHour.getRoomId())
                    .addTag("deviceType", fillDataHour.getDeviceType())
                    .addTag("categoryId", fillDataHour.getCategoryId())
                    .addField("Total", fillDataHour.getTotal())
                    .time(currentTime, WritePrecision.MS);
//        writeApi.writePoint(BUCKET, ORG, pointHour);
            totalFilledCount++;
            log.info("填充设备{}编号{}整点数据{}", device.getDeviceName(), device.getDeviceCode(), fillDataHour);

            // 填充半点数据（当前时间+30分钟）
            Instant halfHourTime = currentTime.plusSeconds(30 * 60L);
            if (!halfHourTime.isAfter(gapPeriod.getEnd()) && !halfHourTime.isAfter(now)) {
                InfluxMqttPlug fillDataHalfHour = createFillData(validData, halfHourTime, device);
                Point pointHalfHour = Point.measurement("device")
                        .addTag("deviceCode", fillDataHalfHour.getDeviceCode())
                        .addTag("roomId", fillDataHalfHour.getRoomId())
                        .addTag("deviceType", fillDataHalfHour.getDeviceType())
                        .addTag("categoryId", fillDataHalfHour.getCategoryId())
                        .addField("Total", fillDataHalfHour.getTotal())
                        .time(halfHourTime, WritePrecision.MS);
                writeApi.writePoint(BUCKET, ORG, pointHalfHour);
                totalFilledCount++;
                log.info("填充设备{}编号{}半点数据{}", device.getDeviceName(), device.getDeviceCode(), fillDataHalfHour);
            }

            // 移动到下一个整点（+1小时）
            currentTime = currentTime.plusSeconds(60 * 60L);
        }

        log.info("设备 {} 总共填充了 {} 条空白数据", device.getDeviceCode(), totalFilledCount);
        writeApi.close();
    }


    /**
     * 从记录中获取Double类型的值
     */
    private Double getDoubleValue(FluxRecord record, String fieldName) {
        Object value = record.getValueByKey(fieldName);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }

    /**
     * 创建填充数据
     */
    private InfluxMqttPlug createFillData(InfluxMqttPlug lastDataPoint, Instant fillTime, Device device) {
        InfluxMqttPlug fillData = new InfluxMqttPlug();

        // 复制最后一条记录的数据
        fillData.setDeviceCode(lastDataPoint.getDeviceCode());
        fillData.setRoomId(device.getDeviceRoom() != null ? device.getDeviceRoom().toString() : null);
        fillData.setDeviceType(device.getDeviceTypeId() != null ? device.getDeviceTypeId().toString() : null);
        fillData.setCategoryId(device.getCategoryId() != null ? device.getCategoryId().toString() : null);

        // 复制数值字段
        fillData.setTotal(lastDataPoint.getTotal());
        // 设置时间
        fillData.setTime(fillTime);

        return fillData;
    }
}
