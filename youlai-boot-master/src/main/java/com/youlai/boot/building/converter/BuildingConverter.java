package com.youlai.boot.building.converter;

import com.youlai.boot.common.model.Option;
import org.mapstruct.Mapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.building.model.entity.Building;
import com.youlai.boot.building.model.form.BuildingForm;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

/**
 * 教学楼管理对象转换器
 *
 * @author way
 * @since 2025-05-08 14:00
 */
@Mapper(componentModel = "spring")
public interface BuildingConverter {

    BuildingForm toForm(Building entity);

    Building toEntity(BuildingForm formData);


    @Mappings({
            @Mapping(target = "value", source = "id"),
            @Mapping(target = "label", source = "buildingName")
    })
    Option<Long> toOption(Building entity);

    List<Option<Long>> toOptions(List<Building> list);
}