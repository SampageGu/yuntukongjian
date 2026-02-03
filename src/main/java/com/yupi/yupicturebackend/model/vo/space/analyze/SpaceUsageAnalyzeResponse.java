package com.yupi.yupicturebackend.model.vo.space.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间使用封装响应类
 */
@Data
public class SpaceUsageAnalyzeResponse implements Serializable {


    private Long usedSize;


    private Long maxSize;


    private Double sizeUsageRatio;


    private Long usedCount;


    private Long maxCount;


    private Double countUsageRatio;

    private static final long serialVersionUID = 1L;
}
