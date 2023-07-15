package com.dinstone.grape.exception;

public enum JobErrorCode implements ErrorCode {

	ID_EMPTY(2001), DTR_INVALID(2002), TTR_INVALID(2003);

	private int value;

	private JobErrorCode(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}
}
