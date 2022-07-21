package com.example.dianping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dianping.dto.Result;
import com.example.dianping.dto.UserDTO;
import com.example.dianping.entity.Blog;
import com.example.dianping.entity.User;
import com.example.dianping.mapper.BlogMapper;
import com.example.dianping.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dianping.service.IUserService;
import com.example.dianping.utils.RedisConstants;
import com.example.dianping.utils.SystemConstants;
import com.example.dianping.utils.UserHolder;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 笔记相关服务类。最后编辑于：2022-6-29。
 * @author yuchen
 */
@Service
@AllArgsConstructor
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    private final IUserService userService;

    private final StringRedisTemplate redisTemplate;

    @Override
    public Result getBlog(long id) {
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("笔记不存在");
        }
        Long authorId = blog.getUserId();
        User author = userService.getById(authorId);      // 这篇笔记的作者
        blog.setIcon(author.getIcon());
        blog.setAuthorName(author.getNickName());
        UserDTO user = UserHolder.getUser();
        if (user != null) {
            Long userId = user.getId();     // 正在查看该日记的用户
            blog.setLikedByMe(likedByMe(id, userId));
        }

        return Result.ok(blog);
    }

    @Override
    public Result queryHotBlogs(Integer current) {
        Page<Blog> page = query()
                .orderByDesc("likes")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        List<Blog> records = page.getRecords();
        UserDTO user = UserHolder.getUser();
        records.forEach(blog -> {
            User author = userService.getById(blog.getUserId());
            blog.setAuthorName(author.getNickName());
            blog.setIcon(author.getIcon());
            if (user != null) {
                boolean liked = likedByMe(blog.getId(), user.getId());
                blog.setLikedByMe(liked);
            }
        });
        return Result.ok(records);
    }

    @Override
    public Result like(Long id) {
        Long userId = UserHolder.getUser().getId();
        boolean liked = likedByMe(id, userId);
        String key = RedisConstants.BLOG_LIKED_BY_KEY + id;
        if (Boolean.TRUE.equals(liked)) {
            redisTemplate.opsForZSet().remove(key, userId.toString());
            update().setSql("likes = likes - 1").eq("id", id).update();         // 修改 MySQL 中记录
            return Result.ok("取消点赞成功");
        } else {
            redisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());     // 将当前用户加入笔记的点赞用户集合中，当前时间作为该用户的分数
            update().setSql("likes = likes + 1").eq("id", id).update();
            return Result.ok("点赞成功");
        }
    }

    /**
     * 检查某个用户有没有点赞过某篇笔记。
     * @param blogId    笔记 ID
     * @param userId    用户 ID
     * @return          查询结果
     */
    private boolean likedByMe(long blogId, long userId) {
        String key = RedisConstants.BLOG_LIKED_BY_KEY + blogId;
        Double score = redisTemplate.opsForZSet().score(key, String.valueOf(userId));
        return score != null;
    }

    /**
     * 保存笔记。
     * @param blog      笔记实体
     * @return          操作结果
     */
    @Override
    public Result saveBlog(Blog blog) {
        blog.setUserId(UserHolder.getUser().getId());
        save(blog);
        return Result.ok(blog.getId());
    }

    /**
     * 查询某篇笔记最先点赞的 5 个用户。
     * @param id        笔记 ID
     * @return          查询结果
     */
    @Override
    public Result likedUsers(Long id) {
        String key = RedisConstants.BLOG_LIKED_BY_KEY + id;
        Set<String> userIds = redisTemplate.opsForZSet().range(key, 0, 4);
        if (userIds == null) {
            return Result.ok(Collections.emptyList());
        }
        List<UserDTO> users = userIds.stream()
                .map(userService::getById)
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(users);
    }

    /**
     * 分页查询某个用户的所有笔记。
     * @param userId    用户 ID
     * @param current   当前页
     * @return          查询结果
     */
    @Override
    public Result blogByUser(Long userId, Integer current) {
        List<Blog> blogs = query().eq("user_id", userId)
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE))
                .getRecords();
        return Result.ok(blogs);
    }
}
