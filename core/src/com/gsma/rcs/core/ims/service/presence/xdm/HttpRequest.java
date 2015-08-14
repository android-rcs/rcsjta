/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.core.ims.service.presence.xdm;

/**
 * HTTP request
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class HttpRequest {
    /**
     * URL
     */
    private String mURl;

    /**
     * Content
     */
    private String mContent;

    /**
     * Content type
     */
    private String mContentType;

    /**
     * Cookie
     */
    private String mCookie;

    /**
     * HTTP authentication agent MD5
     */
    private HttpAuthenticationAgent mAuthenticationAgent = new HttpAuthenticationAgent();

    /**
     * Constructor
     * 
     * @param url URL
     * @param content Content
     * @param contentType Content type
     */
    public HttpRequest(String url, String content, String contentType) {
        mURl = url;
        mContent = content;
        mContentType = contentType;
    }

    /**
     * Returns the authentication agent
     * 
     * @return Authentication agent
     */
    public HttpAuthenticationAgent getAuthenticationAgent() {
        return mAuthenticationAgent;
    }

    /**
     * Returns the HTTP method
     * 
     * @return Method
     */
    public abstract String getMethod();

    /**
     * Returns the HTTP URL
     * 
     * @return URL
     */
    public String getUrl() {
        return mURl;
    }

    /**
     * Returns the HTTP content
     * 
     * @return Conetnt
     */
    public String getContent() {
        return mContent;
    }

    /**
     * Returns the HTTP content
     * 
     * @return Conetnt
     */
    public int getContentLength() {
        int length = 0;
        if (mContent != null) {
            length = mContent.length();
        }
        return length;
    }

    /**
     * Returns the content type
     * 
     * @return Mime content type
     */
    public String getContentType() {
        return mContentType;
    }

    /**
     * Returns the cookie
     * 
     * @return Cookie
     */
    public String getCookie() {
        return mCookie;
    }

    /**
     * Set the cookie
     * 
     * @param cookie Cookie
     */
    public void setCookie(String cookie) {
        mCookie = cookie;
    }

    /**
     * Returns the AUID of the request
     * 
     * @return AUID
     */
    public String getAUID() {
        return mURl.split("/")[1];
    }
}
