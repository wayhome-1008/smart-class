package com.youlai.boot.scene.model.entity;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableName;
import com.youlai.boot.common.base.BaseEntity;

/**
 * 规则引擎脚本实体对象
 *
 * @author way
 * @since 2025-07-29 11:54
 */
@Getter
@Setter
@TableName("rule_script")
public class RuleScript extends BaseEntity{
    private String scriptId;

    /**
     * 分类ID
     */
    private Long categoryId;
    /**
     * 分类名称
     */
    private String categoryName;
    /**
     * 场景ID
     */
    private Long sceneId;
    /**
     * 脚本名称
     */
    private String scriptName;
    /**
     * 脚本数据
     */
    private String scriptData;
    /**
     * 应用名称
     */
    private String applicationName;
    /**
     * 脚本类型:script=普通脚本，switch_script=选择脚本,if_script=条件脚本，for_script=数量循环脚本，while_script=条件循环，break_script=退出循环脚本
     */
    private String scriptType;
    /**
     * 脚本事件类型(1=设备上报，2=平台下发，3=设备上线，4=设备离线)
     */
    private Integer scriptEvent;
    /**
     * 脚本动作(1=消息重发，2=消息通知，3=Http推送，4=Mqtt桥接，5=数据库存储)
     */
    private Integer scriptAction;
    /**
     * 脚本用途(1=数据流，2=触发器，3=执行动作)
     */
    private Integer scriptPurpose;
    /**
     * 脚本执行顺序，值越大优先级越高
     */
    private Integer scriptOrder;
    /**
     * 脚本语言（groovy | qlexpress | js | python | lua | aviator）
     */
    private String scriptLanguage;
    /**
     * 是否生效
     */
    private Integer enable;
    /**
     * 创建者
     */
    private String createBy;
    /**
     * 更新者
     */
    private String updateBy;
    /**
     * 备注
     */
    private String remark;
}
