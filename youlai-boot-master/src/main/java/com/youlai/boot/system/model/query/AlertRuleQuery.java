package com.youlai.boot.system.model.query;

import com.youlai.boot.common.base.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 报警配置分页查询对象
 *
 * @author way
 * @since 2025-07-21 11:22
 */
@Schema(description ="报警配置查询对象")
@Getter
@Setter
public class AlertRuleQuery extends BasePageQuery {
    @Schema(description = "规则名称")
    private String ruleName;

    @Schema(description = "触发报警的设备 ID")
    private Long deviceId;
    @Schema(description = "报警级别（继承规则的 level）")
    private Integer level;
    @Schema(description = "事件状态（0 - 未处理，1 - 已处理，2 - 已忽略）")
    private String status;

}
