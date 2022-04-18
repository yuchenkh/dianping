package com.example.dianping.config;

import com.example.dianping.utils.interceptor.AccessInterceptor;
import com.example.dianping.utils.interceptor.LoginInterceptor;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 配置。最后编辑于：2022-4-17。
 * @author yuchen
 */
@Configuration
@AllArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    /**
     * 这个依赖实际是在 {@link AccessInterceptor} 中使用
     */
    private final StringRedisTemplate stringRedisTemplate;

    // 添加自定义的拦截器
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AccessInterceptor(stringRedisTemplate)).order(0);
        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(
                        "/shop/**",
                        "/shop-type/**",
                        "/voucher/**",
                        "/blog/hot",
                        "/user/code",
                        "/user/login"
                )
                .order(1);
    }
}
