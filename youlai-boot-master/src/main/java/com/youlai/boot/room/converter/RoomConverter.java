package com.youlai.boot.room.converter;

import com.youlai.boot.common.model.Option;
import org.mapstruct.Mapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.room.model.entity.Room;
import com.youlai.boot.room.model.form.RoomForm;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

import java.util.List;

/**
 * 房间管理对象转换器
 *
 * @author way
 * @since 2025-05-09 12:09
 */
@Mapper(componentModel = "spring")
public interface RoomConverter{

    RoomForm toForm(Room entity);

    Room toEntity(RoomForm formData);

    @Mappings({
            @Mapping(target = "value", source = "id"),
            @Mapping(target = "label", source = "classroomCode")
    })
    Option<Long> toOption(Room entity);

    List<Option<Long>> toOptions(List<Room> list);
}