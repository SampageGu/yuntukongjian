package com.yupi.yupicturebackend.model.vo;

import com.yupi.yupicturebackend.model.entity.Space;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 空间视图
 */
@Data
public class SpaceVO implements Serializable {

    private Long id;


    private String spaceName;


    private Integer spaceLevel;


    private Long maxSize;


    private Long maxCount;


    private Long totalSize;


    private Long totalCount;


    private Long userId;


    private Date createTime;


    private Date editTime;


    private Date updateTime;

    /**
     * 空间类型：0-私有 1-团队
     */
    private Integer spaceType;

    /**
     * 创建用户信息
     */
    private UserVO user;

    private static final long serialVersionUID = 1L;

    /**
     * 权限列表
     */
    private List<String> permissionList = new ArrayList<>();

    public static Space voToObj(SpaceVO spaceVO) {
        if (spaceVO == null) {
            return null;
        }
        Space space = new Space();
        BeanUtils.copyProperties(spaceVO, space);
        return space;
    }


    public static SpaceVO objToVo(Space space) {
        if (space == null) {
            return null;
        }
        SpaceVO spaceVO = new SpaceVO();
        BeanUtils.copyProperties(space, spaceVO);
        return spaceVO;
    }
}
