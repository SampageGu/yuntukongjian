package com.yupi.yupicturebackend.manage.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * URL图片上传
 */
@Service
public class UrlPictureUpload extends PictureUploadTemplate {
    /**
     * 获取输入源的原始文件名
     *
     * @param inputSource
     * @return
     */
    @Override
    protected String getOriginalFilename(Object inputSource) {
        String fileUrl = (String) inputSource;
        return FileUtil.mainName(fileUrl);
    }

    /**
     * 处理输入流并生成本地文件
     *
     * @param inputSource
     * @param file
     */
    @Override
    protected void processFile(Object inputSource, File file) throws IOException {
        String fileUrl = (String) inputSource;
        HttpUtil.downloadFile(fileUrl, file);
    }

    /**
     * 校验输入流（本地文件或url）
     *
     * @param inputSource
     */
    @Override
    protected void validPicture(Object inputSource) {
        String fileUrl = (String) inputSource;
//        校验非空
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "文件地址为空");
//        校验url格式
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址格式不正确");
        }
//        校验url协议
        ThrowUtils.throwIf(!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://"), ErrorCode.PARAMS_ERROR, "请支持http/https协议的地址");
//        发送head请求验证文件是否存在
        HttpResponse httpResponse = null;
        try {
            httpResponse = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }
            //        文件存在,文件类型校验
            String contentType = httpResponse.header("Content-Type");
//            不为空,才检验是否合法,这样校验规则相对宽松
            if (StrUtil.isNotBlank(contentType)) {
                final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp");
                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPES.contains(contentType.toLowerCase()),
                        ErrorCode.PARAMS_ERROR, "文件类型错误");
            }
            String contentLengthStr = httpResponse.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)) {

                long contentLength = Long.parseLong(contentLengthStr);
                final long ONE_M = 1024 * 1024;
                ThrowUtils.throwIf(contentLength > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过2M");
            }
//        文件大小,文件后缀校验
        } finally {
            if (httpResponse != null) {
                httpResponse.close();
            }

        }
    }
}
