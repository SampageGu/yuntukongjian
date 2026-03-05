package com.yunpicture.yunpicturebackend.model.vo.space.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 空间图片分类响应
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceCategoryAnalyzeResponse implements Serializable {


    private String category;


    private Long count;


    private Long totalSize;

    private static final long serialVersionUID = 1L;
}
