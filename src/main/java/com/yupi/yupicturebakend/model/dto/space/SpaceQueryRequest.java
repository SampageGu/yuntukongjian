package com.yupi.yupicturebakend.model.dto.space;

import com.yupi.yupicturebakend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询空间请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceQueryRequest extends PageRequest implements Serializable {


    private Long id;


    private Long userId;


    private String spaceName;


    private Integer spaceLevel;

    private static final long serialVersionUID = 1L;
}
