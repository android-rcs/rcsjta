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

package com.orangelabs.rcs.core.ims.service;

import android.util.SparseArray;

import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.ims.service.capability.Capabilities;
import com.orangelabs.rcs.core.ims.service.presence.PresenceInfo;

/**
 * Contact info
 */
public class ContactInfo {
    
	public enum RegistrationState {
		UNKNOWN(0), ONLINE(1), OFFLINE(2);

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

		public final int toInt() {
			return mValue;
		}
		
		public static RegistrationState valueOf(int value) {
			RegistrationState state = mValueToEnum.get(value);
		    if (state != null) {
		        return state;
		    }
		    return UNKNOWN;
		}
	};
	
	public enum BlockingState {
		NONE(0), BLOCKED(1);

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

		public final int toInt() {
			return mValue;
		}
		
		public static BlockingState valueOf(int value) {
			BlockingState state = mValueToEnum.get(value);
		    if (state != null) {
		        return state;
		    }
		    return NONE;
		}
	};	
	
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
		 * Presence relationship: the user has sent an invitation to the contact without response for now
		 */
		PENDING_OUT(5),
		/**
		 * Presence relationship: the contact has sent an invitation to the user without response for now
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

		public final int toInt() {
			return mValue;
		}
		
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
	private Capabilities capabilities;
	
	/**
	 * Presence info, relevant only if social info is activated for this contact
	 */
	private PresenceInfo presenceInfo;
	
	/**
	 * Contact identifier
	 */
	private ContactId mContact;
	
	/**
	 * Display name of RCS contact
	 */
	private String displayName;
	
	/**
	 * Registration state
	 */
	private RegistrationState registrationState = RegistrationState.UNKNOWN;
	
	/**
	 * RCS status
	 */
	private RcsStatus rcsStatus = RcsStatus.NOT_RCS;
	
	/**
	 * RCS status timestamp
	 */
	private long rcsStatusTimestamp = 0L;
	
	/**
	 * Blocking state
	 */
	private BlockingState blockingState = BlockingState.NONE;
	
	/**
	 * Blocking timestamp
	 */
	private long blockingTs = -1L;

	/**
	 * Constructor
	 */
	public ContactInfo() {
	}

    /**
	 * Constructor
	 * 
	 * @param contactInfo
	 */
	public ContactInfo(ContactInfo info) {
		mContact = info.getContact();
		registrationState = info.getRegistrationState();
		rcsStatus = info.getRcsStatus();
		rcsStatusTimestamp = info.getRcsStatusTimestamp();
		capabilities = info.capabilities;
		presenceInfo = info.getPresenceInfo();
		displayName = info.getDisplayName();
		blockingState = info.getBlockingState();
		blockingTs = info.getBlockingTimestamp();
	}

    /**
	 * Set the capabilities
	 * 
	 * @param capabilities Capabilities
	 */
	public void setCapabilities(Capabilities capabilities) {
		this.capabilities = capabilities;
	}
	
	/**
	 * Returns the capabilities
	 * 
	 * @return Capabilities
	 */
	public Capabilities getCapabilities(){
		return capabilities;
	}
	
    /**
	 * Set the presence info
	 * 
	 * @param info Presence info
	 */
	public void setPresenceInfo(PresenceInfo info) {
		presenceInfo = info;
	}
	
	/**
	 * Returns the presence info
	 * 
	 * @return PresenceInfo
	 */
	public PresenceInfo getPresenceInfo(){
		return presenceInfo;
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
	public ContactId getContact(){
		return mContact;
	}
	
    /**
	 * Set the RCS status
	 * 
	 * @param rcsStatus RCS status
	 */
	public void setRcsStatus(RcsStatus rcsStatus) {
		this.rcsStatus = rcsStatus;
	}
	
	/**
	 * Returns the RCS status
	 * 
	 * @return rcsStatus
	 */
	public RcsStatus getRcsStatus(){
		return rcsStatus;
	}
	
    /**
	 * Set the registration state
	 * 
	 * @param int registrationState
	 */
	public void setRegistrationState(RegistrationState state) {
		registrationState = state;
	}
	
	/**
	 * Returns the registration state
	 * 
	 * @return registrationState
	 */
	public RegistrationState getRegistrationState(){
		return registrationState;
	}
	
    /**
	 * Set the RCS status timestamp
	 * 
	 * @param timestamp Last RCS status date of change
	 */
	public void setRcsStatusTimestamp(long timestamp) {
		this.rcsStatusTimestamp = timestamp;
	}
	
	/**
	 * Returns the RCS status timestamp
	 * 
	 * @return timestamp
	 */
	public long getRcsStatusTimestamp(){
		return rcsStatusTimestamp;
	}

    /**
     * Is a RCS contact
     *
     * @return true if the contact is RCS
     */
    public boolean isRcsContact() {
        return (!RcsStatus.NO_INFO.equals(rcsStatus) && !RcsStatus.NOT_RCS.equals(rcsStatus));

    }

	/**
	 * Returns the RCS display name
	 * 
	 * @return the RCS display name
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Sets the RCS display name
	 * 
	 * @param displayName
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
    /**
	 * Set the blocking state
	 * 
	 * @param state State
	 */
	public void setBlockingState(BlockingState state) {
		blockingState = state;
	}
	
	/**
	 * Returns the blocking state
	 * 
	 * @return State
	 */
	public BlockingState getBlockingState(){
		return blockingState;
	}
	
	/**
	 * Returns the blocking timestamp
	 * 
	 * @return Timestamp
	 */
	public long getBlockingTimestamp(){
		return blockingTs;
	}	

	/**
	 * Set the blocking timestamp
	 * 
	 * @param ts Timestamp
	 */
	public void setBlockingTimestamp(long ts){
		this.blockingTs = ts;
	}	
	
	/**
	 * Returns a string representation of the object
	 * 
	 * @return String
	 */
	public String toString() {
		String result =  "Contact=" + mContact +
			", Status=" + rcsStatus +
			", State=" + registrationState +
			", Timestamp=" + rcsStatusTimestamp + 
			", Blocked=" + blockingState +
			", Blocked at=" + blockingTs;
		if (capabilities != null) {
			result += ", Capabilities=" + capabilities.toString();
		}
		if (presenceInfo != null) {
			result += ", Presence=" + presenceInfo.toString();
		}
		return result;
	}
}
