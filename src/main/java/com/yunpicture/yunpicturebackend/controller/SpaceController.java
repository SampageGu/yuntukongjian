package com.yunpicture.yunpicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yunpicture.yunpicturebackend.annotation.AuthCheck;
import com.yunpicture.yunpicturebackend.common.BaseResponse;
import com.yunpicture.yunpicturebackend.common.DeleteRequest;
import com.yunpicture.yunpicturebackend.common.ResultUtils;
import com.yunpicture.yunpicturebackend.constant.UserConstant;
import com.yunpicture.yunpicturebackend.exception.BusinessException;
import com.yunpicture.yunpicturebackend.exception.ErrorCode;
import com.yunpicture.yunpicturebackend.exception.ThrowUtils;
import com.yunpicture.yunpicturebackend.manage.auth.SpaceUserAuthManager;
import com.yunpicture.yunpicturebackend.model.dto.space.*;
import com.yunpicture.yunpicturebackend.model.dto.space.*;

import com.yunpicture.yunpicturebackend.model.entity.Picture;
import com.yunpicture.yunpicturebackend.model.entity.Space;
import com.yunpicture.yunpicturebackend.model.entity.User;

import com.yunpicture.yunpicturebackend.model.enums.SpaceLevelEnum;
import com.yunpicture.yunpicturebackend.model.vo.SpaceVO;
import com.yunpicture.yunpicturebackend.service.SpaceService;
import com.yunpicture.yunpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/space")
@Slf4j
public class SpaceController {

    @Resource
    private SpaceService spaceService;

    @Resource
    private UserService userService;
    @Autowired
    private SpaceUserAuthManager spaceUserAuthManager;


    /**
     * 删除空间
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {

        if (deleteRequest == null || deleteRequest.getId() < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间不存在");
        }
        User loginUser = userService.getLoginUser(request);
        Long Id = deleteRequest.getId();
//        判断空间是否存在
        Space space = spaceService.getById(Id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
//        权限校验（仅本人和管理员可删除）
        spaceService.checkSpaceAuth(loginUser, space);
//        if (!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
//            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
//        }
//        操作数据库
        boolean b = spaceService.removeById(Id);
        ThrowUtils.throwIf(!b, ErrorCode.OPERATION_ERROR);

        return ResultUtils.success(b);


    }

    /**
     * 更新空间
     *
     * @param spaceUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest, HttpServletRequest request) {
        if (spaceUpdateRequest == null || spaceUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Space space = new Space();
        BeanUtils.copyProperties(spaceUpdateRequest, space);


        spaceService.validSpace(space, false);
        spaceService.fillSpaceBySpaceLevel(space);
        long id = spaceUpdateRequest.getId();
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);

//      操作数据库
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }


    /**
     * 空间查询请求
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Space> getSpaceById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id < 0, ErrorCode.PARAMS_ERROR);
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(space);
    }


    /**
     * 查询脱敏空间
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get/vo")

    public BaseResponse<SpaceVO> getSpaceVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id < 0, ErrorCode.PARAMS_ERROR);
        Space space = spaceService.getById(id);

        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, new Picture(),userService.getLoginUser(request));
        SpaceVO spaceVO = spaceService.getSpaceVO(space, request);
        spaceVO.setPermissionList(permissionList);
        return ResultUtils.success(spaceVO);
    }


    /**
     * 分页查询
     *
     * @param spaceQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();

        Page<Space> spacePage = spaceService.page(new Page<>(current, size),
                spaceService.getQueryWrapper(spaceQueryRequest));

        return ResultUtils.success(spacePage);
    }


    /**
     * 分页查询（脱敏数据）
     *
     * @param spaceQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<SpaceVO>> listSpaceVOByPage(@RequestBody SpaceQueryRequest spaceQueryRequest,
                                                         HttpServletRequest request) {
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();

        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        Page<Space> spacePage = spaceService.page(new Page<>(current, size),
                spaceService.getQueryWrapper(spaceQueryRequest));

        return ResultUtils.success(spaceService.getSpaceVOPage(spacePage, request));
    }


    /**
     * 编辑空间
     *
     * @param spaceEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditRequest spaceEditRequest, HttpServletRequest request) {
        if (spaceEditRequest == null || spaceEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Space space = new Space();
        BeanUtils.copyProperties(spaceEditRequest, space);


        space.setEditTime(new Date());

        spaceService.validSpace(space, false);
//        填充数据
        spaceService.fillSpaceBySpaceLevel(space);
        User loginUser = userService.getLoginUser(request);

        long id = spaceEditRequest.getId();
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        spaceService.checkSpaceAuth(loginUser, space);
//        if (!oldSpace.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
//            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
//        }


        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }


    /**
     * 新增空间
     *
     * @param spaceAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceAddRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        User loginUser = userService.getLoginUser(request);
        long newId = spaceService.addSpace(spaceAddRequest, loginUser);
        return ResultUtils.success(newId);


    }


    /**
     * 获取所有的空间级别，便于前端展示
     * @return
     */
    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values())
                .map(spaceLevelEnum -> new SpaceLevel(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()))
                .collect(Collectors.toList());
        return ResultUtils.success(spaceLevelList);
    }


}

