package com.example.dianping.service.impl;

import cn.hutool.core.bean.BeanUtil;
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
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

/**
 * 用户登录相关服务实现类。最后编辑于：2022-4-17。
 * @author yuchen
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    /**
     * 根据用户提供的手机号和 Session 信息，生成验证码。
     * @param phone     用户手机号
     * @param session   该用户对应的 {@code HttpSession}
     * @return          处理结果
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        // 验证手机号的合法性
        if (RegexUtils.isPhoneInvalid(phone)) {
            log.debug("用户提交的手机号不合法：" + phone);
            return Result.fail("手机号不合法");
        } else {
            // 随机生成一个 6 位数的验证码
            String code = RandomUtil.randomNumbers(6);
            // 在 session 中保存
            session.setAttribute("code", code);
            // 暂时通过在日志中打印出验证码，来模拟向用户手机发送验证码的过程
            log.debug("向用户手机号 " + phone + " 发送验证码：" + code);
            return Result.ok();
        }
    }

    /**
     * 用户登录。
     * @param loginForm     用户提交的登录表单
     * @param session       {@code HttpSession} 实体
     * @return              处理结果
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        // 验证手机号的合法性
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) {
            log.debug("用户提交的手机号不合法：" + phone);
            return Result.fail("手机号不合法");
        }
        // 校验表单中的验证码
        String cachedCode = session.getAttribute("code").toString();
        if (!loginForm.getCode().equals(cachedCode)) {
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
        // 将用户信息存到 session 中
        User user = query().eq("phone", phone).one();
        session.setAttribute("user",
                BeanUtil.copyProperties(user, UserDTO.class));
        return Result.ok();
    }
}
