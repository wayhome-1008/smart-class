package com.youlai.boot.scene.model.query;

import com.youlai.boot.common.base.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 触发器分页查询对象
 *
 * @author way
 * @since 2025-07-30 17:25
 */
@Schema(description ="触发器查询对象")
@Getter
@Setter
public class TriggerQuery extends BasePageQuery {

}
