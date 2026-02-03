package com.yupi.yupicturebakend.model.dto.space.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用空间分析请求
 */
@Data
public class SpaceAnalyzeRequest implements Serializable {


    private Long spaceId;


    private boolean queryPublic;


    private boolean queryAll;

    private static final long serialVersionUID = 1L;
}

