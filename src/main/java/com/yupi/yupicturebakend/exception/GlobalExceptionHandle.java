package com.yupi.yupicturebakend.exception;

import com.yupi.yupicturebakend.common.BaseResponse;
import com.yupi.yupicturebakend.common.ResultUtils;
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
}
