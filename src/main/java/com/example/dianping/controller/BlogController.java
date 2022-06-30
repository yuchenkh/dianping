package com.example.dianping.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dianping.dto.Result;
import com.example.dianping.dto.UserDTO;
import com.example.dianping.entity.Blog;
import com.example.dianping.service.IBlogService;
import com.example.dianping.service.IUserService;
import com.example.dianping.utils.SystemConstants;
import com.example.dianping.utils.UserHolder;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 查看笔记、保存笔记、点赞笔记、查询热门笔记等接口。
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@AllArgsConstructor
@RequestMapping("/blog")
public class BlogController {

    private final IBlogService blogService;

    private final IUserService userService;

    /**
     * 根据 ID 查询笔记。
     * @param id    笔记 ID
     * @return      查询结果
     */
    @GetMapping("/detail/{id}")
    public Result get(@PathVariable Long id) {
        return blogService.getBlog(id);
    }

    /**
     * 获取热门笔记。
     * @param current       当前页
     * @return              查询结果
     */
    @GetMapping("/hot")
    public Result queryHotBlogs(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogService.queryHotBlogs(current);
    }

    /**
     * 对笔记点赞/取消点赞。
     * @param id    笔记 ID
     * @return      结果
     */
    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        return blogService.like(id);
    }

    /**
     * 保存笔记。
     * @param blog      笔记实体
     * @return          保存结果
     */
    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        return blogService.saveBlog(blog);
    }

    /**
     * 查询笔记的点赞用户列表（按点赞时间取前 5 个）。
     * @param id        笔记 ID
     * @return          查询结果
     */
    @GetMapping("/likedUsers/{id}")
    public Result likedUsers(@PathVariable Long id) {
        return blogService.likedUsers(id);
    }

    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        // 根据用户查询
        Page<Blog> page = blogService.query()
                .eq("user_id", user.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }
}
