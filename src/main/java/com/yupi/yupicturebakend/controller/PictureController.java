package com.yupi.yupicturebakend.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupicturebakend.annotation.AuthCheck;
import com.yupi.yupicturebakend.common.BaseResponse;
import com.yupi.yupicturebakend.common.DeleteRequest;
import com.yupi.yupicturebakend.common.ResultUtils;
import com.yupi.yupicturebakend.constant.UserConstant;
import com.yupi.yupicturebakend.exception.BusinessException;
import com.yupi.yupicturebakend.exception.ErrorCode;
import com.yupi.yupicturebakend.exception.ThrowUtils;
import com.yupi.yupicturebakend.model.dto.User.*;
import com.yupi.yupicturebakend.model.dto.picture.*;
import com.yupi.yupicturebakend.model.entity.Picture;
import com.yupi.yupicturebakend.model.entity.User;
import com.yupi.yupicturebakend.model.enums.PictureReviewStatusEnum;
import com.yupi.yupicturebakend.model.vo.LoginUserVO;
import com.yupi.yupicturebakend.model.vo.PictureTagCategory;
import com.yupi.yupicturebakend.model.vo.PictureVO;
import com.yupi.yupicturebakend.model.vo.UserVO;
import com.yupi.yupicturebakend.service.PictureService;
import com.yupi.yupicturebakend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.implementation.bytecode.Throw;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/picture")
@Slf4j
public class PictureController {

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;


    /**
     * 上传图片(可重复上传)
     *
     * @param multipartFile
     * @param pictureUploadRequest
     * @param request
     * @return
     */
    @PostMapping("/upload")
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request
    ) {
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }


    /**
     * 删除图片
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片不存在");
        }
        User loginUser = userService.getLoginUser(request);
        Long Id = deleteRequest.getId();
//        判断图片是否存在
        Picture picture = pictureService.getById(Id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
//        权限校验（仅本人和管理员可删除）
        if (!picture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
//        操作数据库
        boolean b = pictureService.removeById(Id);
        ThrowUtils.throwIf(!b, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(b);


    }

    /**
     * 更新图片
     *
     * @param pictureUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);

        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));

        pictureService.validPicture(picture);

        long id = pictureUpdateRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //        补充审核参数
        pictureService.fillReviewParams(picture, userService.getLoginUser(request));
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }


    /**
     * 图片查询请求
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id < 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(picture);
    }


    /**
     * 查询脱敏图片
     *
     * @param id
     * @param request
     * @return
     */
    @GetMapping("/get/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id < 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(id);

        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);

        return ResultUtils.success(pictureService.getPictureVO(picture, request));
    }


    /**
     * 分页查询
     *
     * @param pictureQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();

        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));

        return ResultUtils.success(picturePage);
    }


    /**
     * 分页查询（脱敏数据）
     *
     * @param pictureQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();

        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
//        普通用户只能看审核通过的图片
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));

        return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
    }


    /**
     * 编辑图片
     *
     * @param pictureEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);

        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));

        picture.setEditTime(new Date());

        pictureService.validPicture(picture);
        User loginUser = userService.getLoginUser(request);

        long id = pictureEditRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
//        补充审核参数
        pictureService.fillReviewParams(picture, loginUser);


        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }


    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }


    /**
     * 审核图片
     *
     * @param pictureReviewRequest
     * @param request
     * @return
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }


}

