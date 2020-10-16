package com.dinstone.grape.exception;

public class BusinessException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

}
