package com.dinstone.grape.server.handler;

import com.dinstone.grape.exception.ErrorCode;

public enum ValidErrorCode implements ErrorCode {

    TUBE_NAME_EMPTY(2000), JOB_ID_EMPTY(2001), JOB_DTR_INVALID(2002), JOB_TTR_INVALID(2003), // api
    USERNAME_EMPTY(3000), PASSWORD_EMPTY(3001), AUTHEN_INVALID(3002);

    private int value;

    private ValidErrorCode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

}
