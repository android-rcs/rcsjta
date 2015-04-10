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

package com.gsma.rcs.core.ims.service;

import android.util.SparseArray;

import com.gsma.rcs.core.ims.service.capability.Capabilities;
import com.gsma.rcs.core.ims.service.presence.PresenceInfo;
import com.gsma.services.rcs.contact.ContactId;

/**
 * Contact info
 */
public class ContactInfo {

    /**
     * IMS registration state
     */
    public enum RegistrationState {
        /**
         * Unknown registration state
         */
        UNKNOWN(0),
        /**
         * Registered
         */
        ONLINE(1),
        /**
         * Not registered
         */
        OFFLINE(2);

        private int mValue;

        /**
         * A data array to keep mapping between value and RegistrationState
         */
        private static SparseArray<RegistrationState> mValueToEnum = new SparseArray<RegistrationState>();
        static {
            for (RegistrationState entry : RegistrationState.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        private RegistrationState(int value) {
            mValue = value;
        }

        /**
         * Gets integer value associated to RegistrationState instance
         * 
         * @return value
         */
        public final int toInt() {
            return mValue;
        }

        /**
         * Returns a RegistrationState instance for the specified integer value.
         * 
         * @param value
         * @return instance of RegistrationState
         */
        public static RegistrationState valueOf(int value) {
            RegistrationState state = mValueToEnum.get(value);
            if (state != null) {
                return state;
            }
            return UNKNOWN;
        }
    };

    /**
     * User blocking state
     */
    public enum BlockingState {
        /**
         * User is not blocked
         */
        NOT_BLOCKED(0),
        /**
         * user is blocked
         */
        BLOCKED(1);

        private int mValue;

        /**
         * A data array to keep mapping between value and BlockingState
         */
        private static SparseArray<BlockingState> mValueToEnum = new SparseArray<BlockingState>();
        static {
            for (BlockingState entry : BlockingState.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        private BlockingState(int value) {
            mValue = value;
        }

        /**
         * Gets integer value associated to BlockingState instance
         * 
         * @return value
         */
        public final int toInt() {
            return mValue;
        }

        /**
         * Returns a BlockingState instance for the specified integer value.
         * 
         * @param value
         * @return instance of BlockingState
         */
        public static BlockingState valueOf(int value) {
            BlockingState state = mValueToEnum.get(value);
            if (state != null) {
                return state;
            }
            return NOT_BLOCKED;
        }
    };

    /**
     * RCS status
     */
    public enum RcsStatus {
        /**
         * The contact is RCS capable but there is no special presence relationship with the user
         */
        RCS_CAPABLE(0),
        /**
         * The contact is not RCS
         */
        NOT_RCS(1),
        /**
         * Presence relationship: contact 'rcs granted' with the user
         */
        ACTIVE(2),
        /**
         * Presence relationship: the user has revoked the contact
         */
        REVOKED(3),
        /**
         * Presence relationship: the user has blocked the contact
         */
        BLOCKED(4),
        /**
         * Presence relationship: the user has sent an invitation to the contact without response
         * for now
         */
        PENDING_OUT(5),
        /**
         * Presence relationship: the contact has sent an invitation to the user without response
         * for now
         */
        PENDING(6),
        /**
         * Presence relationship: the contact has sent an invitation to the user and cancel it
         */
        CANCELLED(7),
        /**
         * We have never queried the contact capabilities for now
         */
        NO_INFO(8);

        private int mValue;

        /**
         * A data array to keep mapping between value and PresenceSharingStatus
         */
        private static SparseArray<RcsStatus> mValueToEnum = new SparseArray<RcsStatus>();
        static {
            for (RcsStatus entry : RcsStatus.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        private RcsStatus(int value) {
            mValue = value;
        }

        /**
         * Gets integer value associated to RcsStatus instance
         * 
         * @return value
         */
        public final int toInt() {
            return mValue;
        }

        /**
         * Returns a RcsStatus instance for the specified integer value.
         * 
         * @param value
         * @return instance of RcsStatus
         */
        public static RcsStatus valueOf(int value) {
            RcsStatus status = mValueToEnum.get(value);
            if (status != null) {
                return status;
            }
            return NO_INFO;
        }
    };

    /**
     * Capabilities
     */
    private Capabilities mCapabilities;

    /**
     * Presence info, relevant only if social info is activated for this contact
     */
    private PresenceInfo mPresenceInfo;

    /**
     * Contact identifier
     */
    private ContactId mContact;

    /**
     * Display name of RCS contact
     */
    private String mDisplayName;

    /**
     * Registration state
     */
    private RegistrationState mRegistrationState = RegistrationState.UNKNOWN;

    /**
     * RCS status
     */
    private RcsStatus mRcsStatus = RcsStatus.NOT_RCS;

    /**
     * RCS status timestamp
     */
    private long mRcsStatusTimestamp = 0L;

    /**
     * Blocking state
     */
    private BlockingState mBlockingState = BlockingState.NOT_BLOCKED;

    /**
     * Blocking timestamp
     */
    private long mBlockingTs;

    /**
     * Constructor
     */
    public ContactInfo() {
    }

    /**
     * Constructor
     * 
     * @param info
     */
    public ContactInfo(ContactInfo info) {
        mContact = info.getContact();
        mRegistrationState = info.getRegistrationState();
        mRcsStatus = info.getRcsStatus();
        mRcsStatusTimestamp = info.getRcsStatusTimestamp();
        mCapabilities = info.mCapabilities;
        mPresenceInfo = info.getPresenceInfo();
        mDisplayName = info.getDisplayName();
        mBlockingState = info.getBlockingState();
        mBlockingTs = info.getBlockingTimestamp();
    }

    /**
     * Set the capabilities
     * 
     * @param capabilities Capabilities
     */
    public void setCapabilities(Capabilities capabilities) {
        mCapabilities = capabilities;
    }

    /**
     * Returns the capabilities
     * 
     * @return Capabilities
     */
    public Capabilities getCapabilities() {
        return mCapabilities;
    }

    /**
     * Set the presence info
     * 
     * @param info Presence info
     */
    public void setPresenceInfo(PresenceInfo info) {
        mPresenceInfo = info;
    }

    /**
     * Returns the presence info
     * 
     * @return PresenceInfo
     */
    public PresenceInfo getPresenceInfo() {
        return mPresenceInfo;
    }

    /**
     * Set the contact identifier
     * 
     * @param contact Contact identifier
     */
    public void setContact(ContactId contact) {
        mContact = contact;
    }

    /**
     * Returns the contact identifier
     * 
     * @return contactId
     */
    public ContactId getContact() {
        return mContact;
    }

    /**
     * Set the RCS status
     * 
     * @param rcsStatus RCS status
     */
    public void setRcsStatus(RcsStatus rcsStatus) {
        mRcsStatus = rcsStatus;
    }

    /**
     * Returns the RCS status
     * 
     * @return rcsStatus
     */
    public RcsStatus getRcsStatus() {
        return mRcsStatus;
    }

    /**
     * Set the registration state
     * 
     * @param state the registration state
     */
    public void setRegistrationState(RegistrationState state) {
        mRegistrationState = state;
    }

    /**
     * Returns the registration state
     * 
     * @return registrationState
     */
    public RegistrationState getRegistrationState() {
        return mRegistrationState;
    }

    /**
     * Set the RCS status timestamp
     * 
     * @param timestamp Last RCS status date of change
     */
    public void setRcsStatusTimestamp(long timestamp) {
        mRcsStatusTimestamp = timestamp;
    }

    /**
     * Returns the RCS status timestamp
     * 
     * @return timestamp
     */
    public long getRcsStatusTimestamp() {
        return mRcsStatusTimestamp;
    }

    /**
     * Is a RCS contact
     * 
     * @return true if the contact is RCS
     */
    public boolean isRcsContact() {
        return (!RcsStatus.NO_INFO.equals(mRcsStatus) && !RcsStatus.NOT_RCS.equals(mRcsStatus));

    }

    /**
     * Returns the RCS display name
     * 
     * @return the RCS display name
     */
    public String getDisplayName() {
        return mDisplayName;
    }

    /**
     * Sets the RCS display name
     * 
     * @param displayName
     */
    public void setDisplayName(String displayName) {
        mDisplayName = displayName;
    }

    /**
     * Set the blocking state
     * 
     * @param state State
     */
    public void setBlockingState(BlockingState state) {
        mBlockingState = state;
    }

    /**
     * Returns the blocking state
     * 
     * @return State
     */
    public BlockingState getBlockingState() {
        return mBlockingState;
    }

    /**
     * Returns the blocking timestamp
     * 
     * @return Timestamp
     */
    public long getBlockingTimestamp() {
        return mBlockingTs;
    }

    /**
     * Set the blocking timestamp
     * 
     * @param ts Timestamp
     */
    public void setBlockingTimestamp(long ts) {
        mBlockingTs = ts;
    }

    /**
     * Returns a string representation of the object
     * 
     * @return String
     */
    public String toString() {
        String result = "Contact=" + mContact + ", Status=" + mRcsStatus + ", State="
                + mRegistrationState + ", Timestamp=" + mRcsStatusTimestamp + ", Blocked="
                + mBlockingState + ", Blocked at=" + mBlockingTs;
        if (mCapabilities != null) {
            result += ", Capabilities=" + mCapabilities.toString();
        }
        if (mPresenceInfo != null) {
            result += ", Presence=" + mPresenceInfo.toString();
        }
        return result;
    }
}
