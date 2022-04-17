package com.example.dianping.utils;

import com.example.dianping.dto.UserDTO;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 用于登录状态检查的拦截器。最后编辑于：2022-4-17。
 * @author yuchen
 */
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 获取当前 request 所关联的 session
        HttpSession session = request.getSession();
        // 从 session 中获取用户信息
        Object user = session.getAttribute("user");
        // 如果 session 中没有用户信息，则说明该用户未登录，拦截该请求并设定响应状态码为 401
        if (user == null) {
            response.setStatus(401);
            return false;
        }
        // 若用户已登录，则调用静态方法将用户信息存放至 ThreadLocal 供后续业务逻辑使用，然后放行
        UserHolder.saveUser((UserDTO) user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserHolder.removeUser();
    }
}
