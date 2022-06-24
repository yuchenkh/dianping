package com.example.dianping.utils.interceptor;

import com.example.dianping.utils.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * 用于登录状态检查的拦截器。最后编辑于：2022-4-17。
 * @author yuchen
 */
public class LoginCheckInterceptor implements HandlerInterceptor {

    // 对于接管的每一个请求，如果其 UserHolder 中有用户信息则说明已经登录
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (UserHolder.getUser() == null) {         // 若当前线程中没有用户信息，则说明用户是未登录状态，拦截该请求
            response.setStatus(401);
            return false;
        }
        return true;
    }
}
