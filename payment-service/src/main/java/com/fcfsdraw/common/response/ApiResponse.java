package com.fcfsdraw.common.response;

public record ApiResponse<T>(
        boolean success,
        String code,
        T data
) {

    private static final String SUCCESS_CODE = "SUCCESS";

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, SUCCESS_CODE, data);
    }

    public static <T> ApiResponse<T> failure(String code, T data) {
        return new ApiResponse<>(false, code, data);
    }
}
