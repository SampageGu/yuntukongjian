package com.yunpicture.yunpicturebackend.manage.websocket.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PictureEditRequestMessage {

    
    private String type;


    /**
     * 执行的编辑动作（放大、缩小。。。）
     */
    private String editAction;
}
