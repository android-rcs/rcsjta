/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p/>
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.core.ims.service.im.chat.iscomposing;

import static com.gsma.rcs.utils.StringUtils.UTF8_STR;

import com.gsma.rcs.utils.DateUtils;
import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;

/**
 * Is composing info document (see RFC3994)
 */
public class IsComposingInfo {

    public static String MIME_TYPE = "application/im-iscomposing+xml";

    private static final String CRLF = "\r\n";

    private boolean mActive;

    private long mLastActiveDate;

    private long mRefreshTime;

    private String mContentType;

    /**
     * Constructor
     */
    public IsComposingInfo() {
        mContentType = "";
    }

    public void setState(String state) {
        mActive = state.equalsIgnoreCase("active");
    }

    /**
     * Sets the last active timestamp
     * 
     * @param lastActiveTimestamp the last active timestamp
     */
    public void setLastActiveDate(String lastActiveTimestamp) {
        mLastActiveDate = DateUtils.decodeDate(lastActiveTimestamp);
    }

    /**
     * Sets the refresh time
     *
     * @param refreshTime in milliseconds
     */
    public void setRefreshTime(long refreshTime) {
        mRefreshTime = refreshTime;
    }

    /**
     * Sets the content type
     * 
     * @param contentType the content type
     */
    public void setContentType(String contentType) {
        mContentType = contentType;
    }

    public boolean isStateActive() {
        return mActive;
    }

    public long getLastActiveDate() {
        return mLastActiveDate;
    }

    /**
     * Gets the refresh time
     *
     * @return refresh time in milliseconds
     */
    public long getRefreshTime() {
        return mRefreshTime;
    }

    /**
     * Gets the content type
     * 
     * @return the content type
     */
    public String getContentType() {
        return mContentType;
    }

    /**
     * Build is composing document
     *
     * @param status Status
     * @return XML document
     */
    public static String buildIsComposingInfo(boolean status) {
        return new StringBuilder("<?xml version=\"1.0\" encoding=\"")
                .append(UTF8_STR)
                .append("\"?>")
                .append(CRLF)
                .append("<isComposing xmlns=\"urn:ietf:params:xml:ns:im-iscomposing\"")
                .append(CRLF)
                .append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")
                .append(CRLF)
                .append("xsi:schemaLocation=\"urn:ietf:params:xml:ns:im-composing iscomposing.xsd\">")
                .append(CRLF).append("<state>").append(status ? "active" : "idle")
                .append("</state>").append(CRLF).append("<contenttype>")
                .append(MimeType.TEXT_MESSAGE).append("</contenttype>").append(CRLF)
                .append("<lastactive>").append(DateUtils.encodeDate(System.currentTimeMillis()))
                .append("</lastactive>").append(CRLF).append("<refresh>60</refresh>").append(CRLF)
                .append("</isComposing>").toString();
    }
}
