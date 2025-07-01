//package com.youlai.boot.device.model.influx;
//
//import com.influxdb.annotations.Column;
//import com.influxdb.annotations.Measurement;
//import lombok.Data;
//
//import java.time.Instant;
//
///**
// *@Author: way
// *@CreateTime: 2025-06-09  10:48
// *@Description: TODO
// */
//@Data
//@Measurement(name = "device")
//public class InfluxPlug {
//
//    @Column(tag = true)
//    private String deviceCode;
//
//    @Column(tag = true)
//    private Long roomId;
//
//    @Column(name = "activePowerA")
//    private Double activePowerA;
//
////    @Column(name = "activePowerB")
////    private Integer activePowerB;
////
////    @Column(name = "activePowerC")
////    private Integer activePowerC;
//
//    @Column(name = "RMS_VoltageA")
//    private Double RMS_VoltageA;
//
////    @Column(name = "RMS_VoltageB")
////    private Integer RMS_VoltageB;
////
////    @Column(name = "RMS_VoltageC")
////    private Integer RMS_VoltageC;
//
//    @Column(name = "RMS_CurrentA")
//    private Double RMS_CurrentA;
//
////    @Column(name = "RMS_CurrentB")
////    private Integer RMS_CurrentB;
////
////    @Column(name = "RMS_CurrentC")
////    private Integer RMS_CurrentC;
//
//    @Column(name = "electricalEnergy")
//    private Double electricalEnergy;
//
//    @Column(timestamp = true)
//    private Instant time;
//}
