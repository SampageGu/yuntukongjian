package com.yupi.yupicturebackend.model.dto.User;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户新增
 *
 * @TableName user
 */
@Data
public class UserAddRequest implements Serializable {


    private String userName;


    private String userAccount;


    private String userAvatar;


    private String userProfile;


    private String userRole;

    private static final long serialVersionUID = 1L;
}
