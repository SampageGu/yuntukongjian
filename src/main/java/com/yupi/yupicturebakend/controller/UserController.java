package com.yupi.yupicturebakend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupicturebakend.annotation.AuthCheck;
import com.yupi.yupicturebakend.common.BaseResponse;
import com.yupi.yupicturebakend.common.DeleteRequest;
import com.yupi.yupicturebakend.common.ResultUtils;
import com.yupi.yupicturebakend.constant.UserConstant;
import com.yupi.yupicturebakend.exception.ErrorCode;
import com.yupi.yupicturebakend.exception.ThrowUtils;
import com.yupi.yupicturebakend.model.dto.User.*;
import com.yupi.yupicturebakend.model.entity.User;
import com.yupi.yupicturebakend.model.vo.LoginUserVO;
import com.yupi.yupicturebakend.model.vo.UserVO;
import com.yupi.yupicturebakend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     *
     * @return
     */
    @PostMapping("/register")

    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {

        log.info("userRegisterController" + userRegisterRequest);
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String password = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long result = userService.userRegister(userAccount, password, checkPassword);
        return ResultUtils.success(result);

    }

    /**
     * 用户登录
     *
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest httpServletRequest) {

        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();


        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, httpServletRequest);
        return ResultUtils.success(loginUserVO);

    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 用户注销
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest httpServletRequest) {

        ThrowUtils.throwIf(httpServletRequest == null, ErrorCode.NOT_FOUND_ERROR);
        boolean userLogout = userService.userLogout(httpServletRequest);
        return ResultUtils.success(userLogout);

    }
    /**
     * 创建用户
     *
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {

        ThrowUtils.throwIf(userAddRequest==null,ErrorCode.PARAMS_ERROR);
        User user=new User();
        BeanUtils.copyProperties(userAddRequest,user);
        final String DEFAULT_PASSWORD="12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean res = userService.save(user);
        ThrowUtils.throwIf(!res,ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());

    }

    /**
     * 管理员查询用户
     * @param id
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(Long id) {

        ThrowUtils.throwIf(id<0,ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user==null,ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);

    }

    /**
     * 根据id获取包装类
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(Long id) {

        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        UserVO userVO = userService.getUserVO(user);
        return ResultUtils.success(userVO);

    }

    /**
     * 删除用户
     *
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> addUser(@RequestBody DeleteRequest deleteRequest) {

        ThrowUtils.throwIf((deleteRequest==null||deleteRequest.getId()<=0),ErrorCode.PARAMS_ERROR);

        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);

    }
    /**
     * 更新用户信息
     *
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {

        ThrowUtils.throwIf(userUpdateRequest==null||userUpdateRequest.getId()==null,ErrorCode.PARAMS_ERROR);
        User user=new User();
        BeanUtils.copyProperties(userUpdateRequest,user);

        boolean b = userService.updateById(user);
        ThrowUtils.throwIf(!b,ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(b);

    }

    /**
     * 分页获取用户封装列表
     * @param userQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>>listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest)

    {
        ThrowUtils.throwIf(userQueryRequest==null,ErrorCode.PARAMS_ERROR);
        int current = userQueryRequest.getCurrent();
        int pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, pageSize), userService.getQueryWrapper(userQueryRequest));
        Page<UserVO>userVOPage=new Page<>(current,pageSize,userPage.getTotal());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);

    }

}
