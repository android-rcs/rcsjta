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

public class ImapMessageStatus {

    private final boolean mSeen, mDraft, mRecent, mDeleted, mAnswered, mFlagged, mReadReportSent;

    private final long mTimestamp = System.currentTimeMillis();

    public static final ImapMessageStatus EMPTY = new ImapMessageStatus();

    private ImapMessageStatus() {
        mSeen = mDraft = mRecent = mDeleted = mAnswered = mFlagged = mReadReportSent = false;
    }

    public ImapMessageStatus(boolean seen, boolean draft, boolean recent, boolean deleted,
            boolean answered, boolean flagged, boolean readReportSent) {
        super();
        this.mSeen = seen;
        this.mDraft = draft;
        this.mRecent = recent;
        this.mDeleted = deleted;
        this.mAnswered = answered;
        this.mFlagged = flagged;
        this.mReadReportSent = readReportSent;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public boolean isSeen() {
        return mSeen;
    }

    public boolean isDraft() {
        return mDraft;
    }

    public boolean isRecent() {
        return mRecent;
    }

    public boolean isDeleted() {
        return mDeleted;
    }

    public boolean isAnswered() {
        return mAnswered;
    }

    public boolean isFlagged() {
        return mFlagged;
    }

    public boolean isReadReportSent() {
        return mReadReportSent;
    }

}
