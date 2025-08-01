package com.youlai.boot.scene.model.query;

import com.youlai.boot.common.base.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 场景脚本分页查询对象
 *
 * @author way
 * @since 2025-07-29 11:58
 */
@Schema(description ="场景脚本查询对象")
@Getter
@Setter
public class SceneScriptQuery extends BasePageQuery {

}
