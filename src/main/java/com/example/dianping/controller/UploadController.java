package com.example.dianping.controller;

import cn.hutool.core.io.FileUtil;
import com.example.dianping.dto.Result;
import com.example.dianping.service.impl.UploadService;
import com.example.dianping.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

/**
 * 文件上传接口。
 */
@Slf4j
@RestController
@RequestMapping("/upload")
public class UploadController {

    /**
     * 上传用户图片。用户在「写笔记」页面添加图片时，需要先调用该接口将每一张图片上传到后端的存储库。
     * @param image     从用户设备读取的图片
     * @return          上传结果，附带被上传图片在后端服务中的地址
     */
    @PostMapping("/image")
    public Result uploadImage(@RequestParam("file") MultipartFile image) {
        try {
            // 获取原始文件名称
            String originalFilename = image.getOriginalFilename();
            // 生成新文件名
            String fileName = UploadService.randomFileName(originalFilename);
            // 保存文件
            image.transferTo(new File(SystemConstants.IMAGE_UPLOAD_DIR, fileName));
            // 返回结果
            log.debug("文件上传成功，在存储库中的文件名：{}", fileName);
            return Result.ok(fileName);
        } catch (IOException e) {
            throw new RuntimeException("文件上传失败", e);
        }
    }

    /**
     * 删除用户在「发笔记」页面上传的图片。
     * @param filename      图片在存储库中的文件名
     * @return              删除结果
     */
    @GetMapping("/image/delete")
    public Result deleteBlogImg(@RequestParam("name") String filename) {
        File file = new File(SystemConstants.IMAGE_UPLOAD_DIR, filename);
        if (file.isDirectory()) {
            return Result.fail("错误的文件名称");
        }
        FileUtil.del(file);
        return Result.ok();
    }

}
