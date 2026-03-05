package com.yunpicture.yunpicturebackend.model.dto.User;


import lombok.Data;

import java.io.Serializable;


/**
 * 用户注册
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 8323534294822758030L;
    //    账号
    private String userAccount;
    //    密码
    private String userPassword;


}
