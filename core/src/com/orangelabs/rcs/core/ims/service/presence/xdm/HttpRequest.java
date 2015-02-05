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

package com.orangelabs.rcs.core.ims.service.presence.xdm;

/**
 * HTTP request
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class HttpRequest {
    /**
     * URL
     */
    private String url;

    /**
     * Content
     */
    private String content;

    /**
     * Content type
     */
    private String contentType;

    /**
     * Cookie
     */
    private String cookie = null;

    /**
     * HTTP authentication agent MD5
     */
    private HttpAuthenticationAgent authenticationAgent = new HttpAuthenticationAgent();

    /**
     * Constructor
     * 
     * @param url URL
     * @param content Content
     * @param contentType Content type
     */
    public HttpRequest(String url, String content, String contentType) {
        this.url = url;
        this.content = content;
        this.contentType = contentType;
    }

    /**
     * Returns the authentication agent
     * 
     * @return Authentication agent
     */
    public HttpAuthenticationAgent getAuthenticationAgent() {
        return authenticationAgent;
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
        return url;
    }

    /**
     * Returns the HTTP content
     * 
     * @return Conetnt
     */
    public String getContent() {
        return content;
    }

    /**
     * Returns the HTTP content
     * 
     * @return Conetnt
     */
    public int getContentLength() {
        int length = 0;
        if (content != null) {
            length = content.length();
        }
        return length;
    }

    /**
     * Returns the content type
     * 
     * @return Mime content type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Returns the cookie
     * 
     * @return Cookie
     */
    public String getCookie() {
        return cookie;
    }

    /**
     * Set the cookie
     * 
     * @param cookie Cookie
     */
    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    /**
     * Returns the AUID of the request
     * 
     * @return AUID
     */
    public String getAUID() {
        try {
            String[] parts = url.split("/");
            return parts[1];
        } catch (Exception e) {
            return null;
        }
    }
}
