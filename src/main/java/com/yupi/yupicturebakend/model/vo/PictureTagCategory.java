package com.yupi.yupicturebakend.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 图片标签分类
 */
@Data
public class PictureTagCategory {

    private List<String> tagList;

    private List<String> categoryList;
}
