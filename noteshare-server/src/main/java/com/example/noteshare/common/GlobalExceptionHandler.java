package com.example.noteshare.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<?>> handleBusiness(BusinessException e) {
        int code = e.getErrorCode().getCode();
        HttpStatus status = HttpStatus.OK; // 业务异常默认用 200 + code 区分
        if (code >= 50000) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        } else if (code == 40104) {
            // 40104 AUTH_FORBIDDEN → HTTP 403
            status = HttpStatus.FORBIDDEN;
        } else if (code >= 40100 && code < 40200) {
            // 其余 401xx → HTTP 401
            status = HttpStatus.UNAUTHORIZED;
        } else if (code >= 40300 && code < 40400) {
            // 403xx 笔记权限类 → HTTP 403
            status = HttpStatus.FORBIDDEN;
        }
        return ResponseEntity.status(status).body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
    }

    /** 参数校验异常（@Valid） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(ApiResponse.error(ErrorCode.PARAM_INVALID, msg));
    }

    /** 参数类型错误 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<?>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error(ErrorCode.PARAM_INVALID, "参数类型错误: " + e.getName()));
    }

    /** 请求体格式错误 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<?>> handleNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error(ErrorCode.PARAM_INVALID, "请求体格式错误"));
    }

    /** multipart 文件超过服务端限制 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<?>> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return ResponseEntity.ok(ApiResponse.error(ErrorCode.FILE_TOO_LARGE));
    }

    /** multipart 请求缺少 file 字段 */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<?>> handleMissingPart(MissingServletRequestPartException e) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ErrorCode.PARAM_INVALID, "缺少文件字段: " + e.getRequestPartName()));
    }

    /** multipart 请求解析失败 */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<?>> handleMultipart(MultipartException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error(ErrorCode.PARAM_INVALID, "文件上传请求格式错误"));
    }

    /** Spring Security 认证异常 */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthentication(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ErrorCode.AUTH_TOKEN_MISSING));
    }

    /** Spring Security 授权异常 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ErrorCode.AUTH_FORBIDDEN));
    }

    /** 兜底异常 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleAll(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR));
    }
}
