package com.tkck.trigger.http.handler;

import com.tkck.api.response.Response;
import com.tkck.domain.agent.service.runtime.resilience.ExecutionErrorCode;
import com.tkck.types.enums.ResponseCode;
import com.tkck.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public Response<String> handleAppException(AppException exception) {
        log.warn("app exception, code={}, info={}", exception.getCode(), exception.getInfo());
        return Response.<String>builder()
                .code(exception.getCode())
                .info(exception.getInfo() == null ? "业务处理失败" : exception.getInfo())
                .data(null)
                .build();
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Response<String> handleMaxUploadSize(MaxUploadSizeExceededException exception) {
        return Response.<String>builder()
                .code(ExecutionErrorCode.INVALID_REQUEST.getCode())
                .info("上传文件过大，请压缩后重试")
                .data(null)
                .build();
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public Response<String> handleDataNotFound(EmptyResultDataAccessException exception) {
        return Response.<String>builder()
                .code(ExecutionErrorCode.DATA_NOT_FOUND.getCode())
                .info("请求的数据不存在或已失效")
                .data(null)
                .build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Response<String> handleIllegalArgument(IllegalArgumentException exception) {
        return Response.<String>builder()
                .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                .info(exception.getMessage())
                .data(null)
                .build();
    }

    @ExceptionHandler(Exception.class)
    public Response<String> handleException(Exception exception) {
        log.error("unhandled http exception", exception);
        return Response.<String>builder()
                .code(ResponseCode.UN_ERROR.getCode())
                .info("系统繁忙，请稍后重试")
                .data(null)
                .build();
    }
}
