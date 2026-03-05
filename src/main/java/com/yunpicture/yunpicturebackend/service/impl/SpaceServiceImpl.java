package com.yunpicture.yunpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yunpicture.yunpicturebackend.exception.BusinessException;
import com.yunpicture.yunpicturebackend.exception.ErrorCode;
import com.yunpicture.yunpicturebackend.exception.ThrowUtils;
import com.yunpicture.yunpicturebackend.model.dto.space.SpaceAddRequest;
import com.yunpicture.yunpicturebackend.model.dto.space.SpaceQueryRequest;
import com.yunpicture.yunpicturebackend.model.entity.Space;
import com.yunpicture.yunpicturebackend.model.entity.SpaceUser;
import com.yunpicture.yunpicturebackend.model.entity.User;
import com.yunpicture.yunpicturebackend.model.enums.SpaceLevelEnum;
import com.yunpicture.yunpicturebackend.model.enums.SpaceRoleEnum;
import com.yunpicture.yunpicturebackend.model.enums.SpaceTypeEnum;
import com.yunpicture.yunpicturebackend.model.vo.SpaceVO;
import com.yunpicture.yunpicturebackend.model.vo.UserVO;
import com.yunpicture.yunpicturebackend.service.SpaceService;
import com.yunpicture.yunpicturebackend.mapper.SpaceMapper;
import com.yunpicture.yunpicturebackend.service.SpaceUserService;
import com.yunpicture.yunpicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 49879
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2026-01-30 10:48:59
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceService {

    @Resource
    private UserService userService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private SpaceUserService spaceUserService;

//    为了方便部署，注释掉
//    @Resource
//    @Lazy
//    private DynamicShardingManager dynamicShardingManager;

    /**
     * 分页查询
     *
     * @param spaceQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {

        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        int current = spaceQueryRequest.getCurrent();
        int pageSize = spaceQueryRequest.getPageSize();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        Integer spaceType = spaceQueryRequest.getSpaceType();


        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);

        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
//      增加空间类型查询
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceType), " spaceType", spaceType);

        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 空间脱敏
     *
     * @param space
     * @param request
     * @return
     */
    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        SpaceVO spaceVO = SpaceVO.objToVo(space);

        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }
        return spaceVO;
    }

    /**
     * 获取空间包装类
     *
     * @param spacePage
     * @param request
     * @return
     */
    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }

        List<SpaceVO> spaceVOList = spaceList.stream().map(SpaceVO::objToVo).collect(Collectors.toList());

        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userService.getUserVO(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    /**
     * 判断空间是否合法
     *
     * @param space
     */

    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);

        Long id = space.getId();
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        Long maxSize = space.getMaxSize();
        Long maxCount = space.getMaxCount();
        Long totalSize = space.getTotalSize();
        Long totalCount = space.getTotalCount();
        Long userId = space.getUserId();
        Date createTime = space.getCreateTime();
        Date editTime = space.getEditTime();
        Date updateTime = space.getUpdateTime();
        Integer isDelete = space.getIsDelete();

        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        Integer spaceType = space.getSpaceType();
        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);

//        创建时校验
        if (add) {
            ThrowUtils.throwIf(StrUtil.isBlank(space.getSpaceName()),
                    ErrorCode.PARAMS_ERROR, "空间名称不能为空");

            ThrowUtils.throwIf(space.getSpaceLevel() == null,
                    ErrorCode.PARAMS_ERROR, "空间级别不能为空");

            ThrowUtils.throwIf(spaceTypeEnum == null,
                    ErrorCode.PARAMS_ERROR, "空间类别不能为空");

            ThrowUtils.throwIf(space.getId() != null,
                    ErrorCode.PARAMS_ERROR, "非法参数");

            ThrowUtils.throwIf(space.getTotalSize() != null && space.getTotalSize() > 0,
                    ErrorCode.PARAMS_ERROR, "初始已用容量非法");

            ThrowUtils.throwIf(space.getTotalCount() != null && space.getTotalCount() > 0,
                    ErrorCode.PARAMS_ERROR, "初始已用数量非法");
        }
//        修改数据时，空间名称校验
        if (!add) {
            ThrowUtils.throwIf(space.getId() == null,
                    ErrorCode.PARAMS_ERROR, "空间ID不能为空");
            if (space.getMaxSize() != null && space.getTotalSize() != null) {
                ThrowUtils.throwIf(
                        space.getMaxSize() < space.getTotalSize(),
                        ErrorCode.PARAMS_ERROR,
                        "空间最大容量不能小于已使用容量"
                );
            }

            if (space.getMaxCount() != null && space.getTotalCount() != null) {
                ThrowUtils.throwIf(
                        space.getMaxCount() < space.getTotalCount(),
                        ErrorCode.PARAMS_ERROR,
                        "空间最大图片数不能小于已使用数量"
                );
            }
            //        修改数据时，对空间级别进行校验
            if (spaceLevel != null && spaceLevelEnum == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
            }
        }


    }

    /**
     * 根据空间级别填充对象
     *
     * @param space
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            long maxCount = spaceLevelEnum.getMaxCount();
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }

        }
    }

    /**
     * 创建空间
     *
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {

//        填充参数默认值
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);
        if (space.getSpaceType() == null) {
            space.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
        if (StrUtil.isBlank(space.getSpaceName())) {
            space.setSpaceName("默认空间");
        }
        if (space.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
//        根据级别填充容量和大小
        this.fillSpaceBySpaceLevel(space);
//                校验参数
        validSpace(space, true);
//        校验权限，非管理员只能创建普通级别的空间
        Long userId = loginUser.getId();
        space.setUserId(userId);
        if (SpaceLevelEnum.COMMON.getValue() != space.getSpaceLevel() && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别的空间");
        }
//                控制同一用户只能创建一个私有空间，或者团队空间
        String lock = String.valueOf(userId).intern();
        synchronized (lock) {
            Long newSpaceId = transactionTemplate.execute(status -> {
                //            判断是否已有空间
                boolean exists = this.lambdaQuery()
                        .eq(Space::getUserId, userId)
                        .eq(Space::getSpaceType, space.getSpaceType())
                        .exists();
//            如果已有空间，不能创建
                ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户每类空间只能创建一个");
//            否则能创建
                boolean save = this.save(space);
                ThrowUtils.throwIf(!save, ErrorCode.OPERATION_ERROR, "保存空间失败");
//              创建成功后，如果是团队空间，则自动添加空间成员
                if (space.getSpaceType().equals(SpaceTypeEnum.TEAM.getValue())) {
                    //添加空间成员，自己为管理员
                    SpaceUser spaceUser = new SpaceUser();
                    spaceUser.setSpaceId(space.getId());
                    spaceUser.setUserId(userId);
                    spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                    boolean result = spaceUserService.save(spaceUser);
                    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "添加空间成员失败");

                }

//                动态创建分表，仅对团队空间生效
//                dynamicShardingManager.createSpacePictureTable(space);

//            返回新写入的数据id

                return space.getId();
            });
            return newSpaceId;
        }


    }

    @Override
    public void checkSpaceAuth(User loginUser, Space space) {
        //        权限校验（仅本人和管理员可删除）
        if (!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
    }


}




