package com.example.dianping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.dianping.dto.Result;
import com.example.dianping.dto.UserDTO;
import com.example.dianping.entity.Follow;
import com.example.dianping.mapper.FollowMapper;
import com.example.dianping.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dianping.service.IUserService;
import com.example.dianping.utils.RedisConstants;
import com.example.dianping.utils.UserHolder;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@AllArgsConstructor
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    private final StringRedisTemplate redisTemplate;

    private final IUserService userService;

    @Override
    public Result followed(Long userId) {
        if (UserHolder.getUser() == null) {
            return Result.ok(false);
        }
        return Result.ok(followed(UserHolder.getUser().getId(), userId));
    }


    @Override
    public Result follow(Long targetId) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }
        Long userId = user.getId();
        if (followed(userId, targetId)) {
            return Result.fail("您已经关注了该用户");
        } else {
            boolean result = save(new Follow(userId, targetId));
            if (result) {
                log.debug("用户 {} 关注了用户 {}", userId, targetId);
                redisTemplate.opsForSet().add(RedisConstants.FOLLOWING_SET_KEY + userId.toString(), targetId.toString());      // 把关注记录同样缓存至 Redis，用于后续查询共同关注时使用
                return Result.ok("关注成功");
            } else {
                return Result.fail("关注失败");
            }
        }

    }

    @Override
    public Result unfollow(Long targetId) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }
        Long userId = user.getId();
        if (followed(userId, targetId)) {
            boolean result = remove(
                    new QueryWrapper<Follow>()
                            .eq("user_id", userId)
                            .eq("followed_user_id", targetId));
            if (result) {
                log.debug("用户 {} 取关了用户 {}", userId, targetId);
                redisTemplate.opsForSet().remove(RedisConstants.FOLLOWING_SET_KEY + userId.toString(), targetId.toString());
                return Result.ok("取关成功");
            } else {
                return Result.fail("取关失败");
            }
        } else {
            return Result.fail("您未关注该用户");
        }
    }

    private boolean followed(Long subjectId, Long objectId) {
        return query().eq("user_id", subjectId)
                .eq("followed_user_id", objectId)
                .exists();
    }

    @Override
    public Result commonFollowing(Long objectId) {
        Long subjectId = UserHolder.getUser().getId();
        Set<String> intersect = redisTemplate.opsForSet()
                .intersect(RedisConstants.FOLLOWING_SET_KEY + subjectId.toString(),
                        RedisConstants.FOLLOWING_SET_KEY + objectId.toString());
        if (intersect == null || intersect.size() == 0) {
            return Result.ok();
        }
        List<UserDTO> common = intersect.stream()
                .map(Long::valueOf)
                .map(userService::getById)
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(common);
    }
}
