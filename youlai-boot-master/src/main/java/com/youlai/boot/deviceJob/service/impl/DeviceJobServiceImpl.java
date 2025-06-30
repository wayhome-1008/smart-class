package com.youlai.boot.deviceJob.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.youlai.boot.deviceJob.mapper.DeviceJobMapper;
import com.youlai.boot.deviceJob.service.DeviceJobService;
import com.youlai.boot.deviceJob.model.entity.DeviceJob;
import com.youlai.boot.deviceJob.model.form.DeviceJobForm;
import com.youlai.boot.deviceJob.model.query.DeviceJobQuery;
import com.youlai.boot.deviceJob.model.vo.DeviceJobVO;
import com.youlai.boot.deviceJob.converter.DeviceJobConverter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;

/**
 * 任务管理服务实现类
 *
 * @author way
 * @since 2025-06-30 18:11
 */
@Service
@RequiredArgsConstructor
public class DeviceJobServiceImpl extends ServiceImpl<DeviceJobMapper, DeviceJob> implements DeviceJobService {

    private final DeviceJobConverter deviceJobConverter;

    /**
    * 获取任务管理分页列表
    *
    * @param queryParams 查询参数
    * @return {@link IPage<DeviceJobVO>} 任务管理分页列表
    */
    @Override
    public IPage<DeviceJobVO> getDeviceJobPage(DeviceJobQuery queryParams) {
        Page<DeviceJobVO> pageVO = this.baseMapper.getDeviceJobPage(
                new Page<>(queryParams.getPageNum(), queryParams.getPageSize()),
                queryParams
        );
        return pageVO;
    }
    
    /**
     * 获取任务管理表单数据
     *
     * @param id 任务管理ID
     * @return 任务管理表单数据
     */
    @Override
    public DeviceJobForm getDeviceJobFormData(Long id) {
        DeviceJob entity = this.getById(id);
        return deviceJobConverter.toForm(entity);
    }
    
    /**
     * 新增任务管理
     *
     * @param formData 任务管理表单对象
     * @return 是否新增成功
     */
    @Override
    public boolean saveDeviceJob(DeviceJobForm formData) {
        DeviceJob entity = deviceJobConverter.toEntity(formData);
        return this.save(entity);
    }
    
    /**
     * 更新任务管理
     *
     * @param id   任务管理ID
     * @param formData 任务管理表单对象
     * @return 是否修改成功
     */
    @Override
    public boolean updateDeviceJob(Long id,DeviceJobForm formData) {
        DeviceJob entity = deviceJobConverter.toEntity(formData);
        return this.updateById(entity);
    }
    
    /**
     * 删除任务管理
     *
     * @param ids 任务管理ID，多个以英文逗号(,)分割
     * @return 是否删除成功
     */
    @Override
    public boolean deleteDeviceJobs(String ids) {
        Assert.isTrue(StrUtil.isNotBlank(ids), "删除的任务管理数据为空");
        // 逻辑删除
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(Long::parseLong)
                .toList();
        return this.removeByIds(idList);
    }

}
