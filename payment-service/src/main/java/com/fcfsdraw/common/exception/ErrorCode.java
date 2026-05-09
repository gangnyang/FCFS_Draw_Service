package com.fcfsdraw.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "WALLET_NOT_FOUND", "지갑을 찾을 수 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "INSUFFICIENT_BALANCE", "잔액이 부족합니다."),
    INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "금액은 0보다 커야 합니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청 값이 올바르지 않습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }
}
