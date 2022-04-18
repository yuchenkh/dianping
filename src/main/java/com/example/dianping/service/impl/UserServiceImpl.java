package com.example.dianping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.dianping.dto.LoginFormDTO;
import com.example.dianping.dto.Result;
import com.example.dianping.dto.UserDTO;
import com.example.dianping.entity.User;
import com.example.dianping.mapper.UserMapper;
import com.example.dianping.service.IUserService;
import com.example.dianping.utils.RegexUtils;
import com.example.dianping.utils.SystemConstants;
import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static com.example.dianping.utils.RedisConstants.*;

/**
 * 用户登录相关服务实现类。最后编辑于：2022-4-17。
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
        if (RegexUtils.isPhoneInvalid(phone)) {
            log.debug("用户提交的手机号不合法：" + phone);
            return Result.fail("手机号不合法");
        }
        // 校验用户提交的验证码是否正确
        String pin = redisTemplate.opsForValue().get(PIN_KEY_PREFIX + phone);
        if (!loginForm.getCode().equals(pin)) {
            return Result.fail("验证码错误");
        }
        // 手机号和验证码匹配，现在去数据库中查找该用户，如果不存在则创建用户
        if (!query().eq("phone", phone).exists()) {
            User user = new User()
                    .setPhone(phone)
                    .setNickName(SystemConstants.USER_NICK_NAME_PREFIX + RandomUtil.randomString(6));
            save(user);
            log.debug("手机号 " + phone + " 尚未注册，向数据库添加该用户。");
        }
        // 将用户部分信息（ID、昵称、头像）保存至 Redis，一方面供拦截器判定用户登录状态使用，Redis 中有用户 token 对应的用户信息时，即说明该用户目前是已登录状态；
        // 另一方面供 /user/me 接口使用，可以直接到 Redis 取这几项用户数据，将来数据项多了，可以得修改业务逻辑
        User user = query().eq("phone", phone).one();
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        String token = UUID.randomUUID(false).toString();        // 用户 token 使用 UUID
        // 注意：Redis 要求所有 key 和 value 都是字符串类型，所以这里需要处理一下
        Map<String, Object> userMap = BeanUtil.beanToMap(
                userDTO,
                new HashMap<>(),
                CopyOptions.create()
                        .ignoreNullValue()
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));

        redisTemplate.opsForHash().putAll(USER_TOKEN_PREFIX + token, userMap);
        redisTemplate.expire(USER_TOKEN_PREFIX + token, Duration.ofMinutes(USER_TOKEN_TTL));        // 设置用户 token 过期时间

        // 将生成的用户 token 作为返回值传给前端，供用户下次请求时作为 header
        return Result.ok(token);
    }
}
