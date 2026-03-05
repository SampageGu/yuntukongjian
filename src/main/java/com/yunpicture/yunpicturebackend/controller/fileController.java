package com.yunpicture.yunpicturebackend.controller;

import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import com.yunpicture.yunpicturebackend.annotation.AuthCheck;
import com.yunpicture.yunpicturebackend.common.BaseResponse;
import com.yunpicture.yunpicturebackend.common.ResultUtils;
import com.yunpicture.yunpicturebackend.constant.UserConstant;
import com.yunpicture.yunpicturebackend.exception.BusinessException;
import com.yunpicture.yunpicturebackend.exception.ErrorCode;
import com.yunpicture.yunpicturebackend.manage.CosManage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@RequestMapping("/file")
@RestController
@Slf4j
public class fileController {


    @Resource
    private CosManage cosManage;


    /**
     * 测试文件上传
     *
     * @param multipartFile
     * @return
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile) {
        String originalFilename = multipartFile.getOriginalFilename();
        String filePath = String.format("/test/%s", originalFilename);
//        上传文件
        File file = null;
        try {
            file = File.createTempFile(filePath, null);
            multipartFile.transferTo(file);
            cosManage.putObject(filePath, file);
//        返回访问的地址
            return ResultUtils.success(filePath);
        } catch (IOException e) {
            log.error("file upload error,filePath=" + filePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");

        } finally {
            if (file != null) {
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error" + filePath);
                }
            }
        }

    }

    /**
     * 文件下载
     *
     * @param filepath
     * @param response
     * @throws IOException
     */
    @GetMapping("/test/download")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public void testDownloadFile(String filepath, HttpServletResponse response) throws IOException {
        COSObjectInputStream cosObjectInput = null;


        try {
            COSObject object = cosManage.getObject(filepath);
            cosObjectInput = object.getObjectContent();
            byte[] bytes = IOUtils.toByteArray(cosObjectInput);

//        设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
//      写入相应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (IOException e) {
            log.error("file Download error,filePath=" + filepath, e);
            throw new RuntimeException(e);
        } finally {
            if (cosObjectInput != null) {
                cosObjectInput.close();
            }
        }

    }
}
