package com.example.dianping.service;

import com.example.dianping.dto.Result;
import com.example.dianping.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 商户相关服务。最后编辑于：2022-4-19。
 * @author yuchen
 */
public interface IShopService extends IService<Shop> {

    Shop shopById(long id);

    Result updateShop(Shop shop);

}
