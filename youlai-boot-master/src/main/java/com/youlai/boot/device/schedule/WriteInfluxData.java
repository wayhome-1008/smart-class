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

    @Scheduled(cron = "0 59 16 * * ?")
    public void writeInfluxData() {
        log.info("[===========定时写入数据============]");
        //1.查询出所有设备最后存在的数据
        Map<Object, Object> deviceMap = redisTemplate.opsForHash().entries(RedisConstants.Device.DEVICE);
        for (Map.Entry<Object, Object> entry : deviceMap.entrySet()) {
            String deviceCode = (String) entry.getKey();
            Device device = (Device) entry.getValue();
            if (device.getDeviceTypeId() == 4 || device.getDeviceTypeId() == 8) {
                log.info("设备 {} 类型为 {}", deviceCode, device.getDeviceTypeId());
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

        // 2. 检查是否存在数据空白期
        List<Instant> gapDays = findGapDays(records);

        if (!gapDays.isEmpty()) {
            log.info("设备 {} 检测到 {} 天的数据空白期", device.getDeviceCode(), gapDays.size());

            // 3. 查找空白期前的最后有效数据
            InfluxMqttPlug lastValidData = findLastValidDataBeforeGap(device, gapDays.get(0));

            if (lastValidData != null) {
                // 4. 填充空白期数据
                fillGapDays(device, lastValidData, gapDays);
            }
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

        String flux = String.format(
                "from(bucket: \"%s\") "
                        + "|> range(start: %s) "
                        + "|> filter(fn: (r) => r._measurement == \"device\"" +
                        " and r.deviceCode == \"%s\" " +
                        "and r.categoryId == \"%s\"" +
                        " and r.roomId == \"%s\"" +
                        " and r._field == \"Total\") " +
                        "|> aggregateWindow(every: 1d, fn: last, createEmpty: true) " +
                        "|> fill(column: \"_value\", value: 0.0) " +
                        "|> sort(columns: [\"_time\"])", BUCKET, rangeStart + "d", device.getDeviceCode(), device.getCategoryId() != null ? device.getCategoryId().toString() : "", device.getDeviceRoom() != null ? device.getDeviceRoom().toString() : "");

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
     * 查找数据空白期（值为0的天，排除当天）
     * @param records 数据记录
     * @return 空白期时间列表
     */
    private List<Instant> findGapDays(List<FluxRecord> records) {
        List<Instant> gapDays = new ArrayList<>();
        // 获取今天的开始时间（00:00:00）
        Instant todayStart = Instant.now().atZone(java.time.ZoneId.systemDefault()).toLocalDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();

        for (FluxRecord record : records) {
            Object value = record.getValueByKey("_value");
            Instant recordTime = record.getTime();

            // 排除当天的数据
            if (recordTime.isBefore(todayStart)) {
                // 如果值为0，认为是空白期（因为我们用0填充了空白时间段）
                if (value instanceof Number && ((Number) value).doubleValue() == 0.0) {
                    gapDays.add(recordTime);
                }
            }
        }

        log.info("检测到 {} 天的数据空白期（排除当天）", gapDays.size());
        return gapDays;
    }

    /**
     * 查找空白期前的最后有效数据
     * @param device 设备信息
     * @param gapStartTime 空白期开始时间
     * @return 最后有效数据点
     */
    private InfluxMqttPlug findLastValidDataBeforeGap(Device device, Instant gapStartTime) {
        QueryApi queryApi = influxDBClient.getQueryApi();

        String flux = String.format(
                "from(bucket: \"%s\") " +
                        "|> range(start: -365d, stop: %s) " +
                        "|> filter(fn: (r) => r._measurement == \"device\" and r.deviceCode == \"%s\" and r.categoryId == \"%s\" and r.roomId == \"%s\" and r._field == \"Total\") " +
                        "|> filter(fn: (r) => exists r._value) " +  // 只获取有值的记录
                        "|> last()",
                BUCKET,
                gapStartTime.toString(),
                device.getDeviceCode(),
                device.getCategoryId() != null ? device.getCategoryId().toString() : "",
                device.getDeviceRoom() != null ? device.getDeviceRoom().toString() : ""
        );

        log.info("设备 {} 查询空白期前最后有效数据的Flux语句: {}", device.getDeviceCode(), flux);
        log.info("设备 {} 空白期开始时间: {}", device.getDeviceCode(), gapStartTime);

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
     * 填充空白期数据（按小时填充）
     * @param device 设备信息
     * @param validData 有效数据点
     * @param gapDays 空白期时间列表
     */
    private void fillGapDays(Device device, InfluxMqttPlug validData, List<Instant> gapDays) {
        WriteApi writeApi = influxDBClient.getWriteApi();
        int totalFilledCount = 0;

        for (Instant gapDay : gapDays) {
            // 为每一天填充24小时的数据
            int filledCount = 0;
            for (int hour = 0; hour < 24; hour++) {
                // 计算每个小时的时间点
                Instant hourTime = gapDay.plusSeconds(hour * 3600L); // 每小时3600秒

                // 创建填充数据
                InfluxMqttPlug fillData = createFillData(validData, hourTime, device);

                // 转换为Point并写入InfluxDB
                Point point = Point.measurement("device")
                        .addTag("deviceCode", fillData.getDeviceCode())
                        .addTag("roomId", fillData.getRoomId())
                        .addTag("deviceType", fillData.getDeviceType())
                        .addTag("categoryId", fillData.getCategoryId())
                        .addField("Total", fillData.getTotal())
                        .time(hourTime, WritePrecision.MS);

//                writeApi.writePoint(BUCKET, ORG, point);
                filledCount++;
                totalFilledCount++;
            }
            log.info("设备 {} 填充空白日期 {} 的 {} 条小时数据并且值为{}", device.getDeviceCode(), gapDay, filledCount, validData.getTotal());
        }
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
