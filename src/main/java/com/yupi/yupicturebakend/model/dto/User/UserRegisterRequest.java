package com.yupi.yupicturebakend.model.dto.User;


import lombok.Data;

import java.io.Serializable;


/**
 * 用户注册
 */
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 8323534294822758030L;
    //    账号
    private String userAccount;
    //    密码
    private String userPassword;
    //    确认密码
    private String checkPassword;


}
