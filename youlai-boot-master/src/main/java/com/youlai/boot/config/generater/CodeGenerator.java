//package com.youlai.boot.config.generater;
//
//import com.baomidou.mybatisplus.generator.FastAutoGenerator;
//import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
//
///**
// * @author YanFa * @author: way * @date: 2024/5/21 13:36
// */
//public class CodeGenerator {
//
//    public static void main(String[] args) {
//        FastAutoGenerator.create("jdbc:mysql://192.168.80.216:3306/youlai_boot", "root", "root")
//                .globalConfig(
//                        builder -> {
//                            builder
//                                    .author("way") // 设置作者
//                                    .disableOpenDir() // 禁止打开输出目录
//                                    .outputDir("C:\\Users\\YanFa\\Desktop\\demo\\youlai_boot-master\\youlai-boot-master\\src\\main\\java"); // wh
//                        })
//                .packageConfig(
//                        builder -> {
//                            builder
//                                    .parent("com.youlai.boot") // 设置父包名
//                                    .moduleName("device") // 设置父包模块名
//                                    .controller("controller")
//                                    .service("service")
//                                    .mapper("mapper")
//                                    .entity("model.entity"); // 设置entity包名
//                            //               .pathInfo(
//                            //               Collections.singletonMap(OutputFile.mapperXml, "D://"));
//                            // 设置mapperXml生成路径
//                        })
//                .strategyConfig(
//                        builder -> {
//                            builder
//                                    // 设置生成表名
//                                    .addInclude("device")
//                                    // 开启lombok
//                                    .entityBuilder()
//                                    .enableLombok()
//                                    // 开启属性注解@TableField&&@TableId
//                                    .enableTableFieldAnnotation()
//                                    // 开启restcontroller
//                                    .controllerBuilder()
//                                    .enableRestStyle();
//                        })
//                // 使用Freemarker引擎模板，默认的是Velocity引擎模板
//                .templateEngine(new FreemarkerTemplateEngine())
//                .execute();
//    }
//}
