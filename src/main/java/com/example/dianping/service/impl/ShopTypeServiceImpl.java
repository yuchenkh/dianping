package com.example.dianping.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.example.dianping.entity.ShopType;
import com.example.dianping.mapper.ShopTypeMapper;
import com.example.dianping.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.example.dianping.utils.RedisConstants.SHOP_TYPES_KEY;

/**
 * 商户类型相关服务实现类。最后编辑于：2022-4-18。
 * @author yuchen
 */
@Service
@AllArgsConstructor
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 获取所有商户类型。
     * @return  一个代表所有商户类型的 {@link ShopType} 的列表
     */
    @Override
    public List<ShopType> allTypes() {
        // 先去 Redis 中查询。商户类型一般都是一起查，不会单独查询某一个类型。
        String json = redisTemplate.opsForValue().get(SHOP_TYPES_KEY);
        if (StrUtil.isNotEmpty(json)) {
            log.debug("商户类型信息在 Redis 中命中。");
            return JSONUtil.toList(json, ShopType.class);
        }
        // Redis 中没有缓存，则去 MySQL 中查询
        List<ShopType> list = list();
        log.debug("商户类型信息从 MySQL 中查询，并写入 Redis。");
        redisTemplate.opsForValue().set(SHOP_TYPES_KEY, JSONUtil.toJsonStr(list));

        return list;
    }
}
