package com.yupi.yupicturebackend.manage.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import com.yupi.yupicturebackend.config.CosClientConfig;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.manage.CosManage;
import com.yupi.yupicturebackend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;


/**
 * 抽象类 上传图片
 */
@Slf4j
public abstract class PictureUploadTemplate {

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
     * @param inputSource
     * @param uploadPathPrefix
     * @return
     */
    public UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
//        1、校验图片
        validPicture(inputSource);
//        2、获取图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originalFilename = getOriginalFilename(inputSource);
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
//            处理文件来源
            processFile(inputSource, file);
            PutObjectResult putObjectResult = cosManage.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
//            获取到图片处理结果
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if (CollUtil.isNotEmpty(objectList)) {
//                获取压缩之后得到的文件信息
                CIObject compressedCiObject = objectList.get(0);
//                缩略图默认等于压缩图
                CIObject thumbnailCiObject=compressedCiObject;
                if(objectList.size()>1)
                {
                    thumbnailCiObject = objectList.get(1);
                }

//                封装压缩图的返回结果
                return buildResult(originalFilename, compressedCiObject, thumbnailCiObject, imageInfo);
            }
            return buildResult(imageInfo, uploadPath, originalFilename, file);
        } catch (IOException e) {
            log.error("图片上传失败" + uploadPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");

        } finally {
            //        清理临时文件
            deleteTempFile(file);
        }

    }

    /**
     * 封装返回结果
     *
     * @param originalFilename   原始文件名
     * @param compressedCiObject 压缩后的对象
     * @param thumbnailCiObject 缩略图对象
     * @return
     */
    private UploadPictureResult buildResult(String originalFilename, CIObject compressedCiObject, CIObject thumbnailCiObject, ImageInfo imageInfo) {
        //封装返回结果
//      计算宽和高
        int picwidth = compressedCiObject.getWidth();
        int picheight = compressedCiObject.getHeight();
        double picScale = NumberUtil.round(picwidth * (1.0) / picheight, 2).doubleValue();

//      设置压缩后的原图地址
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressedCiObject.getKey());
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicSize(compressedCiObject.getSize().longValue());
        uploadPictureResult.setPicWidth(picwidth);
        uploadPictureResult.setPicHeight(picheight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(compressedCiObject.getFormat());
//        设置缩略图地址
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/"+thumbnailCiObject.getKey());
        uploadPictureResult.setPicColor(imageInfo.getAve());
        //        返回访问的地址
        return uploadPictureResult;
    }

    /**
     * 封装返回结果
     *
     * @param imageInfo        对象存储返回的图片信息
     * @param uploadPath
     * @param originalFilename
     * @param file
     * @return
     */
    private UploadPictureResult buildResult(ImageInfo imageInfo, String uploadPath, String originalFilename, File file) {
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
        uploadPictureResult.setPicColor(imageInfo.getAve());

        //        返回访问的地址
        return uploadPictureResult;
    }

    /**
     * 获取输入源的原始文件名
     *
     * @param inputSource
     * @return
     */
    protected abstract String getOriginalFilename(Object inputSource);

    /**
     * 处理输入流并生成本地文件
     *
     * @param inputSource
     */
    protected abstract void processFile(Object inputSource, File file) throws IOException;

    /**
     * 校验输入流（本地文件或url）
     *
     * @param inputSource
     */
    protected abstract void validPicture(Object inputSource);


}
