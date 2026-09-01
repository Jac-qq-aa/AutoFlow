package io.autoflow.common.error;

import io.autoflow.common.api.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    ApiResponse<Void> business(BusinessException exception) {
        return ApiResponse.error(exception.code(), exception.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    ApiResponse<Void> accessDenied(AccessDeniedException exception) {
        return ApiResponse.error(exception.code(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiResponse<Void> validation(MethodArgumentNotValidException exception) {
        var first = exception.getBindingResult().getFieldErrors().stream().findFirst();
        return ApiResponse.error("VALIDATION_FAILED", first.map(e -> e.getField() + ": " + e.getDefaultMessage()).orElse("Invalid request"));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    ApiResponse<Void> unexpected(Exception exception) {
        log.error("Unexpected request failure", exception);
        return ApiResponse.error("INTERNAL_ERROR", "The request could not be completed");
    }
}
