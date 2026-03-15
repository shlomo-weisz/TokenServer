package com.tokenlearn.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard API response envelope returned by every controller method.
 *
 * @param <T> response payload type when the request succeeds
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private ErrorData error;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorData(code, message));
    }

    /**
     * Structured error payload included when a request fails.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorData {
        private String code;
        private String message;
    }
}
