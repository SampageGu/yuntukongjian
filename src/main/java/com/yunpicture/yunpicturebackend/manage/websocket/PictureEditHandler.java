package com.yunpicture.yunpicturebackend.manage.websocket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.yunpicture.yunpicturebackend.manage.websocket.disruptor.PictureEditEventProducer;
import com.yunpicture.yunpicturebackend.manage.websocket.model.PictureEditActionEnum;
import com.yunpicture.yunpicturebackend.manage.websocket.model.PictureEditMessageTypeEnum;
import com.yunpicture.yunpicturebackend.manage.websocket.model.PictureEditRequestMessage;
import com.yunpicture.yunpicturebackend.manage.websocket.model.PictureEditResponseMessage;
import com.yunpicture.yunpicturebackend.model.entity.User;
import com.yunpicture.yunpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图片编辑 WebSocket 处理器
 */
@Component
@Slf4j
public class PictureEditHandler extends TextWebSocketHandler {

    /**
     * 正在编辑图片的用户映射，key 为图片ID，value 为用户ID
     */
    private final Map<Long, Long> pictureEditingUsers = new ConcurrentHashMap<>();

    /**
     * 图片对应的 WebSocket 会话映射，key 为图片ID，value 为 WebSocketSession 集合
     */
    private final Map<Long, Set<WebSocketSession>> pictureSessions = new ConcurrentHashMap<>();

    @Resource
    private UserService userService;

    @Resource
    @Lazy
    private PictureEditEventProducer pictureEditEventProducer;

