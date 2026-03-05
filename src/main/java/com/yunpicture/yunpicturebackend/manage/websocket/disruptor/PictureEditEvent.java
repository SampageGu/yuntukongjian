package com.yunpicture.yunpicturebackend.manage.websocket.disruptor;

import com.yunpicture.yunpicturebackend.manage.websocket.model.PictureEditRequestMessage;
import com.yunpicture.yunpicturebackend.model.entity.User;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

/**
 * 图片编辑事件
 */
@Data
public class PictureEditEvent {

    
    private PictureEditRequestMessage pictureEditRequestMessage;

    
    private WebSocketSession session;
    
    
    private User user;

    
    private Long pictureId;

}
