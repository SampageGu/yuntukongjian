package com.yunpicture.yunpicturebackend.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.yunpicture.yunpicturebackend.common.BaseResponse;
import com.yunpicture.yunpicturebackend.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandle {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?>businessExceptionHandle(BusinessException businessException)
    {
        log.error("BusinessException",businessException);
        return ResultUtils.error(businessException.getCode(),businessException.getMessage());
    }


    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?>businessExceptionHandle(RuntimeException e)
    {
        log.error("RuntimeException",e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR,"系统错误");
    }

    /**
     * 未登录异常
     * @param e
     * @return
     */
    @ExceptionHandler(NotLoginException.class)
    public BaseResponse<?> notLoginException(NotLoginException e) {
        log.error("NotLoginException", e);
        return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR, e.getMessage());
    }

    /**
     * 无权限异常
     * @param e
     * @return
     */
    @ExceptionHandler(NotPermissionException.class)
    public BaseResponse<?> notPermissionExceptionHandler(NotPermissionException e) {
        log.error("NotPermissionException", e);
        return ResultUtils.error(ErrorCode.NO_AUTH_ERROR, e.getMessage());
    }
}
