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

package com.gsma.rcs.provisioning;

/**
 * Provisioning parameter
 * 
 * @author jexa7410
 */
public class Parameter {
    /**
     * Parameter name
     */
    private String name;

    /**
     * Parameter value
     */
    private String value;

    /**
     * Parameter type
     */
    private String type;

    /**
     * Parameter path
     */
    private String path;

    /**
     * Constructor
     * 
     * @param name Name
     * @param value Value
     * @param type Type
     * @param path Path
     */
    public Parameter(String name, String value, String type, String path) {
        this.name = name;
        this.value = value;
        this.type = type;
        this.path = path;
    }

    /**
     * Returns parameter name
     * 
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * Returns parameter value
     * 
     * @return String
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns parameter type
     * 
     * @return String
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the path associated to the parameter
     * 
     * @return
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns the string representtaion of the parameter
     * 
     * @return String
     */
    public String toString() {
        return "Name: " + name + ", value=" + value + ", type=" + type + ", path=" + path;
    }
}
