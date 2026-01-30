package com.yupi.yupicturebakend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicturebakend.exception.BusinessException;
import com.yupi.yupicturebakend.exception.ErrorCode;
import com.yupi.yupicturebakend.exception.ThrowUtils;
import com.yupi.yupicturebakend.manage.CosManage;
import com.yupi.yupicturebakend.manage.FileManage;
import com.yupi.yupicturebakend.manage.upload.FilePictureUpload;
import com.yupi.yupicturebakend.manage.upload.PictureUploadTemplate;
import com.yupi.yupicturebakend.manage.upload.UrlPictureUpload;
import com.yupi.yupicturebakend.model.dto.file.UploadPictureResult;
import com.yupi.yupicturebakend.model.dto.picture.PictureQueryRequest;
import com.yupi.yupicturebakend.model.dto.picture.PictureReviewRequest;
import com.yupi.yupicturebakend.model.dto.picture.PictureUploadByBatchRequest;
import com.yupi.yupicturebakend.model.dto.picture.PictureUploadRequest;
import com.yupi.yupicturebakend.model.entity.Picture;
import com.yupi.yupicturebakend.model.entity.User;
import com.yupi.yupicturebakend.model.enums.PictureReviewStatusEnum;
import com.yupi.yupicturebakend.model.vo.PictureVO;
import com.yupi.yupicturebakend.model.vo.UserVO;
import com.yupi.yupicturebakend.service.PictureService;
import com.yupi.yupicturebakend.mapper.PictureMapper;
import com.yupi.yupicturebakend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 49879
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2026-01-22 10:41:16
 */
@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {


    @Resource
    private FileManage fileManage;

    @Resource
    private UserService userService;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private FilePictureUpload filePictureUpload;
    @Autowired
    private CosManage cosManage;

    /**
     * 上传图片
     *
     * @param inputSource          文件输入源
     * @param pictureUploadRequest
     * @param user
     * @return
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User user) {
//      校验参数
        ThrowUtils.throwIf(user == null, ErrorCode.NO_AUTH_ERROR);
//        判断是新增还是删除
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
//      如果是更新，判断图片是否存在
        Picture oldPicture=null;
        if (pictureId != null) {
            oldPicture = this.getById(pictureId);

            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
//            仅本人或管理员可编辑图片
            if (oldPicture.getUserId().equals(user.getId()) && !userService.isAdmin(user)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限");
            }
        }

//       上传图片
//        根据用户id划分目录
        String uploadPathPrefix = String.format("public/%s", user.getId());

//        根据inputsourse 类型区分上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
//        构造入库图片信息
        //        补充审核参数
//        操作数据库
        Picture picture = new Picture();

        BeanUtils.copyProperties(uploadPictureResult, picture);
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
//        支持外层上传图片名称
        String picName = uploadPictureResult.getPicName();
        if(pictureUploadRequest!=null&&pictureUploadRequest.getPicName()!=null)
        {
            picName=pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setUserId(user.getId());

//        如果picid不为空，是更新，否则是新增
        if (pictureId != null) {
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
//        补充审核参数
        this.fillReviewParams(picture, user);
        boolean saved = this.saveOrUpdate(picture);


        ThrowUtils.throwIf(saved == false, ErrorCode.OPERATION_ERROR, "数据库操作失败");
//       如果是更新，清理图片资源
        if(oldPicture!=null&&!oldPicture.getUrl().equals(picture.getUrl()))
        {
        clearPictureFile(oldPicture);
        }

        return PictureVO.objToVo(picture);
    }

    /**
     * 分页查询
     *
     * @param pictureQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Date reviewTime = pictureQueryRequest.getReviewTime();
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();


        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and(
                    qw -> qw.like("name", searchText)
                            .or()
                            .like("introduction", searchText)
            );
        }

        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(ObjUtil.isNotEmpty(reviewMessage), "reviewMessage", reviewMessage);

        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 获取图片包装类（单条）
     *
     * @param picture
     * @param request
     * @return
     */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {

        PictureVO pictureVO = PictureVO.objToVo(picture);

        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    /**
     * 获取图片包装类（分页）
     *
     * @param picturePage
     * @param request
     * @return
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }

        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());

        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    /**
     * 图片校验
     *
     * @param picture
     */
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);

        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();

        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param user
     */
    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User user) {
//        1、校验参数
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum enumByValue = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        String reviewMessage = pictureReviewRequest.getReviewMessage();
        if (id == null || reviewStatus == null || PictureReviewStatusEnum.REVIEWING.equals(enumByValue)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
//        2、判断图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
//        3、校验审核状态是否重复
        if (oldPicture.getReviewStatus().equals(enumByValue)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "重复审核");
        }
//        4、数据库操作
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(user.getId());
        updatePicture.setReviewTime(new Date());
        boolean b = this.updateById(updatePicture);
        ThrowUtils.throwIf(!b, ErrorCode.OPERATION_ERROR, "数据库操作异常");
    }

    /**
     * 填充审核参数
     *
     * @param picture
     * @param user
     */
    @Override
    public void fillReviewParams(Picture picture, User user) {
        if (userService.isAdmin(user)) {
//            管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(user.getId());
            picture.setReviewTime(new Date());
            picture.setReviewMessage("管理员自动过审");
        } else {
//            非管理员
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }

    }

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param user
     * @return
     */
    @Override
    public Integer upploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User user) {
//       校验参数
        Integer count = pictureUploadByBatchRequest.getCount();
        String searchText = pictureUploadByBatchRequest.getSearchText();
//        名称前缀默认是关键词
        String namePrefix= pictureUploadByBatchRequest.getNamePrefix();
        if(StrUtil.isBlank(namePrefix))
        {
            namePrefix=searchText;
        }
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多30条");
//        抓取内容
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
//        解析内容
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isEmpty(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements imageElementList = div.select("img.mimg");
//        遍历元素，依次上传图片
        int uploadCount = 0;
        for (Element imageElement : imageElementList) {
            String fileUrl = imageElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，跳过:{}", fileUrl);
                continue;
            }
//            处理图片地址，防止转义或者和对象存储冲突
//            codefather.cn?yupi=dog 只保留codefather.cn
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            //        上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(fileUrl);
            pictureUploadRequest.setPicName(namePrefix+(1+uploadCount));
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, user);
                log.info("图片上传成功，id={}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e
            ) {
                log.error("图片上传失败");
            }
            if (uploadCount >= count) {
                break;
            }


        }


        return uploadCount;
    }

    /**
     * 清理图片
     *
     * @param oldPicture
     */
    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
//        1、判断该图片是否被多条记录使用
        String pictureUrl=oldPicture.getUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
//        不止一条记录用到了该数据
        if(count>1)
        {
            return;
        }
//        删除图片
        cosManage.deleteObject(pictureUrl);
//        删除缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();

        if(StrUtil.isNotBlank(thumbnailUrl))
        {
            cosManage.deleteObject(thumbnailUrl);
        }

    }
}




