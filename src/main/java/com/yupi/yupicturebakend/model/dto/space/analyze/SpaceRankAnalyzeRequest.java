package com.yupi.yupicturebakend.model.dto.space.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间排行
 */
@Data
public class SpaceRankAnalyzeRequest implements Serializable {


    private Integer topN = 10;

    private static final long serialVersionUID = 1L;
}

