package com.yupi.yupicturebackend.model.vo.space.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 空间图片大小响应
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpaceSizeAnalyzeResponse implements Serializable {


    private String sizeRange;


    private Long count;

    private static final long serialVersionUID = 1L;
}