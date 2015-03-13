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

package com.sonymobile.rcs.cpm.ms;

import java.util.Set;

/**
 * The Group State object is an XML object with XML Content Type
 * "application/group-state-object+xml" containing the last valid CPM Group Session Identity for the
 * Group Chat, as well as the last set of active participants at the time the Group Chat was torn
 * down because of inactivity.
 */
public interface CpmGroupState extends CpmObject {

    public static final String CONTENT_TYPE = "application/group-state-object+xml";

    /**
     * Returns the last focus session id
     * 
     * @return the last focus session id
     */
    public String getLastFocusSessionId();

    /**
     * The time of the state
     * 
     * @return the timestamp
     */
    public long getTimeStamp();

    /**
     * Returns the participants
     * 
     * @return the participants as a list
     */
    public Set<Participant> getParticipants();

    /**
     * @return
     */
    public Set<String> getParticipantAddresses();

    /**
     * Return the enclosing session
     * 
     * @return the session
     */
    public SessionHistoryFolder getSession();

    /**
     * "open", "closed" ...
     * 
     * @return
     */
    public String getType();

}
