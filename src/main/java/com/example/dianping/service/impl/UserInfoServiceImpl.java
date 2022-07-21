package com.example.dianping.service.impl;

import com.example.dianping.entity.UserInfo;
import com.example.dianping.mapper.UserInfoMapper;
import com.example.dianping.service.IUserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;


@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

}
