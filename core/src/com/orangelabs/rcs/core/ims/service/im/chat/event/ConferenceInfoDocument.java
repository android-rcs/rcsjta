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
package com.orangelabs.rcs.core.ims.service.im.chat.event;

import java.util.Vector;

/**
 * Conference-Info document
 *
 * @author jexa7410
 */
public class ConferenceInfoDocument {
    /**
     * State values
     */ 
    public final static String STATE_PARTIAL = "partial";
    public final static String STATE_FULL = "full";
    public final static String STATE_DELETED = "deleted";

    /**
     * Conference URI
     */
    private String entity;

    /**
     * State attribute
     */
    private String state;

    /**
     * List of users
     */
    private Vector<User> users = new Vector<User>();

    /**
     * Maximum number of participants for the chat session
     */
    private int maxUserCount = 0;

    /**
     * Current number of participants in the chat session
     */
    private int userCount = 0;

    /**
     * Constructor
     * 
     * @param entity Conference URI
     * @param state State attribute
     */
    public ConferenceInfoDocument(String entity, String state) {
        this.entity = entity;
        this.state = state;
    }

    /**
     * Return the conference URI
     *
     * @return Conference URI
     */
    public String getEntity() {
        return entity;
    }

    /**
     * Reeturn the state
     *
     * @return State
     */
    public String getState() {
        return state;
    }

    /**
     * Add a user
     *
     * @param user User
     */
    public void addUser(User user) {
        users.addElement(user);
    }

    /**
     * Get the list of users
     *
     * @return List of users
     */
    public Vector<User> getUsers() {
        return users;
    }

    /**
     * Get the maximum user count
     *
     * @return Max user count or 0 if not contained in the conference info
     */
    public int getMaxUserCount() {
        return maxUserCount;
    }

    /**
     * Set the maximum user count
     *
     * @param maxUserCount Max user to set
     */
    public void setMaxUserCount(int maxUserCount) {
        this.maxUserCount = maxUserCount;
    }

    /**
     * Get the user count
     *
     * @return User count or 0 if not contained in the conference info
     */
    public int getUserCount() {
        return userCount;
    }

    /**
     * Set the user count
     *
     * @param userCount User count
     */
    public void setUserCount(int userCount) {
        this.userCount = userCount;
    }
}
