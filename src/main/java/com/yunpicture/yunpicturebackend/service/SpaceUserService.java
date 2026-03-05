package com.yunpicture.yunpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yunpicture.yunpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.yunpicture.yunpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.yunpicture.yunpicturebackend.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yunpicture.yunpicturebackend.model.vo.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author 49879
* @description 针对表【space_user(空间用户关联)】的数据库操作Service
* @createDate 2026-02-03 11:06:59
*/
public interface SpaceUserService extends IService<SpaceUser> {



    /**
     * 空间成员脱敏
     *
     * @param spaceUser
     * @param request
     * @return
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);


    /**
     * 获取空间成员包装类
     *
     * @param spaceUserList
     * @return
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);


    /**
     * 判断空间成员是否合法
     *
     * @param spaceUser
     * @param add   是否为创建时检验
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);



    /**
     * 创建空间
     *
     * @param spaceUserAddRequest
     * @return
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 分页查询
     *
     * @param spaceUserQueryRequest
     * @return
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

}
