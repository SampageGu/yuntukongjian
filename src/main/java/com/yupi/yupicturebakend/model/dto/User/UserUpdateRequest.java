package com.yupi.yupicturebakend.model.dto.User;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户更新
 *
 * @TableName user
 */
@Data
public class UserUpdateRequest implements Serializable {


    private Long id;


    private String userName;


    private String userAvatar;


    private String userProfile;


    private String userRole;

    private static final long serialVersionUID = 1L;
}

