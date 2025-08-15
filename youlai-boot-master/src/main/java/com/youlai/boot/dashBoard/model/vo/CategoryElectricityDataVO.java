package com.youlai.boot.dashBoard.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 *@Author: way
 *@CreateTime: 2025-08-14  16:08
 *@Description: TODO
 */
@Data
@Schema(description = "分类用电量信息")
public class CategoryElectricityDataVO {

    @Schema(description = "分类ID")
    private Long categoryId;

    @Schema(description = "分类名称")
    private String categoryName;

    @Schema(description = "该分类用电量")
    private Double categoryElectricity;
}
