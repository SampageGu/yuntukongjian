package com.yunpicture.yunpicturebackend.model.dto.space;

import com.yunpicture.yunpicturebackend.common.PageRequest;
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
    /**
     * 空间类型：0-私有 1-团队
     */
    private Integer spaceType;

    private static final long serialVersionUID = 1L;
}
