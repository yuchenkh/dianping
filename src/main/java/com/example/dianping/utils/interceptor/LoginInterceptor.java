package com.example.dianping.utils.interceptor;

import cn.hutool.core.util.StrUtil;
import com.example.dianping.utils.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * 用于登录状态检查的拦截器。最后编辑于：2022-4-17。
 * @author yuchen
 */
public class LoginInterceptor implements HandlerInterceptor {

    // 对于接管的每一个请求，如果其 UserHolder 中有用户信息则说明已经登录
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 从 request header 中取得用户 token（由前端添加）
        String token = request.getHeader("authorization");
        if (StrUtil.isBlank(token)) {       // 前端请求未提供 token 则拦截该请求，并设定响应状态码为 401
            response.setStatus(401);
            return false;
        }

        if (UserHolder.getUser() == null) {
            response.setStatus(401);
            return false;
        }

        return true;
    }
}
