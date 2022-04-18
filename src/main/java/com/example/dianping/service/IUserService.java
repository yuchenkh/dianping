package com.example.dianping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.dianping.dto.LoginFormDTO;
import com.example.dianping.dto.Result;
import com.example.dianping.entity.User;

import javax.servlet.http.HttpSession;

/**
 * 用户登录相关服务。最后编辑于：2022-4-17。
 * @author yuchen
 */
public interface IUserService extends IService<User> {

    // 向用户手机发送验证码
    Result sendCode(String phone);

    // 用户登录
    Result login(LoginFormDTO loginForm);
}
