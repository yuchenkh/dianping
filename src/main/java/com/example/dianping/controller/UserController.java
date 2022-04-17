package com.example.dianping.controller;

import com.example.dianping.dto.LoginFormDTO;
import com.example.dianping.dto.Result;
import com.example.dianping.entity.UserInfo;
import com.example.dianping.service.IUserInfoService;
import com.example.dianping.service.IUserService;
import com.example.dianping.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

/**
 * 用户登录相关方法。最后编辑于：2022-4-17。
 * @author yuchen
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    /**
     * 用户手机号登录验证码
     * @param phone     用户手机号
     * @param session   该用户对应的 HttpSession，不需要前端手动提交
     * @return          处理结果
     */
    @PostMapping("/code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        return userService.sendCode(phone, session);
    }

    /**
     * 用户登录（可以是手机验证码登录，也可以是密码登录）。
     * @param loginForm     登录表单，包含手机号、验证码或密码。
     * @param session       {@code HttpSession}
     * @return              处理结果
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session){
        return userService.login(loginForm, session);
    }

    /**
     * 登出功能
     * @return 无
     */
    @PostMapping("/logout")
    public Result logout(){
        // TODO 实现登出功能
        return Result.fail("功能未完成");
    }

    /**
     * 查询用户个人信息。
     * @return  处理结果
     */
    @GetMapping("/me")
    public Result me() {
        return Result.ok(UserHolder.getUser());         // 这里 UserHolder 中的用户信息是拦截器在请求到达 controller 之前写入的
    }

    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }
}
