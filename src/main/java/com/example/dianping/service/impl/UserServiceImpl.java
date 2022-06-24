package com.example.dianping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dianping.dto.LoginFormDTO;
import com.example.dianping.dto.Result;
import com.example.dianping.dto.UserDTO;
import com.example.dianping.entity.User;
import com.example.dianping.mapper.UserMapper;
import com.example.dianping.service.IUserService;
import com.example.dianping.utils.RegexUtils;
import com.example.dianping.utils.SystemConstants;
import com.example.dianping.utils.interceptor.UserBindingInterceptor;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static com.example.dianping.utils.RedisConstants.*;

/**
 * 用户登录相关服务实现类。最后编辑于：2022-4-25。
 * @author yuchen
 */
@Service
@AllArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 用户登录，向其手机号发送验证码
     * @param phone     用户手机号
     * @return          处理结果
     */
    @Override
    public Result sendCode(String phone) {
        // 验证手机号的合法性
        if (RegexUtils.isPhoneInvalid(phone)) {
            log.debug("用户提交的手机号不合法：" + phone);
            return Result.fail("手机号不合法");
        } else {
            // 随机生成一个 6 位数的验证码
            String pin = RandomUtil.randomNumbers(6);
            // 将生成的验证码保存至 Redis，用于用户登录时与其提交的值进行比对
            String pinKey = PIN_KEY_PREFIX + phone;
            redisTemplate.opsForValue().set(pinKey, pin, Duration.ofMinutes(PIN_TTL));
            // 暂时通过在日志中打印出验证码，来模拟向用户手机发送验证码的过程
            log.debug("向用户手机号 " + phone + " 发送验证码：" + pin);

            return Result.ok();
        }
    }

    /**
     * 用户登录。
     * @param loginForm     用户提交的登录表单
     * @return              处理结果
     */
    @Override
    public Result login(LoginFormDTO loginForm) {
        // 验证手机号的合法性
        String phone = loginForm.getPhone();
        String password = loginForm.getPassword();
        String pin = loginForm.getCode();
        if (RegexUtils.isPhoneInvalid(phone)) {
            log.debug("用户提交的手机号不合法：" + phone);
            return Result.fail("手机号不合法");
        }
        if (password != null) {
            return loginWithPassword(phone, password);
        } else if (pin != null) {
            return loginWithPIN(phone, pin);
        } else {
            return Result.fail("登录失败");
        }
    }

    /**
     * 使用密码登录。
     * @param phone     手机号
     * @param password  密码
     * @return          登录结果
     */
    private Result loginWithPassword(String phone, String password) {
        User user = getOne(new QueryWrapper<User>().eq("phone", phone));
        if (user == null) {
            return Result.fail("该手机号尚未注册");
        }
        if (!password.equals(user.getPassword())) {
            return Result.fail("密码错误");
        }
        // 将登录用户的部分信息缓存至 Redis 并获取这次登录所对应的 token
        log.debug("用户使用密码登录成功，手机号：" + phone);
        String token = cacheLoggedInUserInfo(phone);

        return Result.ok(token);        // 将生成的用户 token 作为返回值传给前端，供用户下次请求时作为 header
    }

    /**
     * 使用手机验证码登录。
     * @param phone     手机号
     * @param pin       验证码
     * @return          登录结果
     */
    private Result loginWithPIN(String phone, String pin) {
        // 校验用户提交的验证码是否正确
        String cachedPin = redisTemplate.opsForValue().get(PIN_KEY_PREFIX + phone);
        if (!pin.equals(cachedPin)) {
            return Result.fail("验证码错误");
        }
        // 手机号和验证码匹配，现在去数据库中查找该用户，如果不存在则创建用户
        log.debug("用户使用手机验证码登录成功，手机号：" + phone);
        if (!query().eq("phone", phone).exists()) {
            User user = new User()
                    .setPhone(phone)
                    .setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(6));
            save(user);
            log.debug("手机号 " + phone + " 尚未注册，向数据库添加该用户。");
        }
        // 将登录用户的部分信息缓存至 Redis 并获取这次登录所对应的 token
        String token = cacheLoggedInUserInfo(phone);

        return Result.ok(token);        // 将生成的用户 token 作为返回值传给前端，供用户下次请求时作为 header
    }

    /**
     * <p>将指定手机号对应用户的 {@link UserDTO} 信息缓存至 Redis，代表该用户处于已登录状态，同时返回一个随机 token，该 token 只对应于这次登录。</p>
     * <p>缓存的用户信息一方面供拦截器判定用户登录状态使用，Redis 中有用户 token 对应的用户信息时，即说明该用户目前是已登录状态；
     * 另一方面，已登录用户每次请求接口时，我们定义的 {@link UserBindingInterceptor} 拦截器会将 Redis 中缓存的用户信息保存至当前线程的 ThreadLocal 中，供 `/user/me` 等 endpoint 使用。</p>
     *
     *
     * @param phone     用户手机号
     * @return          生成的随机 token
     */
    private String cacheLoggedInUserInfo(String phone) {
        User user = query().eq("phone", phone).one();
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        String token = UUID.randomUUID(false).toString();        // 使用 UUID 生成这次登录对应的 token
        // UserDTO -> HashMap
        // 注意：Redis 要求所有 key 和 value 都是字符串类型，所以这里需要处理一下
        Map<String, Object> userMap = BeanUtil.beanToMap(
                userDTO,
                new HashMap<>(),
                CopyOptions.create()
                        .ignoreNullValue()
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        redisTemplate.opsForHash().putAll(LOGGED_IN_USER_PREFIX + token, userMap);
        redisTemplate.expire(LOGGED_IN_USER_PREFIX + token, Duration.ofMinutes(LOGGED_IN_USER_TTL));        // 设置用户 token 过期时间
        log.debug("已将用户：" + phone + " 的部分信息缓存至 Redis，并生成 token：" + token);

        return token;
    }
}
