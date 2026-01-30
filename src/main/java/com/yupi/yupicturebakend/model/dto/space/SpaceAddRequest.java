package com.yupi.yupicturebakend.model.dto.space;

import lombok.Data;

import java.io.Serializable;

@Data
public class SpaceAddRequest implements Serializable {


    private String spaceName;


    private Integer spaceLevel;

    private static final long serialVersionUID = 1L;
}
