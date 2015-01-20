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

package com.gsma.services.rcs;

import android.util.SparseArray;

import com.gsma.services.rcs.contacts.ContactId;

/**
 * This class represents the common configuration of RCS Services
 * 
 * @author YPLO6403
 */
public class CommonServiceConfiguration {

	private final ICommonServiceConfiguration mIConfig;

	/**
	 * The messaging client mode
	 */
	public enum MessagingMode {
		/**
		 * Messaging mode not defined
		 */
		NONE(0),
		/**
		 * Messaging mode integrated
		 */
		INTEGRATED(1),
		/**
		 * Messaging mode converged
		 */
		CONVERGED(2),
		/**
		 * Messaging mode seamless
		 */
		SEAMLESS(3);

		private int mValue;

		private static SparseArray<MessagingMode> mValueToEnum = new SparseArray<MessagingMode>();
		static {
			for (MessagingMode entry : MessagingMode.values()) {
				mValueToEnum.put(entry.toInt(), entry);
			}
		}

		private MessagingMode(int value) {
			mValue = value;
		}

		/**
		 * Gets integer value associated to MessagingMode instance
		 * 
		 * @return value
		 */
		public final int toInt() {
			return mValue;
		}

		/**
		 * Returns a MessagingMode instance for the specified integer value.
		 * 
		 * @param value
		 * @return instance
		 */
		public static MessagingMode valueOf(int value) {
			MessagingMode entry = mValueToEnum.get(value);
			if (entry != null) {
				return entry;
			}
			throw new IllegalArgumentException(new StringBuilder("No enum const class ")
					.append(MessagingMode.class.getName()).append(".").append(value).toString());

		}

	};

	/**
	 * The messaging method
	 */
	public enum MessagingMethod {
		/**
		 * Messaging method automatic
		 */
		AUTOMATIC(0),
		/**
		 * Messaging method RCS
		 */
		RCS(1),
		/**
		 * Messaging method non RCS
		 */
		NON_RCS(2);

		private int mValue;

		private static SparseArray<MessagingMethod> mValueToEnum = new SparseArray<MessagingMethod>();
		static {
			for (MessagingMethod entry : MessagingMethod.values()) {
				mValueToEnum.put(entry.toInt(), entry);
			}
		}

		private MessagingMethod(int value) {
			mValue = value;
		}

		/**
		 * Gets integer value associated to MessagingMethod instance
		 * 
		 * @return value
		 */
		public final int toInt() {
			return mValue;
		}

		/**
		 * Returns a MessagingMethod instance for the specified integer value.
		 * 
		 * @param value
		 * @return instance
		 */
		public static MessagingMethod valueOf(int value) {
			MessagingMethod entry = mValueToEnum.get(value);
			if (entry != null) {
				return entry;
			}
			throw new IllegalArgumentException(new StringBuilder("No enum const class ")
					.append(MessagingMethod.class.getName()).append(".").append(value).toString());

		}

	};

	/**
	 * Constructor
	 * 
	 * @param iConfig
	 *            ICommonServiceConfiguration instance
	 * @hide
	 */
	/* package private */CommonServiceConfiguration(ICommonServiceConfiguration iConfig) {
		mIConfig = iConfig;
	}

	/**
	 * Returns True if RCS configuration is valid.
	 * 
	 * @return True if RCS configuration is valid.
	 * @throws RcsServiceException
	 */
	public boolean isConfigValid() throws RcsServiceException {
		try {
			return mIConfig.isConfigValid();
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}

	/**
	 * Returns the display name associated to the RCS user account.<br>
	 * The display name may be updated by the end user via the RCS settings application.
	 * 
	 * @return Display name
	 * @throws RcsServiceException
	 */
	public String getMyDisplayName() throws RcsServiceException {
		try {
			return mIConfig.getMyDisplayName();
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}

	/**
	 * Sets the display name associated to the RCS user account.
	 * 
	 * @param name
	 *            the new display name
	 * @throws RcsServiceException
	 */
	public void setMyDisplayName(String name) throws RcsServiceException {
		try {
			mIConfig.setMyDisplayName(name);
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}

	/**
	 * Returns the user contact Identifier (i.e. username part of the IMPU).
	 * 
	 * @return the contact ID
	 * @throws RcsServiceException
	 */
	public ContactId getMyContactId() throws RcsServiceException {
		try {
			return mIConfig.getMyContactId();
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}

	/**
	 * Returns the messaging client mode which can be INTEGRATED, CONVERGED, SEAMLESS or NONE.
	 * 
	 * @return the messaging client mode
	 * @throws RcsServiceException
	 */
	public MessagingMode getMessagingUX() throws RcsServiceException {
		try {
			int messagingMode = mIConfig.getMessagingUX();
			return MessagingMode.valueOf(messagingMode);
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}

	/**
	 * Returns the default messaging method which can be AUTOMATIC, RCS or NON_RCS.
	 * 
	 * @return the default messaging method
	 * @throws RcsServiceException
	 */
	public MessagingMethod getDefaultMessagingMethod() throws RcsServiceException {
		try {
			int defaultMessagingMethod = mIConfig.getDefaultMessagingMethod();
			return MessagingMethod.valueOf(defaultMessagingMethod);
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}

	/**
	 * Sets the default messaging method.
	 * 
	 * @param method
	 *            the default messaging method which can be AUTOMATIC, RCS or NON_RCS.
	 * @throws RcsServiceException
	 */
	public void setDefaultMessagingMethod(MessagingMethod method) throws RcsServiceException {
		try {
			mIConfig.setDefaultMessagingMethod(method.toInt());
		} catch (Exception e) {
			throw new RcsServiceException(e);
		}
	}

}