package com.youlai.boot.alertEvent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.youlai.boot.alertEvent.model.entity.AlertEvent;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.youlai.boot.alertEvent.model.query.AlertEventQuery;
import com.youlai.boot.alertEvent.model.vo.AlertEventVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 报警记录Mapper接口
 *
 * @author way
 * @since 2025-07-21 12:16
 */
@Mapper
public interface AlertEventMapper extends BaseMapper<AlertEvent> {

    /**
     * 获取报警记录分页数据
     *
     * @param page 分页对象
     * @param queryParams 查询参数
     * @return {@link Page<AlertEventVO>} 报警记录分页列表
     */
    Page<AlertEventVO> getAlertEventPage(Page<AlertEventVO> page, AlertEventQuery queryParams);

}
