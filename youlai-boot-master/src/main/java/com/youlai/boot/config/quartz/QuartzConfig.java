package com.youlai.boot.config.quartz;

import org.quartz.Scheduler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * @Author: way
 * @CreateTime: 2025-07-01  09:53
 * @Description: 配置Quartz属性
 */
@Configuration
public class QuartzConfig {

    // 如果需要使用数据源（如JdbcJobStore），取消注释并注入DataSource
    // private final DataSource dataSource;
    //
    // public QuartzConfig(DataSource dataSource) {
    //     this.dataSource = dataSource;
    // }

    /**
     * 配置SchedulerFactoryBean
     * 设置Quartz属性并创建调度器工厂
     */
    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() {
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        factory.setQuartzProperties(quartzProperties());
        factory.setWaitForJobsToCompleteOnShutdown(true); // 应用关闭时等待任务完成
        factory.setAutoStartup(true); // 自动启动调度器
        factory.setStartupDelay(2); // 延迟2秒启动，确保其他组件先初始化

        // 如果使用数据源，取消注释以下行
        // factory.setDataSource(dataSource);

        return factory;
    }

    /**
     * 配置Quartz属性
     * 设置使用内存存储方案(RAMJobStore)
     */
    @Bean
    public Properties quartzProperties() {
        Properties properties = new Properties();
        properties.setProperty("org.quartz.scheduler.instanceName", "DeviceJobScheduler");
        properties.setProperty("org.quartz.scheduler.instanceId", "AUTO");
        properties.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore"); // 内存存储
        properties.setProperty("org.quartz.threadPool.threadCount", "10"); // 线程池大小
        properties.setProperty("org.quartz.threadPool.threadPriority", "5");
        return properties;
    }

    /**
     * 获取调度器实例
     */
    @Bean(name = "scheduler")
    public Scheduler scheduler(SchedulerFactoryBean factory) throws Exception {
        return factory.getScheduler();
    }

    // 移除QuartzInitializerListener Bean，避免与Spring管理的Quartz冲突
    // @Bean
    // public QuartzInitializerListener executorListener() {
    //     return new QuartzInitializerListener();
    // }
}