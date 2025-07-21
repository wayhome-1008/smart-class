package com.youlai.boot.system.model.query;

import com.youlai.boot.common.base.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

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

}
