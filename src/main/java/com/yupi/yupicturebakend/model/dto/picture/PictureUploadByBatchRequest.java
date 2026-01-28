package com.yupi.yupicturebakend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadByBatchRequest implements Serializable {


    private static final long serialVersionUID = 1L;
    /**
     * 抓取数量
     */
    private Integer count = 10;
    /**
     * 抓取词
     */
    private String searchText;
    /**
     * 图片前缀
     */
    private String namePrefix;
}
