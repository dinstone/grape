/*
 * Copyright (C) 2016~2024 dinstone<dinstone@163.com>
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
package com.dinstone.grape.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BaseTest {

	public static void main(String[] args) {
		String f = "^([a-z]|[A-Z])(\\w|-)*";
		Pattern p = Pattern.compile(f);
		String s = "tust_name";
		Matcher m = p.matcher(s);
		if (m.matches()) {
			System.out.println("match");
		} else {
			System.out.println("no match");
		}

		testExecuteTime();
	}

	private static void testExecuteTime() {
		int loopTime = 100000;
		// Integer i = 0;
		long i = 154768L;
		long startTime;

		startTime = System.currentTimeMillis();
		for (int j = 0; j < loopTime; j++) {
			String str = String.valueOf(i);
		}
		System.out.println("String.valueOf()：" + (System.currentTimeMillis() - startTime) + "ms");

		startTime = System.currentTimeMillis();
		for (int j = 0; j < loopTime; j++) {
			String str = Long.toString(i);
		}
		System.out.println("Long.toString()：" + (System.currentTimeMillis() - startTime) + "ms");

		startTime = System.currentTimeMillis();
		for (int j = 0; j < loopTime; j++) {
			String str = "" + i;
		}
		System.out.println("i + \"\"：" + (System.currentTimeMillis() - startTime) + "ms");
	}

}
