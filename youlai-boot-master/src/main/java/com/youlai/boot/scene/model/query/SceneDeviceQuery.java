package com.youlai.boot.scene.model.query;

import com.youlai.boot.common.base.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 场景设备分页查询对象
 *
 * @author way
 * @since 2025-07-29 12:03
 */
@Schema(description ="场景设备查询对象")
@Getter
@Setter
public class SceneDeviceQuery extends BasePageQuery {

}
