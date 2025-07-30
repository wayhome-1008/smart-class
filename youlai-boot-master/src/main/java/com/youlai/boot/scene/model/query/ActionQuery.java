package com.youlai.boot.scene.model.query;

import com.youlai.boot.common.base.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 执行器分页查询对象
 *
 * @author way
 * @since 2025-07-30 17:28
 */
@Schema(description ="执行器查询对象")
@Getter
@Setter
public class ActionQuery extends BasePageQuery {

}
