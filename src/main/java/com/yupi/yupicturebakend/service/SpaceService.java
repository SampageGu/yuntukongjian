package com.yupi.yupicturebakend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupicturebakend.model.dto.space.SpaceAddRequest;
import com.yupi.yupicturebakend.model.dto.space.SpaceQueryRequest;

import com.yupi.yupicturebakend.model.entity.Space;
import com.yupi.yupicturebakend.model.entity.Space;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupicturebakend.model.entity.User;
import com.yupi.yupicturebakend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author 49879
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2026-01-30 10:48:59
*/
public interface SpaceService extends IService<Space> {

    /**
     * 分页查询
     *
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);


    /**
     * 空间脱敏
     *
     * @param space
     * @param request
     * @return
     */
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request);


    /**
     * 获取空间包装类
     *
     * @param spacePage
     * @param request
     * @return
     */
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);


    /**
     * 判断空间是否合法
     *
     * @param space
     * @param add   是否为创建时检验
     */
    void validSpace(Space space, boolean add);

    /**
     * 根据空间级别填充对象
     *
     * @param space
     */
    public void fillSpaceBySpaceLevel(Space space);

    /**
     * 创建空间
     *
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    void checkSpaceAuth(User loginUser, Space space);
}
