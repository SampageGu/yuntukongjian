package com.yupi.yupicturebakend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadRequest implements Serializable {


    private Long id;

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 文件url地址
     */
    private String fileUrl;

    /**
     * 图片名称
     */
    private String picName;

    private static final long serialVersionUID = 1L;
}
