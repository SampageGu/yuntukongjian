package com.yunpicture.yunpicturebackend.manage.websocket.model;

import com.yunpicture.yunpicturebackend.model.vo.UserVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PictureEditResponseMessage {


    /**
     * 消息类型
     */
    private String type;


    /**
     * 信息
     */
    private String message;


    /**
     * 执行的编辑动作
     */
    private String editAction;

    
    private UserVO user;
}
