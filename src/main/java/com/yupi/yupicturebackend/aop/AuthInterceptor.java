package com.yupi.yupicturebackend.aop;

import com.yupi.yupicturebackend.annotation.AuthCheck;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.enums.UserRoleEnum;
import com.yupi.yupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
@Slf4j
public class AuthInterceptor {

    @Resource
    private UserService userService;


    /**
     * 执行拦截
     *
     * @param joinPoint
     * @param authCheck
     * @return
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
//        获取当前用户
        User loginUser = userService.getLoginUser(request);
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);

//        如果不需要权限，放行
        if (mustRoleEnum == null) {
            return joinPoint.proceed();
        }
//        必须有权限，才会通过以下代码

        UserRoleEnum enumByValue = UserRoleEnum.getEnumByValue(loginUser.getUserRole());

        if (enumByValue == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
//        要求必须有管理员权限，但用户没有
        if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(enumByValue)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
//        通过权限校验
        return joinPoint.proceed();
    }
}
