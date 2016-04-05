/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.ri.messaging.adapter;

import com.gsma.rcs.ri.R;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.contact.ContactId;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by yplo6403 on 12/01/2016.
 */
public class TalkListArrayItem implements Comparable<TalkListArrayItem> {

    private final long mTimestamp;
    private final RcsService.Direction mDirection;
    private String mSubject;
    private final String mChatId;
    private final ContactId mContact;
    private String mContent;
    private final String mMimeType;
    private int mUnreadCount;

    /**
     * Constructor for XMS and RCS chat information
     *
     * @param chatId the chat ID
     * @param contact the contact ID
     * @param timestamp the timestamp
     * @param direction the direction
     * @param content the content
     * @param mimeType the mime type
     * @param unreadCount the read status
     */
    public TalkListArrayItem(String chatId, ContactId contact, long timestamp,
            RcsService.Direction direction, String content, String mimeType, int unreadCount) {
        mChatId = chatId;
        mContact = contact;
        mTimestamp = timestamp;
        mDirection = direction;
        mContent = content;
        mMimeType = mimeType;
        mUnreadCount = unreadCount;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public RcsService.Direction getDirection() {
        return mDirection;
    }

    public ContactId getContact() {
        return mContact;
    }

    public void setContent(String content) {
        mContent = content;
    }

    public String getContent() {
        return mContent;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public String getChatId() {
        return mChatId;
    }

    public String getSubject() {
        return mSubject;
    }

    public void setSubject(String subject) {
        mSubject = subject;
    }

    @Override
    public int compareTo(TalkListArrayItem another) {
        if (another == null) {
            throw new NullPointerException("Cannot compare to null");
        }
        return Long.valueOf(another.getTimestamp()).compareTo(mTimestamp);
    }

    public int getUnreadCount() {
        return mUnreadCount;
    }

    public void incrementUnreadCount() {
        mUnreadCount++;
    }

    public boolean isGroupChat() {
        return mContact == null || !mChatId.equals(mContact.toString());
    }

    static public class ViewHolder {

        private final TextView mStatusText;
        private final TextView mTimestampText;
        private final TextView mContentText;
        private final ImageView mAvatarImage;

        ViewHolder(View view) {
            mAvatarImage = (ImageView) view.findViewById(R.id.avatar);
            mStatusText = (TextView) view.findViewById(R.id.status_text);
            mTimestampText = (TextView) view.findViewById(R.id.timestamp_text);
            mContentText = (TextView) view.findViewById(R.id.content_text);
        }

        public TextView getStatusText() {
            return mStatusText;
        }

        public TextView getTimestampText() {
            return mTimestampText;
        }

        public TextView getContentText() {
            return mContentText;
        }

        public ImageView getAvatarImage() {
            return mAvatarImage;
        }

    }

    static public class ViewHolderOneToOne extends ViewHolder {

        private final TextView mContactText;

        public ViewHolderOneToOne(View view) {
            super(view);
            mContactText = (TextView) view.findViewById(R.id.contact_text);
        }

        public TextView getContactText() {
            return mContactText;
        }
    }

    static public class ViewHolderGroup extends ViewHolder {

        private final TextView mSubjectText;

        public ViewHolderGroup(View view) {
            super(view);
            mSubjectText = (TextView) view.findViewById(R.id.subject_text);
        }

        public TextView getSubjectText() {
            return mSubjectText;
        }
    }

}
