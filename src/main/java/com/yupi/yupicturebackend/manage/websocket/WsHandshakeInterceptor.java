package com.yupi.yupicturebackend.manage.websocket;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.yupi.yupicturebackend.manage.auth.SpaceUserAuthManager;
import com.yupi.yupicturebackend.manage.auth.model.SpaceUserPermissionConstant;
import com.yupi.yupicturebackend.model.entity.Picture;
import com.yupi.yupicturebackend.model.entity.Space;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.enums.SpaceTypeEnum;
import com.yupi.yupicturebackend.service.PictureService;
import com.yupi.yupicturebackend.service.SpaceService;
import com.yupi.yupicturebackend.service.SpaceUserService;
import com.yupi.yupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * WebSocket 握手拦截器
 */
@Slf4j
@Component
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 建立连接之前的拦截器
     * @param request
     * @param response
     * @param wsHandler
     * @param attributes 给 WebSocketSession 设置属性
     * @return
     * @throws Exception
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
//       获取当前登录用户
        if (request instanceof ServerHttpRequest) {
//            拿到httpservletRequest
            HttpServletRequest httpServletRequest = ((ServletServerHttpRequest) request).getServletRequest();
//            从请求中获取信息
            String pictureId = httpServletRequest.getParameter("pictureId");
            if (StrUtil.isBlank(pictureId)) {
                log.error("图片为空，拒绝握手请求");
                return false;
            }
//            获取用户信息
            User loginUser = userService.getLoginUser(httpServletRequest);
            if (ObjUtil.isEmpty(loginUser)) {
                log.error("用户未登录，拒绝握手请求");
                return false;
            }
//            校验用户是否有编辑图片权限

//            获取图片信息
            Picture picture = pictureService.getById(pictureId);
            if (ObjUtil.isEmpty(picture)) {
                log.error("图片为空，拒绝握手请求");
                return false;
            }
            Long spaceId = picture.getSpaceId();
            Space space = null;
            if (spaceId != null) {
                space = spaceService.getById(spaceId);
                if (ObjUtil.isEmpty(space)) {
                    log.error("空间不存在，拒绝握手请求");
                    return false;
                }
                //        如果不是团队空间，无权限
                if (space.getSpaceType() != SpaceTypeEnum.TEAM.getValue()) {
                    log.error("非团队空间，拒绝握手请求");
                    return false;
                }
            }
            //        校验用户是否有编辑权限，如果没有权限则返回 false，拒绝握手请求
            List<String> permissionList = spaceUserAuthManager.getPermissionList(space,new Picture(), loginUser);
            if (!permissionList.contains(SpaceUserPermissionConstant.PICTURE_EDIT)) {
                log.error("用户无编辑权限，拒绝握手请求");
                return false;
            }
            //        将用户登录状态存储到 attributes 中
            attributes.put("user", loginUser);
            attributes.put("pictureId", Long.valueOf(pictureId));
            attributes.put("userId", loginUser.getId());





        }

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
