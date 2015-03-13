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

public class ImapFolder {

    private final String mName;
    private final String mPath;
    private final String mSeparator;

    public ImapFolder(String path, String name, String sepa) {
        super();
        mPath = path.toString();
        mName = name.toString();
        mSeparator = sepa.toString();
    }

    public String getPathSeparator() {
        return mSeparator;
    }

    public String getName() {
        return mName;
    }

    public String getFullPath() {
        return mPath;
    }

    @Override
    public String toString() {
        return "ImapFolder[path=" + mPath + ", name=" + mName + ", sepa=" + mSeparator + "]";
    }

    public ImapFolder getSubFolder(String folderName) {
        StringBuilder sb = new StringBuilder();
        sb.append(mPath);
        sb.append(mSeparator);
        sb.append(folderName);

        ImapFolder child = new ImapFolder(sb.toString(), folderName, mSeparator);
        return child;
    }

}
