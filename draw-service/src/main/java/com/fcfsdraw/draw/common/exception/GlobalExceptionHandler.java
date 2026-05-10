package com.fcfsdraw.draw.common.exception;

import com.fcfsdraw.draw.common.response.ApiResponse;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.redisson.client.RedisConnectionException;
import org.redisson.client.RedisTimeoutException;
import org.slf4j.MDC;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TRACE_ID_KEY = "traceId";

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.errorCode();
        log.warn("business exception occurred. code={}, message={}", errorCode.code(), exception.getMessage());

        return ResponseEntity.status(errorCode.status())
                .body(ApiResponse.failure(errorCode.code(), new ErrorResponse(errorCode.message(), currentTraceId())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(ErrorCode.INVALID_REQUEST.message());

        log.warn("validation exception occurred. message={}", message);

        return ResponseEntity.status(ErrorCode.INVALID_REQUEST.status())
                .body(ApiResponse.failure(ErrorCode.INVALID_REQUEST.code(), new ErrorResponse(message, currentTraceId())));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleConstraintViolationException(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations()
                .stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse(ErrorCode.INVALID_REQUEST.message());

        log.warn("constraint violation occurred. message={}", message);

        return ResponseEntity.status(ErrorCode.INVALID_REQUEST.status())
                .body(ApiResponse.failure(ErrorCode.INVALID_REQUEST.code(), new ErrorResponse(message, currentTraceId())));
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiResponse<ErrorResponse>> handleNotFoundException(Exception exception) {
        log.warn("not found exception occurred. message={}", exception.getMessage());

        return ResponseEntity.status(ErrorCode.NOT_FOUND.status())
                .body(ApiResponse.failure(ErrorCode.NOT_FOUND.code(), new ErrorResponse(ErrorCode.NOT_FOUND.message(), currentTraceId())));
    }

    @ExceptionHandler({
            RedisConnectionException.class,
            RedisTimeoutException.class,
            CannotAcquireLockException.class,
            PessimisticLockingFailureException.class,
            CannotCreateTransactionException.class,
            QueryTimeoutException.class,
            TransactionSystemException.class,
            LockTimeoutException.class,
            PessimisticLockException.class,
            JpaSystemException.class,
            PersistenceException.class
    })
    public ResponseEntity<ApiResponse<ErrorResponse>> handleLockException(Exception exception) {
        Throwable rootCause = rootCause(exception);
        log.warn(
                "lock acquisition delayed or failed. exceptionType={}, rootCauseType={}, message={}",
                exception.getClass().getName(),
                rootCause.getClass().getName(),
                rootCause.getMessage()
        );

        return ResponseEntity.status(ErrorCode.LOCK_TIMEOUT.status())
                .body(ApiResponse.failure(ErrorCode.LOCK_TIMEOUT.code(), new ErrorResponse(ErrorCode.LOCK_TIMEOUT.message(), currentTraceId())));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleException(Exception exception, HttpServletRequest request) {
        Throwable rootCause = rootCause(exception);
        log.error(
                "unhandled exception occurred. uri={}, exceptionType={}, rootCauseType={}, rootCauseMessage={}",
                request.getRequestURI(),
                exception.getClass().getName(),
                rootCause.getClass().getName(),
                rootCause.getMessage(),
                exception
        );

        return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.status())
                .body(ApiResponse.failure(
                        ErrorCode.INTERNAL_SERVER_ERROR.code(),
                        new ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR.message(), currentTraceId())
                ));
    }

    private String currentTraceId() {
        return Optional.ofNullable(MDC.get(TRACE_ID_KEY)).orElse("");
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }
}
