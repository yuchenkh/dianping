package com.example.dianping.service.impl;

import cn.hutool.core.util.StrUtil;
import com.example.dianping.utils.SystemConstants;

import java.io.File;
import java.util.UUID;

/**
 * 笔记上传需要用到的一些方法。没有对应的实体，所以没有服务接口和实现类的概念。
 */
public class UploadService {

    /**
     * 生成随机文件名。
     * @param originalFilename  原文件名
     * @return                  随机文件名
     */
    public static String randomFileName(String originalFilename) {
        // 获取后缀
        String suffix = StrUtil.subAfter(originalFilename, ".", true);
        // 生成目录
        String name = UUID.randomUUID().toString();
        int hash = name.hashCode();
        int d1 = hash & 0xF;
        int d2 = (hash >> 4) & 0xF;
        // 判断目录是否存在
        File dir = new File(SystemConstants.IMAGE_UPLOAD_DIR, StrUtil.format("/blogs/{}/{}", d1, d2));
        if (!dir.exists()) {
            dir.mkdirs();
        }
        // 生成文件名
        return StrUtil.format("/blogs/{}/{}/{}.{}", d1, d2, name, suffix);
    }
}
