package com.yupi.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupicturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.yupi.yupicturebackend.model.dto.picture.*;

import com.yupi.yupicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.vo.PictureVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author 49879
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2026-01-22 10:41:16
 */
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     *
     * @param inputSource
     * @param pictureUploadRequest
     * @param user
     * @return
     */
    public PictureVO uploadPicture(Object inputSource,
                                   PictureUploadRequest pictureUploadRequest,
                                   User user);

    /**
     * 分页查询
     *
     * @param pictureQueryRequest
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);


    /**
     * 图片脱敏
     * @param picture
     * @param request
     * @return
     */
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request);


    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 判断图片是否合法
     * @param picture
     */
    void validPicture(Picture picture);


    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param user
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User user);

    /**
     * 填充审核参数
     *
     * @param picture
     * @param user
     */
    void fillReviewParams(Picture picture, User user);

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param user
     * @return
     */
    Integer upploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User user);


    /**
     * 清理图片
     * @param oldPicture
     */
    void clearPictureFile(Picture oldPicture);

    /**
     * 校验空间图片的权限
     *
     * @param loginUser
     * @param picture
     */
    void checkPictureAuth(User loginUser, Picture picture);

    /**
     * 删除图片
     *
     * @param pictureId
     * @param loginUser
     */
    void deletePicture(long pictureId, User loginUser);


    /**
     * 编辑图片
     *
     * @param pictureEditRequest
     * @param loginUser
     */
    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);


    /**
     * 根据图片搜索类似颜色图片
     *
     * @param spaceId
     * @param picColor
     * @param loginUser
     * @return
     */
    List<PictureVO> searchPictureBycolor(Long spaceId, String picColor, User loginUser);


    /**
     * 批量编辑图片
     *
     * @param pictureEditByBatchRequest
     * @param loginUser
     */
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);


    /**
     * ai绘图
     *
     * @param createPictureOutPaintingTaskRequest
     * @param loginUser
     */
    CreateOutPaintingTaskResponse createOutPaintingTaskResponse(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser);

}
