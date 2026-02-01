package com.yupi.yupicturebakend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.repository.AbstractRepository;
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
import com.yupi.yupicturebakend.model.dto.picture.*;
import com.yupi.yupicturebakend.model.entity.Picture;
import com.yupi.yupicturebakend.model.entity.Space;
import com.yupi.yupicturebakend.model.entity.User;
import com.yupi.yupicturebakend.model.enums.PictureReviewStatusEnum;
import com.yupi.yupicturebakend.model.vo.PictureVO;
import com.yupi.yupicturebakend.model.vo.UserVO;
import com.yupi.yupicturebakend.service.PictureService;
import com.yupi.yupicturebakend.mapper.PictureMapper;
import com.yupi.yupicturebakend.service.SpaceService;
import com.yupi.yupicturebakend.service.UserService;
import com.yupi.yupicturebakend.utils.ColorSimilarUtils;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.implementation.bytecode.Throw;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
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

    @Resource
    private SpaceService spaceService;

    @Resource
    private TransactionTemplate transactionTemplate;

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
//        校验空间是否存在
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            //        校验是否有空间的权限，仅空间管理员可以上传
            if (!user.getId().equals(space.getUserId()) && !userService.isAdmin(user)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
//            校验大小
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间条数不足");
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间大小不足");
            }

        }

//        判断是新增还是删除
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
//      如果是更新，判断图片是否存在
        Picture oldPicture = null;
        if (pictureId != null) {
            oldPicture = this.getById(pictureId);

            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
//            仅本人或管理员可编辑图片
            if (oldPicture.getUserId().equals(user.getId()) && !userService.isAdmin(user)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限");
            }
//            校验空间是否一致
//            没传spaceid，复用原有的spaceid(这样也兼容公共图库)
            if (spaceId == null) {
                if (oldPicture.getSpaceId() != null) {
                    spaceId = oldPicture.getSpaceId();
                }
            } else {
//                传了spaceid，必须和之前一致
                if (ObjUtil.notEqual(spaceId, oldPicture.getSpaceId())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间id不一致");
                }

            }
        }

//       上传图片
//        根据用户id划分目录=>按照空间划分目录
        String uploadPathPrefix;
        if (spaceId == null) {
//            公共图库
            uploadPathPrefix = String.format("public/%s", user.getId());
        } else {
//            空间
            uploadPathPrefix = String.format("space/%s", spaceId);
        }


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
        picture.setSpaceId(spaceId);

        BeanUtils.copyProperties(uploadPictureResult, picture);
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
//        支持外层上传图片名称
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && pictureUploadRequest.getPicName() != null) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setPicColor(uploadPictureResult.getPicColor());
        picture.setName(picName);
        picture.setUserId(user.getId());

//        如果picid不为空，是更新，否则是新增
        if (pictureId != null) {
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
//        补充审核参数
        this.fillReviewParams(picture, user);


        Long finalSpaceId = spaceId;

        transactionTemplate.execute(status -> {
            boolean saved = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(saved == false, ErrorCode.OPERATION_ERROR, "数据库操作失败");
//        更新空间的使用额度
            if (finalSpaceId != null) {

//            更新空间使用额度
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize=totalSize+" + picture.getPicSize())
                        .setSql("totalCount=totalCount+1")
                        .update();
                ThrowUtils.throwIf(update == false, ErrorCode.OPERATION_ERROR, "数据库额度更新失败");

            }
            return picture;
        });


//       如果是更新，清理图片资源
        if (oldPicture != null && !oldPicture.getUrl().equals(picture.getUrl())) {
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
        Long spaceId = pictureQueryRequest.getSpaceId();
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Date reviewTime = pictureQueryRequest.getReviewTime();
        int current = pictureQueryRequest.getCurrent();
        int pageSize = pictureQueryRequest.getPageSize();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();

        if (StrUtil.isNotBlank(searchText)) {
            queryWrapper.and(
                    qw -> qw.like("name", searchText)
                            .or()
                            .like("introduction", searchText)
            );
        }

        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.isNull(nullSpaceId, "spaceId");
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
//        >=startEditTime
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
//        <=endEditTime
        queryWrapper.ge(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);

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
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
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
            pictureUploadRequest.setPicName(namePrefix + (1 + uploadCount));
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
        String pictureUrl = oldPicture.getUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
//        不止一条记录用到了该数据
        if (count > 1) {
            return;
        }
//        删除图片
        cosManage.deleteObject(pictureUrl);
//        删除缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();

        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManage.deleteObject(thumbnailUrl);
        }

    }

    /**
     * 校验空间图片的权限
     *
     * @param loginUser
     * @param picture
     */
    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        Long userId = loginUser.getId();
        if (spaceId == null) {
//            公共图库，仅本人和管理员可操作
            if (!picture.getUserId().equals(userId) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }

        } else
