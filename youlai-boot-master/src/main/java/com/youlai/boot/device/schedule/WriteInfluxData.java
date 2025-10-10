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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

    @Scheduled(cron = "0 59 16 * * ?") //
    public void writeInfluxData() {
        log.info("[===========定时写入数据============]");
        //1.查询出所有设备最后存在的数据
        Map<Object, Object> deviceMap = redisTemplate.opsForHash().entries(RedisConstants.Device.DEVICE);
        for (Map.Entry<Object, Object> entry : deviceMap.entrySet()) {
            String deviceCode = (String) entry.getKey();
            Device device = (Device) entry.getValue();
            if (device.getDeviceTypeId() == 4 || device.getDeviceTypeId() == 8) {
                log.info("设备 {} 类型为 {}", deviceCode, device.getDeviceType());
                // 2.查询该设备在InfluxDB中的最后一条有效数据
                InfluxMqttPlug lastDataPoint = getLastDataPointFromInfluxDB(deviceCode, -3);
                if (lastDataPoint != null) {
                    // 3.填充空白时间段的数据
                    fillMissingData(deviceCode, lastDataPoint, device);
                }
            }
        }
    }

    /**
     * 从InfluxDB查询设备最后一条有效数据（使用天级窗口聚合）
     * @param deviceCode 设备编码
     * @param rangeStart 查询起始时间范围(天数)
     * @return 最后一条数据点
     */
    private InfluxMqttPlug getLastDataPointFromInfluxDB(String deviceCode, Integer rangeStart) {
        QueryApi queryApi = influxDBClient.getQueryApi();

        // 使用窗口聚合按天分组，获取每天最后一条记录，然后取整体最后一条
        String flux = String.format(
                "from(bucket: \"%s\") " +
                        "|> range(start: %s) " +
                        "|> filter(fn: (r) => r[\"_measurement\"] == \"device\") " +
                        "|> filter(fn: (r) => r[\"deviceCode\"] == \"%s\") " +
                        "|> aggregateWindow(every: 1d, fn: last) " +
                        "|> last()",
                BUCKET, rangeStart + "d", deviceCode
        );
        log.info("查询语句：{}", flux);
        List<FluxTable> tables = queryApi.query(flux, ORG);
        if (!tables.isEmpty() && !tables.get(0).getRecords().isEmpty()) {
            FluxRecord record = tables.get(0).getRecords().get(0);
            if (record.getValueByKey("_value") != null) {
                // 构建InfluxMqttPlug对象
                InfluxMqttPlug plug = new InfluxMqttPlug();
                plug.setDeviceCode(deviceCode);
                plug.setTime(record.getTime());
                // 设置各个字段值
                plug.setTotal(getDoubleValue(record, "Total"));
                return plug;
            } else {
                // 如果没有找到有效的数据，则进行递归查找
                // 限制递归深度，避免无限递归
                if (rangeStart > -365) { // 最多查询一年数据
                    return getLastDataPointFromInfluxDB(deviceCode, rangeStart - 1); // 每次增加1天查询范围
                } else {
                    return null; // 超过一年未找到数据则返回null
                }
            }
        }
        return null;
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
     * 从记录中获取Integer类型的值
     */
    private Integer getIntegerValue(FluxRecord record, String fieldName) {
        Object value = record.getValueByKey(fieldName);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }

    /**
     * 从记录中获取String类型的值
     */
    private String getStringValue(FluxRecord record, String fieldName) {
        Object value = record.getValueByKey(fieldName);
        return value != null ? value.toString() : null;
    }

    /**
     * 填充设备的空白数据
     * @param deviceCode 设备编码
     * @param lastDataPoint 最后一条有效数据
     * @param device 设备信息
     */
    private void fillMissingData(String deviceCode, InfluxMqttPlug lastDataPoint, Device device) {
        Instant lastTime = lastDataPoint.getTime();
        Instant now = Instant.now();

        // 计算需要填充的小时数
        long hoursBetween = ChronoUnit.HOURS.between(lastTime, now);

        if (hoursBetween > 1) {
            log.info("设备 {} 需要填充 {} 小时的数据", deviceCode, hoursBetween);

            WriteApi writeApi = influxDBClient.getWriteApi();

            // 按小时间隔填充数据
            for (int i = 1; i <= hoursBetween; i++) {
                Instant fillTime = lastTime.plus(i, ChronoUnit.HOURS);

                // 创建新的InfluxMqttPlug对象，使用最后一条记录的数据填充
                InfluxMqttPlug fillData = createFillData(lastDataPoint, fillTime, device);

                // 转换为Point并写入InfluxDB
                Point point = Point.measurement("device")
                        .addTag("deviceCode", fillData.getDeviceCode())
                        .addTag("roomId", fillData.getRoomId())
                        .addTag("deviceType", fillData.getDeviceType())
                        .addTag("categoryId", fillData.getCategoryId())
                        .addField("Total", fillData.getTotal())
                        .time(fillTime, WritePrecision.MS);

                writeApi.writePoint(BUCKET, ORG, point);

                // 控制写入频率，避免对InfluxDB造成过大压力
                try {
                    TimeUnit.MILLISECONDS.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            writeApi.close();
            log.info("设备 {} 成功填充 {} 条数据", deviceCode, hoursBetween);
        }
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
