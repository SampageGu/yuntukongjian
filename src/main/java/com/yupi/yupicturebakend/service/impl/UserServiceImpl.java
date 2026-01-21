package com.yupi.yupicturebakend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicturebakend.exception.BusinessException;
import com.yupi.yupicturebakend.exception.ErrorCode;
import com.yupi.yupicturebakend.model.dto.User.UserQueryRequest;
import com.yupi.yupicturebakend.model.entity.User;
import com.yupi.yupicturebakend.model.enums.UserRoleEnum;
import com.yupi.yupicturebakend.model.vo.LoginUserVO;
import com.yupi.yupicturebakend.model.vo.UserVO;
import com.yupi.yupicturebakend.service.UserService;
import com.yupi.yupicturebakend.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import javax.swing.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.yupi.yupicturebakend.constant.UserConstant.USER_LOGIN_STATE;

/**
 * @author 49879
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2026-01-19 11:00:41
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    /**
     * 用户注册
     *
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
//        1、校验参数
        log.info("用户注册参数：userAccount={}, password={}, checkPassword={}",
                userAccount, userPassword, checkPassword);
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次密码不一致");
        }
//        2、检查是否与数据库中已有的重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        Long count = this.baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
//        3、密码加密
        String encryptPassword = getEncryptPassword(userPassword);

//        4、插入数据到数据库
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("佚名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }

        return user.getId();
    }

    /**
     * 获取加密后的密码
     *
     * @param userPassword
     * @return
     */
    @Override
    public String getEncryptPassword(String userPassword) {
//        加盐 混淆密码
        final String SALT = "gjj";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    /**
     * 用户登录
     *
     * @param userAccount
     * @param userPassword
     * @param httpServletRequest
     * @return 返回脱敏后的用户信息
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest httpServletRequest) {
//       1、校验
        log.info("userAccount:" + userAccount + "userPassword:" + userPassword);
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码过短");
        }

//        2、对密码加密
        String encryptPassword = getEncryptPassword(userPassword);

//        3、查数据库是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
//        4、不存在抛异常
        if (user == null) {
            log.info("user login failed,userAccount cannot match password");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或者密码错误");
        }
//        5、保存用户登录态
        httpServletRequest.getSession().setAttribute(USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    /**
     * 获得脱敏后的登录用户信息
     *
     * @param user
     * @return
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) return null;
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 获取脱敏后的用户信息
     *
     * @param user
     * @return
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) return null;
        UserVO UserVO = new UserVO();
        BeanUtils.copyProperties(user, UserVO);
        return UserVO;
    }

    /**
     * 获取脱敏后的用户信息列表
     *
     * @param userlist
     * @return
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userlist) {
        if (CollUtil.isEmpty(userlist)) {
            return new ArrayList<>();
        }

        return userlist.stream()
                .map(this::getUserVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
//       判断是否登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        if (user == null || user.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
//        从数据库再查一遍（追求性能的话直接返回上述结果）
        Long userId = user.getId();
        User id = this.getById(userId);
        if (id == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return user;
    }

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        //       判断是否登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);

        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User>queryWrapper=new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id),"id",id);
        queryWrapper.like(ObjUtil.isNotNull(userAccount),"userAccount",userAccount);
        queryWrapper.eq(ObjUtil.isNotNull(userRole),"userRole",userRole);
        queryWrapper.like(ObjUtil.isNotNull(userName),"userName",userName);
        queryWrapper.like(ObjUtil.isNotNull(userProfile),"userProfile",userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;

    }
}