    /**
     * 连接建立成功后调用
     *
     * @param session
     * @throws Exception
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
//        保存连接到会话
        User user = (User) session.getAttributes().get("user");
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        pictureSessions.putIfAbsent(pictureId, ConcurrentHashMap.newKeySet());
        pictureSessions.get(pictureId).add(session);
//        构造响应，发送加入编辑的消息通知
        PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
        responseMessage.setType(PictureEditMessageTypeEnum.INFO.getValue());
        String message = String.format("用户 %s 加入了图片编辑", user.getUserName());
        responseMessage.setMessage(message);
        responseMessage.setUser(userService.getUserVO(user));
//        广播给其他用户
        broadcastToPicture(pictureId, responseMessage, session);
    }

    /**
     * 收到消息后，根据类型处理
     * @param session
     * @param message
     * @throws Exception
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
//        获取消息内容，将json转换为对象
        String payload = message.getPayload();
        PictureEditRequestMessage pictureEditRequestMessage = JSONUtil.toBean(payload, PictureEditRequestMessage.class);
//        根据类型处理
        String type = pictureEditRequestMessage.getType();
        PictureEditMessageTypeEnum pictureEditMessageTypeEnum = PictureEditMessageTypeEnum.getEnumByValue(type);
//      从session获取公共属性
        Long pictureId = (Long) session.getAttributes().get("pictureId");
        User user = (User) session.getAttributes().get("user");

//生产消息到disruptor队列中
       pictureEditEventProducer.publishEvent(pictureEditRequestMessage,session,user,pictureId);

    }

    /**
     * 进入编辑消息
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void handleEnterEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) {
//    没有用户正在编辑图片，才能进入
        if (!pictureEditingUsers.containsKey(pictureId)) {
            pictureEditingUsers.put(pictureId, user.getId());
//        构造响应，发送进入编辑的消息通知
            PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
            responseMessage.setType(PictureEditMessageTypeEnum.ENTER_EDIT.getValue());
            String message = String.format("用户 %s 进入了图片编辑状态", user.getUserName());
            responseMessage.setMessage(message);
            responseMessage.setUser(userService.getUserVO(user));
//        广播给其他用户
            try {
                broadcastToPicture(pictureId, responseMessage);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 编辑操作消息
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void handleEditActionMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) {
//        正在编辑的用户
        Long editingUserId = pictureEditingUsers.get(pictureId);
        String editAction = pictureEditRequestMessage.getEditAction();
        PictureEditActionEnum pictureEditActionEnum = PictureEditActionEnum.getEnumByValue(editAction);
        if(pictureEditActionEnum==null)
        {
            log.error("编辑操作类型错误: {}", editAction);
            return;
        }
        if (editingUserId != null && editingUserId.equals(user.getId())) {
//        构造响应，发送编辑操作的消息通知
            PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
            responseMessage.setType(PictureEditMessageTypeEnum.EDIT_ACTION.getValue());
            String message = String.format("用户 %s 执行了%s操作", user.getUserName(),pictureEditActionEnum.getText());
            responseMessage.setMessage(message);
            responseMessage.setUser(userService.getUserVO(user));
//        广播给除了当前客户端的其他用户，因为当前客户端已经执行了操作，否额会重复编辑
            try {
                broadcastToPicture(pictureId, responseMessage, session);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 退出编辑消息
     * @param pictureEditRequestMessage
     * @param session
     * @param user
     * @param pictureId
     */
    public void handleExitEditMessage(PictureEditRequestMessage pictureEditRequestMessage, WebSocketSession session, User user, Long pictureId) {
//    判断当前用户是不是编辑者
        Long editingUserId = pictureEditingUsers.get(pictureId);
        if (editingUserId != null && editingUserId.equals(user.getId())) {
            pictureEditingUsers.remove(pictureId);
//        构造响应，发送退出编辑的消息通知
            PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
            responseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
            String message = String.format("用户 %s 退出了图片编辑状态", user.getUserName());
            responseMessage.setMessage(message);
            responseMessage.setUser(userService.getUserVO(user));
//        广播给其他用户
            try {
                broadcastToPicture(pictureId, responseMessage);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 关闭连接，释放资源
     * @param session
     * @param status
     * @throws Exception
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        User user=(User) session.getAttributes().get("user");
       Long pictureId= (Long) session.getAttributes().get("pictureId");
//        移除当前用户的编辑状态
        handleExitEditMessage(null, session,user,pictureId);
//        删除会话
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (CollUtil.isNotEmpty(sessionSet)) {
            sessionSet.remove(session);
            if (sessionSet.isEmpty()) {
                pictureSessions.remove(pictureId);
            }
        }
//        广播给其他用户，该用户已经离开
        PictureEditResponseMessage responseMessage = new PictureEditResponseMessage();
        responseMessage.setType(PictureEditMessageTypeEnum.EXIT_EDIT.getValue());
        String message = String.format("用户 %s 退出了图片编辑状态", user.getUserName());
        responseMessage.setMessage(message);
        responseMessage.setUser(userService.getUserVO(user));
//        广播给其他用户
        try {
            broadcastToPicture(pictureId, responseMessage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }

    /**
     * 广播图片编辑消息给其他用户
     *
     * @param pictureId
     * @param pictureEditResponseMessage
     * @param excludeSession
     * @throws IOException
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage, WebSocketSession excludeSession) throws IOException {
        Set<WebSocketSession> sessionSet = pictureSessions.get(pictureId);
        if (CollUtil.isNotEmpty(sessionSet)) {
//            解决精度丢失问题
            ObjectMapper objectMapper = new ObjectMapper();
            SimpleModule module = new SimpleModule();
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);
            objectMapper.registerModule(module);

            String message = objectMapper.writeValueAsString(pictureEditResponseMessage);
            TextMessage textMessage = new TextMessage(message);

            for (WebSocketSession session : sessionSet) {
//              排除掉自己
                if (excludeSession != null && session.equals(excludeSession)) {
                    continue;
                }
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }


            }
        }
    }

    /**
     * 广播图片编辑消息给其他用户
     *
     * @param pictureId
     * @param pictureEditResponseMessage
     * @throws Exception
     */
    private void broadcastToPicture(Long pictureId, PictureEditResponseMessage pictureEditResponseMessage) throws Exception {
        broadcastToPicture(pictureId, pictureEditResponseMessage, null);
    }

}
