package com.yupi.yupicturebakend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class SearchPictureByColor implements Serializable {


    /**
     * 空间 id
     */
    private Long spaceId;

    private String picColor;

    private static final long serialVersionUID = 1L;
}
