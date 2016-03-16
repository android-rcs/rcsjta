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

package com.gsma.rcs.core.ims.protocol.http;

/**
 * HTTP request
 * 
 * @author jexa7410
 */
public abstract class HttpRequest {
    /**
     * URL
     */
    private String mUrl;

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
     * HTTP authentication agent
     */
    private HttpAuthenticationAgent mAuthenticationAgent;

    /**
     * Constructor
     * 
     * @param url URL
     * @param content Content
     * @param contentType Content type
     */
    public HttpRequest(String url, String content, String contentType) {
        mUrl = url;
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
     * Set the authentication agent
     * 
     * @param agent Authentication agent
     */
    public void setAuthenticationAgent(HttpAuthenticationAgent agent) {
        mAuthenticationAgent = agent;
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
        return mUrl;
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
        return mUrl.split("/")[1];
    }
}
