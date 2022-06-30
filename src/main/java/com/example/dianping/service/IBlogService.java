package com.example.dianping.service;

import com.example.dianping.dto.Result;
import com.example.dianping.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IBlogService extends IService<Blog> {

    Result getBlog(long id);

    Result queryHotBlogs(Integer current);

    Result like(Long id);

    Result saveBlog(Blog blog);

    Result likedUsers(Long id);
}
