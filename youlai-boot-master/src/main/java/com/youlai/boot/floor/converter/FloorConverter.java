package com.youlai.boot.floor.converter;

import com.youlai.boot.common.model.Option;
import org.mapstruct.Mapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.floor.model.entity.Floor;
import com.youlai.boot.floor.model.form.FloorForm;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

/**
 * 楼层管理对象转换器
 *
 * @author way
 * @since 2025-05-08 12:23
 */
@Mapper(componentModel = "spring")
public interface FloorConverter{

    FloorForm toForm(Floor entity);

    Floor toEntity(FloorForm formData);

    @Mappings({
            @Mapping(target = "value", source = "id"),
            @Mapping(target = "label", source = "floorNumber")
    })
    Option<Long> toOption(Floor entity);

    List<Option<Long>> toOptions(List<Floor> list);
}