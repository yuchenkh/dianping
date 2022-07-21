package com.example.dianping.controller;

import com.example.dianping.dto.LoginFormDTO;
import com.example.dianping.dto.Result;
import com.example.dianping.service.IUserInfoService;
import com.example.dianping.service.IUserService;
import com.example.dianping.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 用户登录相关方法。最后编辑于：2022-4-17。
 *
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
     *
     * @param phone 用户手机号
     * @return 处理结果
     */
    @PostMapping("/code")
    public Result sendCode(@RequestParam("phone") String phone) {
        return userService.sendCode(phone);
    }

    /**
     * 用户登录（可以是手机验证码登录，也可以是密码登录）。
     *
     * @param loginForm 登录表单，包含手机号、验证码或密码。
     * @return 处理结果
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm) {
        return userService.login(loginForm);
    }

    /**
     * 登出功能
     *
     * @return 无
     */
    @PostMapping("/logout")
    public Result logout() {
        // TODO 实现登出功能
        return Result.fail("功能未完成");
    }

    /**
     * 查询用户信息。
     *
     * @param id 用户 ID
     * @return 查询结果
     */

    @GetMapping("/{id}")
    public Result getUser(@PathVariable Long id) {
        return Result.ok(userService.getById(id));
    }

    /**
     * 查询用户详细信息。
     * @param userId    用户 ID
     * @return          查询结果
     */
    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId) {
        return Result.ok(userInfoService.getById(userId));
    }

    /**
     * 查询当前用户信息。
     * 对这个接口的请求会被拦截器拦截，所以这里不用管登录状态的校验工作。
     * 如果用户已经登录，相应 ThreadLocal 中会有用户信息，所以可以直接到 UserHolder 中取。
     *
     * @return 处理结果
     */
    @GetMapping("/me")
    public Result me() {
        return Result.ok(UserHolder.getUser());         // 这里 UserHolder 中的用户信息是拦截器在请求到达 controller 之前写入的
    }

}
