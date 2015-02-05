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

package com.orangelabs.rcs.core.ims.service.im.chat.cpim;

/**
 * CPIM header
 * 
 * @author jexa7410
 */
public class CpimHeader {
    /***
     * Header name
     */
    private String name;

    /**
     * Header value
     */
    private String value;

    /**
     * Constructor
     * 
     * @param name Header name
     * @param value Header value
     */
    public CpimHeader(String name, String value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Returns header name
     * 
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * Returns header value
     * 
     * @return String
     */
    public String getValue() {
        return value;
    }

    /**
     * Parse CPIM header
     * 
     * @param data Input data
     * @return Header
     * @throws Exception
     */
    public static CpimHeader parseHeader(String data) throws Exception {
        int index = data.indexOf(":");
        String key = data.substring(0, index);
        String value = data.substring(index + 1);
        return new CpimHeader(key.trim(), value.trim());
    }
}
