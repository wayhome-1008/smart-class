//package com.youlai.boot.categoryDeviceRelationship.service.impl;
//
//import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
//import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
//import com.youlai.boot.categoryDeviceRelationship.mapper.CategoryDeviceRelationshipMapper;
//import com.youlai.boot.categoryDeviceRelationship.model.CategoryDeviceRelationship;
//import com.youlai.boot.categoryDeviceRelationship.service.CategoryDeviceRelationshipService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.util.List;
//
///**
// * @author Way
// */
//@Service
//@RequiredArgsConstructor
//public class CategoryDeviceRelationshipServiceImpl extends ServiceImpl<CategoryDeviceRelationshipMapper, CategoryDeviceRelationship>
//        implements CategoryDeviceRelationshipService {
//    private final CategoryDeviceRelationshipMapper categoryDeviceRelationshipMapper;
//
//    @Override
//    public CategoryDeviceRelationship getByDeviceId(Long id) {
//        return categoryDeviceRelationshipMapper.selectOne(new LambdaQueryWrapper<CategoryDeviceRelationship>().eq(CategoryDeviceRelationship::getDeviceId, id));
//    }
//
//    @Override
//    public List<CategoryDeviceRelationship> listByCategoryId(Long categoryId) {
//        return this.categoryDeviceRelationshipMapper.selectList(new LambdaQueryWrapper<CategoryDeviceRelationship>().eq(CategoryDeviceRelationship::getCategoryId, categoryId));
//    }
//
//    @Override
//    public List<CategoryDeviceRelationship> listByDeviceIds(List<Long> deviceIds) {
//        return this.list(new LambdaQueryWrapper<CategoryDeviceRelationship>().in(CategoryDeviceRelationship::getDeviceId, deviceIds));
//    }
//}
//
//
//
//
