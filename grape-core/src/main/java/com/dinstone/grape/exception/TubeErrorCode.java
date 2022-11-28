package com.dinstone.grape.exception;

public enum TubeErrorCode implements ErrorCode {

    UNKOWN(1000), EMPTY(1001), INVALID(1002), GREATE(1003);

    private int value;

    private TubeErrorCode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

}
