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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import android.text.format.Time;

/**
 * Date utility functions
 * 
 * @author jexa7410
 * @author Deutsche Telekom
 */
public class DateUtils {
    /**
     * UTC time zone
     */
    private static TimeZone UTC = TimeZone.getTimeZone("UTC");

    /**
     * ISO 8601 date formats
     */
    private static SimpleDateFormat ISO8601DATEFORMAT[] = {
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ")
    };

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
     * Decode a string date to long value (see ISO8601 and RFC 3339)
     * 
     * @param date Date as string
     * @return Milliseconds
     */
    public static long decodeDate(String date) {
        long millis = -1;

        // Try to use ISO8601
        String normalizedDate = date.replaceAll("Z$", "+0000");
        for (int i = 0; millis == -1 && i < ISO8601DATEFORMAT.length; i++) {
            try {
                Date iso8601 = ISO8601DATEFORMAT[i].parse(normalizedDate);
                millis = iso8601.getTime();
            } catch (ParseException ex) {
                // Try next format
            }
        }

        // If still not valid format is found let's try RFC3339
        if (millis == -1) {
            Time t = new Time(UTC.getID());
            t.parse3339(date);
            millis = t.toMillis(false);
        }

        return millis;
    }
}
