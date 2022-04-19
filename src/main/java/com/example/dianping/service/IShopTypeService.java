package com.example.dianping.service;

import com.example.dianping.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 商户类型相关服务。最后编辑于：2022-4-18。
 * @author yuchen
 */
public interface IShopTypeService extends IService<ShopType> {

    List<ShopType> allTypes();
}
