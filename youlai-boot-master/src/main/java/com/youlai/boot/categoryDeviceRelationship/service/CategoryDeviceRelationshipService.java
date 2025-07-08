package com.youlai.boot.categoryDeviceRelationship.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.youlai.boot.categoryDeviceRelationship.model.CategoryDeviceRelationship;

/**
 * @author Way
 */
public interface CategoryDeviceRelationshipService extends IService<CategoryDeviceRelationship> {

    CategoryDeviceRelationship getByDeviceId(Long id);
}
