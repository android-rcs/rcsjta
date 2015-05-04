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

public class ImapNamespace {

    private final String mPreffix;

    private final String mDelimiter;

    public ImapNamespace(String preffix, String delimiter) {
        super();
        mPreffix = preffix;
        mDelimiter = delimiter;
    }

    protected ImapNamespace(String spec) {
        // EXAMPLE NAMESPACE TO PARSE: ("" ".")
        String s = spec.substring(2, spec.length() - 2);
        String[] a = s.split("\" \"");
        mPreffix = a[0];
        mDelimiter = a[1];
    }

    public boolean hasPreffix() {
        return mPreffix != null && mPreffix.length() > 0;
    }

    public String getDelimiter() {
        return mDelimiter;
    }

    public String getPreffix() {
        return mPreffix;
    }

    @Override
    public String toString() {
        return "IMAPNAMESPACE[pref=" + mPreffix + ",delim=" + mDelimiter + "]";
    }

}
