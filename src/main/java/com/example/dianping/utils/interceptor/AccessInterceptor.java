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

import static com.example.dianping.utils.RedisConstants.USER_TOKEN_PREFIX;

/**
 * 最外层的拦截器，拦截对后端接口的所有请求，不校验登录状态，若是有 token 则刷新 token 有效期。
 * 最后编辑于：2022-4-18。
 * @author yuchen
 */
public class AccessInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;

    // 注意：拦截器并不是由 Spring 初始化的，所以 Spring 也不会帮我们注入 StringRedisTemplate 的依赖，需要自己初始化
    public AccessInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 这个拦截器对于接管的每一个请求，如果 header 中有 token 说明已登录
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 从 request header 中取得用户 token（由前端添加）
        String token = request.getHeader("authorization");
        // 若请求头未携带 token，则直接放行交给下一个拦截器
        if (StrUtil.isBlank(token)) {
            return true;
        }
        // 根据用户 token 在 Redis 中查询相关记录
        // 如果存在记录则说明用户已登录，把用户信息存到 ThreadLocal 中，并刷新
        if (Boolean.TRUE.equals(redisTemplate.hasKey(USER_TOKEN_PREFIX + token))) {
            Map<Object, Object> user = redisTemplate.opsForHash().entries(USER_TOKEN_PREFIX + token);
            UserHolder.saveUser(BeanUtil.fillBeanWithMap(user, new UserDTO(), false));
            redisTemplate.expire(USER_TOKEN_PREFIX + token, Duration.ofMinutes(30));
        }

        return true;
    }

    // 请求处理完成后，从 ThreadLocal 中移除用户信息
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserHolder.removeUser();
    }
}

