package com.yupi.yupicturebakend.model.dto.picture;

import com.yupi.yupicturebakend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 图片查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PictureQueryRequest extends PageRequest implements Serializable {


    private Long id;


    private String name;


    private String introduction;


    private String category;


    private List<String> tags;


    private Long picSize;


    private Integer picWidth;


    private Integer picHeight;


    private Double picScale;


    private String picFormat;


    private String searchText;


    private Long userId;

    private static final long serialVersionUID = 1L;
}
