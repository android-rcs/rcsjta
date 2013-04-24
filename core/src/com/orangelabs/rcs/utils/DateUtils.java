/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.orangelabs.rcs.utils;

import java.util.TimeZone;

import android.text.format.Time;

/**
 * Date utility functions
 *  
 * @author jexa7410
 */
public class DateUtils {
	/**
	 * UTC time zone
	 */
	private static TimeZone UTC = TimeZone.getTimeZone("UTC");
	
	/**
	 * Encode a long date to string value in Z format (see RFC 3339)
	 * 
	 * @param date Date in milliseconds
	 * @return String
	 */
	public static String encodeDate(long date) {
		Time t = new Time(UTC.getID());
		t.set(date);
		return t.format3339(false);
	}
	
	/**
	 * Decode a string date to long value (see RFC 3339)
	 * 
	 * @param date Date as string
	 * @return Milliseconds
	 */
	public static long decodeDate(String date) {
		Time t = new Time(UTC.getID());
		t.parse3339(date);
		return t.toMillis(false);
	}
}
