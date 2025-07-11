package com.youlai.boot.categoryDeviceRelationship.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.youlai.boot.categoryDeviceRelationship.model.CategoryDeviceRelationship;

import java.util.List;

/**
 * @author Way
 */
public interface CategoryDeviceRelationshipService extends IService<CategoryDeviceRelationship> {

    CategoryDeviceRelationship getByDeviceId(Long id);

    List<CategoryDeviceRelationship> listByCategoryId(Long categoryId);
}
