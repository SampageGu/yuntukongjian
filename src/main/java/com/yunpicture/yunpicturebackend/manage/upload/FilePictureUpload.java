package com.yunpicture.yunpicturebackend.manage.upload;

import cn.hutool.core.io.FileUtil;
import com.yunpicture.yunpicturebackend.exception.ErrorCode;
import com.yunpicture.yunpicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 文件上传
 */
@Service
public class FilePictureUpload extends PictureUploadTemplate {
    /**
     * 获取输入源的原始文件名
     *
     * @param inputSource
     * @return
     */
    @Override
    protected String getOriginalFilename(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }

    /**
     * 处理输入流并生成本地文件
     *
     * @param inputSource
     * @param file
     */
    @Override
    protected void processFile(Object inputSource, File file) throws IOException {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        multipartFile.transferTo(file);
    }

    /**
     * 校验输入流（本地文件或url）
     *
     * @param inputSource
     */
    @Override
    protected void validPicture(Object inputSource) {

        MultipartFile multipartFile = (MultipartFile) inputSource;
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件为空");
//        1、校验文件大小
        long fileSize = multipartFile.getSize();
        final long ONE_M = 1024 * 1024;
        ThrowUtils.throwIf(fileSize > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过2M");
//        2、校验文件后缀
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
//        3、允许上传的文件后缀列表（集合）
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "png", "jpg", "webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(suffix), ErrorCode.PARAMS_ERROR, "文件类型错误");
    }
}
