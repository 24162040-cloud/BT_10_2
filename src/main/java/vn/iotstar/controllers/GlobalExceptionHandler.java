package vn.iotstar.controllers;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail authentication(AuthenticationException ex) {
        return ex instanceof AccountStatusException
                ? ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Tài khoản không hoạt động hoặc bị khóa")
                : ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng");
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail forbidden(AccessDeniedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Không có quyền truy cập");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail conflict(DataIntegrityViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Dữ liệu bị trùng hoặc không hợp lệ");
    }
}
