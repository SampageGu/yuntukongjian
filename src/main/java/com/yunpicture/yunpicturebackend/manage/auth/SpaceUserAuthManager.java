package com.yunpicture.yunpicturebackend.manage.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.yunpicture.yunpicturebackend.manage.auth.model.SpaceUserAuthConfig;
import com.yunpicture.yunpicturebackend.manage.auth.model.SpaceUserRole;
import com.yunpicture.yunpicturebackend.model.entity.Picture;
import com.yunpicture.yunpicturebackend.model.entity.Space;
import com.yunpicture.yunpicturebackend.model.entity.SpaceUser;
import com.yunpicture.yunpicturebackend.model.entity.User;
import com.yunpicture.yunpicturebackend.model.enums.SpaceRoleEnum;
import com.yunpicture.yunpicturebackend.model.enums.SpaceTypeEnum;
import com.yunpicture.yunpicturebackend.service.SpaceUserService;
import com.yunpicture.yunpicturebackend.service.UserService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component
public class SpaceUserAuthManager {

    // 从配置文件加载权限配置
    public static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;

    static {
        String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(json, SpaceUserAuthConfig.class);
    }

    @Resource
    private SpaceUserService spaceUserService;
    @Resource
    private UserService userService;

    /**
     * 根据角色获取权限列表
     * @param spaceUserRole
     * @return
     */
    public List<String> getPermissionsByRole(String spaceUserRole) {
        if (StrUtil.isBlank(spaceUserRole)) return new ArrayList<>();

        SpaceUserRole role = SPACE_USER_AUTH_CONFIG.getRoles().stream()
                .filter(r -> r.getKey().equals(spaceUserRole))
                .findFirst()
                .orElse(null);
        if (role == null) {
            return new ArrayList<>();
        } else {
            return role.getPermissions();
        }
    }


    /**
     * 获取用户在空间中的权限列表
     * @param space
     * @param picture
     * @param loginUser
     * @return
     */
    public List<String> getPermissionList(Space space, Picture picture, User loginUser) {
        if (loginUser == null) {
            return new ArrayList<>();
        }
        // 管理员权限
        List<String> ADMIN_PERMISSIONS = getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        // 公共图库
        if (space == null) {
            if (userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            }

            // 如果提供了图片对象，判断是否为本人上传
            if (picture != null && loginUser.getId().equals(picture.getUserId())) {
                return ADMIN_PERMISSIONS; // 本人上传，拥有管理权限
            }
//            否则，返回空
            return new ArrayList<>();

        }
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(space.getSpaceType());
        if (spaceTypeEnum == null) {
            return new ArrayList<>();
        }
        // 根据空间获取对应的权限
        switch (spaceTypeEnum) {
            case PRIVATE:
                // 私有空间，仅本人或管理员有所有权限
                if (space.getUserId().equals(loginUser.getId()) || userService.isAdmin(loginUser)) {
                    return ADMIN_PERMISSIONS;
                } else {
                    return new ArrayList<>();
                }
            case TEAM:
                // 团队空间，查询 SpaceUser 并获取角色和权限
                SpaceUser spaceUser = spaceUserService.lambdaQuery()
                        .eq(SpaceUser::getSpaceId, space.getId())
                        .eq(SpaceUser::getUserId, loginUser.getId())
                        .one();
                if (spaceUser == null) {
                    return new ArrayList<>();
                } else {
                    return getPermissionsByRole(spaceUser.getSpaceRole());
                }
        }
        return new ArrayList<>();
    }

}
