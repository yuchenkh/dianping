package com.example.dianping.utils.interceptor;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.example.dianping.dto.UserDTO;
import com.example.dianping.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Map;

import static com.example.dianping.utils.RedisConstants.LOGGED_IN_USER_PREFIX;

/**
 * 最外层的拦截器，拦截对后端接口的所有请求，若用户已登录则将用户信息注入当前执行线程并刷新 Redis 中 token 的有效期。
 * 最后编辑于：2022-6-24。
 * @author yuchen
 */
public class UserBindingInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;

    // 注意：拦截器并不是由 Spring 初始化的，所以 Spring 也不会帮我们注入 StringRedisTemplate 的依赖，需要自己初始化
    public UserBindingInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * <p>拦截请求，将对应的用户信息注入当前执行线程，并刷新 Redis 中相应的已登录用户记录的过期时间。</p>
     * <p>实现逻辑：检查请求是否带有 "authorization" 请求头（代表用户的 token），如果含有该请求头则将其与 Redis 中的记录比对，
     * 如果匹配则说明该用户已经登录，将用户信息保存到当前线程中（利用 {@link ThreadLocal} 实现），以供后续业务方法使用</p>
     *
     * @param request   HTTP 请求
     * @param response  HTTP 响应
     * @param handler
     * @return
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("authorization");      // 从 request header 中取得用户 token（由前端添加）
        if (StrUtil.isBlank(token)) {           // 若请求头未携带 token，则直接放行交给下一个拦截器
            return true;
        }
        if (Boolean.TRUE.equals(redisTemplate.hasKey(LOGGED_IN_USER_PREFIX + token))) {                     // 根据用户 token 在 Redis 中查询相关记录
            Map<Object, Object> user = redisTemplate.opsForHash().entries(LOGGED_IN_USER_PREFIX + token);
            UserHolder.saveUser(BeanUtil.fillBeanWithMap(user, new UserDTO(), false));          // 把用户信息存到 ThreadLocal 中
            redisTemplate.expire(LOGGED_IN_USER_PREFIX + token, Duration.ofMinutes(30));                // 刷新 Redis 中记录的过期时间
        }
        return true;
    }

    // 请求处理完成后，从当前执行线程中移除用户信息
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserHolder.removeUser();
    }
}

