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

package com.gsma.rcs.core.ims.service.presence;

/**
 * Favorite link
 * 
 * @author Jean-Marc AUFFRET
 */
public class FavoriteLink {
    /**
     * Link
     */
    private String link = null;

    /**
     * Name
     */
    private String name = null;

    /**
     * Constructor
     * 
     * @param link Web link
     */
    public FavoriteLink(String link) {
        this.link = link;
    }

    /**
     * Constructor
     * 
     * @param name Name associated to the link
     * @param link Web link
     */
    public FavoriteLink(String name, String link) {
        this.name = name;
        this.link = link;
    }

    /**
     * Returns the name associated to the link
     * 
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name associated to the link
     * 
     * @param name Name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the web link
     * 
     * @return
     */
    public String getLink() {
        return link;
    }

    /**
     * Set the web link
     * 
     * @param link Web link
     */
    public void setLink(String link) {
        this.link = link;
    }

    /**
     * Returns a string representation of the object
     * 
     * @return String
     */
    public String toString() {
        return "link=" + link + ", name=" + name;
    }
}
