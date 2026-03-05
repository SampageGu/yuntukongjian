package com.yunpicture.yunpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 图片编辑请求
 */
@Data
public class PictureEditRequest implements Serializable {


    private Long id;


    private String name;


    private String introduction;


    private String category;


    private List<String> tags;

    private static final long serialVersionUID = 1L;
}
