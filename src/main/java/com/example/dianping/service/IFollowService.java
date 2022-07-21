package com.example.dianping.service;

import com.example.dianping.dto.Result;
import com.example.dianping.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IFollowService extends IService<Follow> {

    Result followed(Long userId);

    Result follow(Long userId);

    Result unfollow(Long userId);

    Result commonFollowing(Long objectId);
}
