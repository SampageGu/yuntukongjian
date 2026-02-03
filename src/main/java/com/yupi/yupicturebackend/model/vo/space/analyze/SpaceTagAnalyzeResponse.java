package com.yupi.yupicturebackend.model.vo.space.analyze;

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
public class SpaceTagAnalyzeResponse implements Serializable {


    private String category;


    private Long count;


    private Long totalSize;

    private static final long serialVersionUID = 1L;

    // 手动添加这个构造函数
    public SpaceTagAnalyzeResponse(String category, Long count) {
        this.category = category;
        this.count = count;
    }
}