//        私有空间，仅空间管理员可操作
        {
            if (!picture.getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
    }

    /**
     * 删除图片
     *
     * @param pictureId
     * @param loginUser
     */
    @Override
    public void deletePicture(long pictureId, User loginUser) {
        ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        Picture oldPicture = this.getById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        checkPictureAuth(loginUser, oldPicture);

        //        删除图片开启事务
        transactionTemplate.execute(status -> {

            boolean result = this.removeById(pictureId);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
//            更新空间使用额度
            boolean update = spaceService.lambdaUpdate()
                    .eq(Space::getId, oldPicture.getSpaceId())
                    .setSql("totalSize=totalSize-" + oldPicture.getPicSize())
                    .setSql("totalCount=totalCount-1")
                    .update();
            ThrowUtils.throwIf(update == false, ErrorCode.OPERATION_ERROR, "数据库额度更新失败");
            return true;
        });


        this.clearPictureFile(oldPicture);
    }

    /**
     * 编辑图片
     *
     * @param pictureEditRequest
     * @param loginUser
     */
    @Override
    public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {

        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);

        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));

        picture.setEditTime(new Date());

        this.validPicture(picture);

        long id = pictureEditRequest.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        checkPictureAuth(loginUser, oldPicture);

        this.fillReviewParams(picture, loginUser);

        boolean result = this.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }

    /**
     * 根据图片搜索类似颜色图片
     *
     * @param spaceId
     * @param picColor
     * @param loginUser
     * @return
     */
    @Override
    public List<PictureVO> searchPictureBycolor(Long spaceId, String picColor, User loginUser) {
//   1、校验参数
        ThrowUtils.throwIf(spaceId == null || StrUtil.isBlank(picColor), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
//        2、校验权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NO_AUTH_ERROR, "空间不存在");
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问该空间");
        }
//        3、查询该空间下的所有图片
        List<Picture> pictureList = this.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .isNotNull(Picture::getPicColor)
                .list();
//        如果没有图片，直接返回空列表
        if (CollUtil.isEmpty(pictureList)) {
            return new ArrayList<>();
        }
//        4、将颜色字符串转为主色调
        Color targetColor = Color.decode(picColor);
//        计算相似度排序
        List<Picture> sortedPictureList = pictureList.stream().sorted(Comparator.comparingDouble(picture -> {
                    String hexColor = picture.getPicColor();
//            没有主色调的图片默认排序到最后
                    if (StrUtil.isBlank(hexColor)) {
                        return Double.MAX_VALUE;
                    }
                    Color pictureColor = Color.decode(hexColor);
                    return ColorSimilarUtils.calculateSimilarity(targetColor, pictureColor);
                })).limit(12)
                .collect(Collectors.toList());
//        5、返回结果
        return sortedPictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
    }

    /**
     * 批量编辑图片
     *
     * @param pictureEditByBatchRequest
     * @param loginUser
     */
    @Override
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
//        1、获取和校验参数
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        String category = pictureEditByBatchRequest.getCategory();
        List<String> tags = pictureEditByBatchRequest.getTags();
        String nameRule = pictureEditByBatchRequest.getNameRule();
        ThrowUtils.throwIf(CollUtil.isEmpty(pictureIdList), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.PARAMS_ERROR);
//        2、校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (!space.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
//        3、查询指定图片是否存在（仅选择需要的字段）
        List<Picture> pictureList = this.lambdaQuery()
                .select(Picture::getId, Picture::getSpaceId)
                .eq(Picture::getSpaceId, spaceId)
                .in(Picture::getId, pictureIdList)
                .list();
        if (pictureList.isEmpty()) return;
//        4、更新分类和标签
        pictureList.forEach(
                picture -> {
                    if (StrUtil.isNotBlank(category)) {
                        picture.setCategory(category);
                    }
                    if (CollUtil.isNotEmpty(tags)) {
                        picture.setTags(JSONUtil.toJsonStr(tags));
                    }
                }
        );
//        批量重命名
        fillPictureWithNameRule(pictureList, nameRule);
//        5、操作数据库进行批量更新
        boolean b = this.updateBatchById(pictureList);
        ThrowUtils.throwIf(!b, ErrorCode.OPERATION_ERROR, "编辑数据库失败");
    }

    /**
     * nameRule 格式：图片{序号}
     *
     * @param pictureList
     * @param nameRule
     */
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if (StrUtil.isBlank(nameRule) || CollUtil.isEmpty(pictureList)) return;

        long count = 1;
        try {
            for (Picture picture : pictureList) {
                String pictureName = nameRule.replaceAll("\\{序号}", String.valueOf(count++));
                picture.setName(pictureName);
                log.info("pictureName"+pictureName);
            }
        } catch (Exception e) {
            log.error("名称解析错误");
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "名称解析错误");
        }


    }


}




