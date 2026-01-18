package com.yupi.yupicturebakend.exception;

import lombok.Getter;
import org.springframework.web.bind.annotation.GetMapping;

@Getter
public class BusinessException extends RuntimeException{

    private final int code;

    public BusinessException(int code,String message)
    {
        super(message);
        this.code=code;
    }
    public BusinessException(ErrorCode errorCode)
    {
        super(errorCode.getMessage());
        this.code=errorCode.getCode();
    }
    public BusinessException(ErrorCode errorCode,String message)
    {
        super(message);
        this.code=errorCode.getCode();
    }
}
