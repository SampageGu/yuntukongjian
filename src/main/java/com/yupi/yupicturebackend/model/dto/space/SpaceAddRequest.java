package com.yupi.yupicturebackend.model.dto.space;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceAddRequest implements Serializable {


    private String spaceName;


    private Integer spaceLevel;

    /**
     * 空间类型：0-私有 1-团队
     */
    private Integer spaceType;

    private static final long serialVersionUID = 1L;
}
