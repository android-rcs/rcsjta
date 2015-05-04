/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.sonymobile.rcs.imap;

import static com.sonymobile.rcs.imap.ImapUtil.CRLF;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Key, value pair holder with parsing utility methods.
 */
public class Header {

    private String mKey;

    private String mValue;

    private Header(String key) {
        mKey = key.trim();
    }

    protected Header(String key, String rawValue) {
        mKey = key.trim();
        mValue = rawValue.trim();
    }

    public Date getValueAsDate() throws ParseException {
        // Thu, 13 Feb 1989 23:32 -0330
        // EEE, dd MMM yyyy HH:mm:ss Z
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
        String normalized = normalizeSpace(mValue);
        return sdf.parse(normalized);
    }

    public void setValueAsDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z");
        mValue = sdf.format(date);
    }

    public String getKey() {
        return mKey;
    }

    public String getValue() {
        return mValue;
    }

    public static final String normalizeSpace(String string) {
        return string.replaceAll("\\s+", " ").trim();
    }

    public static String cleanComments(String string) {
        if (string.indexOf('(') == -1)
            return string;
        boolean ignore = false;
        boolean escape = false;
        char[] arr = string.toCharArray();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == '\\') {
                escape = true;
                continue;
            } else if (arr[i] == '(' && !escape) {
                ignore = true;
            } else if (arr[i] == ')' && !escape) {
                ignore = false;
            } else if (!ignore) {
                sb.append(arr[i]);
            }
            escape = false;
        }
        return sb.toString();
    }

    public static Header createHeader(String headerSpec) {
        headerSpec = cleanComments(headerSpec);

        int i = headerSpec.indexOf(':');
        if (i != -1) {
            String key = headerSpec.substring(0, i).trim();
            String value = headerSpec.substring(i + 1).trim();
            return new Header(key, value);
        } else {
            return new Header(headerSpec);
        }
    }

    public static Map<String, Header> parseHeaders(String headerString) {
        Map<String, Header> headers = new HashMap<String, Header>();
        // unfold
        headerString = headerString.replace(CRLF + " ", "");

        String[] headersArray = headerString.split(CRLF);
        for (String spec : headersArray) {
            Header header = createHeader(spec);
            headers.put(header.getKey(), header);
        }
        return headers;
    }

    public String getValueAttribute(String attr) {
        String a = attr + "=";
        String[] split = mValue.split(";");
        for (String s : split) {
            s = s.trim();
            if (s.startsWith(a)) {
                char[] arr = s.substring(a.length()).toCharArray();
                if (arr[0] == '"')
                    arr[0] = ' ';
                if (arr[arr.length - 1] == '"')
                    arr[arr.length - 1] = ' ';
                return new String(arr).trim();
            }
        }
        return null;
    }

    public String getMimeType() {
        String[] split = mValue.split(";");
        return split[0];
    }

}
