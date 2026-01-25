package com.yupi.yupicturebakend.manage;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.yupi.yupicturebakend.common.ResultUtils;
import com.yupi.yupicturebakend.config.CosClientConfig;
import com.yupi.yupicturebakend.exception.BusinessException;
import com.yupi.yupicturebakend.exception.ErrorCode;
import com.yupi.yupicturebakend.exception.ThrowUtils;
import com.yupi.yupicturebakend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.cms.CMSAttributes;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.bouncycastle.asn1.cms.CMSAttributes.contentType;


@Slf4j
@Service
public class FileManage {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManage cosManage;

    /**
     * 清理临时文件
     *
     * @param file
     */
    public static void deleteTempFile(File file) {
        if (file != null) {
            boolean deleteResult = file.delete();
            if (!deleteResult) {
                log.error("file delete error" + file.getAbsoluteFile());
            }
        }
    }

    /**
     * 上传图片到云服务器，返回图片信息
     *
     * @param multipartFile
     * @param uploadPathPrefix
     * @return
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
//        1、校验图片
        validPicture(multipartFile);
//        2、图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();
//        自己拼接上传，增加安全性
        String uploadFilename = String.format("%s_%s.%s",
                DateUtil.formatDate(new Date()),
                uuid,
                FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
//        3、解析结果并返回
        //        上传文件
        File file = null;
        try {
            file = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(file);
            PutObjectResult putObjectResult = cosManage.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
//封装返回结果
//      计算宽和高
            int picwidth = imageInfo.getWidth();
            int picheight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picwidth * (1.0) / picheight, 2).doubleValue();


            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(picwidth);
            uploadPictureResult.setPicHeight(picheight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());

            //        返回访问的地址
            return uploadPictureResult;
        } catch (IOException e) {
            log.error("图片上传失败" + uploadPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");

        } finally {
            //        清理临时文件
            deleteTempFile(file);
        }

    }

    private void validPicture(MultipartFile multipartFile) {
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

    //    TODO 新增的方法
    public UploadPictureResult uploadPictureByURL(String fileUrl, String uploadPathPrefix) {
//        1、校验图片
        //    TODO 校验url
        validPicture(fileUrl);
//        2、图片上传地址
        String uuid = RandomUtil.randomString(16);

//    todo
        String originalFilename = FileUtil.mainName(fileUrl);

//        自己拼接上传，增加安全性
        String uploadFilename = String.format("%s_%s.%s",
                DateUtil.formatDate(new Date()),
                uuid,
                FileUtil.getSuffix(originalFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
//        3、解析结果并返回
        //        上传文件
        File file = null;
        try {
            file = File.createTempFile(uploadPath, null);
//        multipartFile.transferTo(file);
//        todo
//        下载文件
            HttpUtil.downloadFile(fileUrl, file);
            PutObjectResult putObjectResult = cosManage.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
//封装返回结果
//      计算宽和高
            int picwidth = imageInfo.getWidth();
            int picheight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picwidth * (1.0) / picheight, 2).doubleValue();


            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(picwidth);
            uploadPictureResult.setPicHeight(picheight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());

            //        返回访问的地址
            return uploadPictureResult;
        } catch (IOException e) {
            log.error("图片上传失败" + uploadPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");

        } finally {
            //        清理临时文件
            deleteTempFile(file);
        }

    }

    /**
     * 根据url校验文件
     *
     * @param fileUrl
     */
    private void validPicture(String fileUrl) {
//        校验非空
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "文件地址为空");
//        校验url格式
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址格式不正确");
        }
//        校验url协议
        ThrowUtils.throwIf(!fileUrl.startsWith("http://") || !fileUrl.startsWith("https://"), ErrorCode.PARAMS_ERROR, "请支持http/https协议的地址");
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
