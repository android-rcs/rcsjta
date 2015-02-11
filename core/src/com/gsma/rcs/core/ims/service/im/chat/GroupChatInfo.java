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

package com.gsma.rcs.core.ims.service.im.chat;

import java.util.Set;

import com.gsma.services.rcs.chat.ParticipantInfo;

/**
 * Group chat info
 * 
 * @author Jean-Marc AUFFRET
 */
public class GroupChatInfo {
    /**
     * Session ID
     */
    private String sessionId;

    /**
     * Rejoin ID
     */
    private String rejoinId;

    /**
     * Contribution Id
     */
    private String contributionId;

    /**
     * Set of initial participants
     */
    private Set<ParticipantInfo> participants;

    /**
     * Subject
     */
    private String subject;

    /**
     * Constructor
     * 
     * @param sessionId Session ID
     * @param rejoindId Rejoin ID
     * @param contributionId Rejoin ID
     * @param particpants Participants
     * @param subject Subject
     */
    public GroupChatInfo(String sessionId, String rejoinId, String contributionId,
            Set<ParticipantInfo> participants, String subject) {
        this.sessionId = sessionId;
        this.rejoinId = rejoinId;
        this.contributionId = contributionId;
        this.participants = participants;
        this.subject = subject;
    }

    /**
     * Returns the session ID
     * 
     * @return ID
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Returns the rejoin ID
     * 
     * @return ID
     */
    public String getRejoinId() {
        return rejoinId;
    }

    /**
     * Returns the contribution ID
     * 
     * @return ID
     */
    public String getContributionId() {
        return contributionId;
    }

    /**
     * Returns set of participants
     * 
     * @return Participants
     */
    public Set<ParticipantInfo> getParticipants() {
        return participants;
    }

    /**
     * Returns the subject
     * 
     * @return Subject
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Returns a string representation of the object
     * 
     * @return String
     */
    public String toString() {
        return "Session ID=" + sessionId + ", Contribution ID=" + contributionId + ", Rejoin ID="
                + rejoinId + ", Subject=" + subject + ", Participants=" + participants.size();
    }
}
