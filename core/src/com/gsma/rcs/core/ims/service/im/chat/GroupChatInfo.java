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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.core.ims.service.im.chat;

import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;

import java.util.Map;

/**
 * Group chat info
 * 
 * @author Jean-Marc AUFFRET
 */
public class GroupChatInfo {

    /**
     * Rejoin ID
     */
    private Uri mRejoinId;

    /**
     * Contribution Id
     */
    private String mContributionId;

    /**
     * Set of initial participants
     */
    private Map<ContactId, ParticipantStatus> mParticipants;

    /**
     * Subject
     */
    private String mSubject;

    /**
     * Local timestamp
     */
    private long mTimestamp;

    /**
     * Constructor
     * 
     * @param rejoindId Rejoin ID
     * @param contributionId Contribution ID
     * @param particpants Participants
     * @param subject Subject
     * @param timestamp Local timestamp
     */
    public GroupChatInfo(Uri rejoinId, String contributionId,
            Map<ContactId, ParticipantStatus> participants, String subject, long timestamp) {
        mRejoinId = rejoinId;
        mContributionId = contributionId;
        mParticipants = participants;
        mSubject = subject;
        mTimestamp = timestamp;
    }

    /**
     * Returns the rejoin ID
     * 
     * @return ID
     */
    public Uri getRejoinId() {
        return mRejoinId;
    }

    /**
     * Returns the contribution ID
     * 
     * @return ID
     */
    public String getContributionId() {
        return mContributionId;
    }

    /**
     * Returns set of participants
     * 
     * @return Participants
     */
    public Map<ContactId, ParticipantStatus> getParticipants() {
        return mParticipants;
    }

    /**
     * Returns the subject
     * 
     * @return Subject
     */
    public String getSubject() {
        return mSubject;
    }

    /**
     * Returns the timestamp
     * 
     * @return Timestamp
     */
    public long getTimestamp() {
        return mTimestamp;
    }

    /**
     * Returns a string representation of the object
     * 
     * @return String
     */
    public String toString() {
        return "Contribution ID=" + mContributionId + ", Rejoin ID=" + mRejoinId + ", Subject="
                + mSubject + ", Participants=" + mParticipants.size() + ", Timestamp=" + mTimestamp;
    }
}
