/*
 * Copyright (C) 2016~2023 dinstone<dinstone@163.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dinstone.grape.server.handler;

import com.dinstone.grape.server.ApplicationContext;
import com.dinstone.grape.server.authen.AuthenProvider;
import com.dinstone.vertx.web.annotation.Context;
import com.dinstone.vertx.web.annotation.Get;
import com.dinstone.vertx.web.annotation.Post;
import com.dinstone.vertx.web.annotation.Produces;
import com.dinstone.vertx.web.annotation.WebHandler;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;

@WebHandler("/authen")
@Produces({ "application/json" })
public class AuthenApiHandler extends RestApiHandler {

    private AuthenProvider authenProvider;

    public AuthenApiHandler(ApplicationContext context) {
        super(context);
        authenProvider = context.getAuthenProvider();
    }

    @Post
    public void login(@Context RoutingContext ctx) {
        JsonObject params = ctx.getBodyAsJson();
        String un = params.getString("username");
        String pw = params.getString("password");
        if (un == null || un.isEmpty()) {
            failed(ctx, ValidErrorCode.USERNAME_EMPTY, "Username is empty");
            return;
        }
        if (pw == null || pw.isEmpty()) {
            failed(ctx, ValidErrorCode.PASSWORD_EMPTY, "Password is empty");
            return;
        }

        User user = authenProvider.authenticate(un, pw);
        if (user != null) {
            ctx.session().put("user", user);
            success(ctx, ctx.session().id());
        } else {
            failed(ctx, ValidErrorCode.AUTHEN_INVALID, "username or password is error");
        }

    }

    @Get
    public void logout(@Context RoutingContext ctx) {
        ctx.session().destroy();
        success(ctx);
    }

}