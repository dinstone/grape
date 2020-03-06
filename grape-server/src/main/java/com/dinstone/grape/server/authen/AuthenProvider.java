package com.dinstone.grape.server.authen;

public interface AuthenProvider {

    AuthenUser authenticate(String un, String pw);

}
