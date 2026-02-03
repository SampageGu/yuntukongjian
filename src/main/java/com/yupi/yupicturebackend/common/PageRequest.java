package com.yupi.yupicturebackend.common;

import lombok.Data;
/**
 * 通用的分页查询请求类
 */
@Data
public class PageRequest {


    private int current = 1;


    private int pageSize = 10;


    private String sortField;


    private String sortOrder = "descend";
}
