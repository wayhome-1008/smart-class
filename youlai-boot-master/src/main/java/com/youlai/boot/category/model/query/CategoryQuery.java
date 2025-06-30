package com.youlai.boot.category.model.query;

import com.youlai.boot.common.base.BasePageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 分类管理分页查询对象
 *
 * @author way
 * @since 2025-06-30 18:52
 */
@Schema(description ="分类管理查询对象")
@Getter
@Setter
public class CategoryQuery extends BasePageQuery {

    private String categoryName;
}
