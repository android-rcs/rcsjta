/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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

package com.orangelabs.rcs.provider.settings;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;

import com.orangelabs.rcs.core.ims.service.capability.Capabilities;
import com.orangelabs.rcs.core.ims.service.extension.ServiceExtensionManager;
import com.orangelabs.rcs.provider.settings.RcsSettingsData.AuthenticationProcedure;
import com.orangelabs.rcs.provider.settings.RcsSettingsData.ConfigurationMode;
import com.orangelabs.rcs.provider.settings.RcsSettingsData.DefaultMessagingMethod;
import com.orangelabs.rcs.provider.settings.RcsSettingsData.FileTransferProtocol;
import com.orangelabs.rcs.provider.settings.RcsSettingsData.GsmaRelease;
import com.orangelabs.rcs.provider.settings.RcsSettingsData.ImSessionStartMode;
import com.orangelabs.rcs.provider.settings.RcsSettingsData.ImageResizeOption;
import com.orangelabs.rcs.provider.settings.RcsSettingsData.MessagingMode;
import com.orangelabs.rcs.provider.settings.RcsSettingsData.NetworkAccessType;

/**
 * RCS settings
 *
 * @author jexa7410
 * @author yplo6403
 *
 */
public class RcsSettings {

	/**
	 * Minimum number of participants in a group chat
	 */
	private static final int MINIMUM_GC_PARTICIPANTS = 2;

	/**
	 * The maximum length of the Group Chat subject
	 */
	private static final int GROUP_CHAT_SUBJECT_MAX_LENGTH = 50;

	private static final String WHERE_CLAUSE = new StringBuilder(RcsSettingsData.KEY_KEY).append("=?").toString();

	// Purposely put in comments. Remove comment strongly impact performance.
//	 /**
//	 * The logger
//	 */
//	 private static final Logger logger = Logger.getLogger(RcsSettings.class.getSimpleName());

	/**
	 * Current instance
	 */
	private static volatile RcsSettings instance;

	/**
	 * Content resolver
	 */
	final private ContentResolver mContentResolver;

	/**
	 * A cache for storing settings in order to increase performance
	 */
	final private Map<String,String> mCache;

	/**
	 * Create instance
	 *
	 * @param ctx
	 *            Context
	 */
	public static void createInstance(Context ctx) {
		if (instance != null) {
			return;
		}
		synchronized (RcsSettings.class) {
			if (instance == null) {
				instance = new RcsSettings(ctx);;
			}
		}
	}

	/**
	 * Returns instance
	 *
	 * @return Instance
	 */
	public static RcsSettings getInstance() {
		return instance;
	}

	/**
	 * Constructor
	 *
	 * @param ctx
	 *            Application context
	 */
	private RcsSettings(Context ctx) {
		super();
		mContentResolver = ctx.getContentResolver();
		mCache = new HashMap<String, String>();
	}

	/**
	 * Read boolean parameter
	 * <p>
	 * If parsing of the value fails, method return false.
	 * 
	 * @param key
	 *            the key field
	 * @param defaultValude
	 *            the default value
	 * @return the value field
	 */
	private boolean readBoolean(String key, boolean defaultValue) {
		try {
			return Boolean.parseBoolean(readParameter(key));
		} catch (Exception e) {
			return defaultValue;
		}
	}

	/**
	 * Write boolean parameter
	 * 
	 * @param key
	 *            the key field
	 * @param value
	 *            the boolean value
	 */
	public void writeBoolean(String key, Boolean value) {
		writeParameter(key, value.toString());
	}

	/**
	 * Read int parameter
	 * <p>
	 * If parsing of the value fails, method return default value.
	 * 
	 * @param key
	 *            the key field
	 * @param defaultValue
	 *            the default value
	 * @return the value field
	 */
	private int readInteger(String key, int defaultValue) {
		return readInteger(key, defaultValue, true);
	}
	
	/**
	 * Read int parameter
	 * <p>
	 * If parsing of the value fails, method return default value.
	 * 
	 * @param key
	 *            the key field
	 * @param defaultValue
	 *            the default value
	 * @param tryReadFromCache
	 *            try to read from cache
	 * @return the value field
	 */
	private int readInteger(String key, int defaultValue, boolean tryReadFromCache) {
		try {
			String result = readParameter(key, tryReadFromCache);
			// Purposely put in comments. Remove comment strongly impact performance.
			// if (logger.isActivated()) {
			// logger.debug("readInteger "+key+"="+result);
			// }
			return Integer.parseInt(result);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	/**
	 * Read String parameter
	 * 
	 * @param key
	 *            the key field
	 * @param defaultValue
	 *            the default value
	 * 
	 * @return the value field or defaultValue (if read fails)
	 */
	private String readString(String key, String defaultValue) {
		return readString(key, defaultValue, true);
	}
	
	/**
	 * Read String parameter
	 * 
	 * @param key
	 *            the key field
	 * @param defaultValue
	 *            the default value
	 * @param tryReadFromCache
	 *            Try to read from the cache
	 * 
	 * @return the value field or defaultValue (if read fails)
	 */
	private String readString(String key, String defaultValue, boolean tryReadFromCache) {
		try {
			return readParameter(key, tryReadFromCache);
		} catch (Exception e) {
			return defaultValue;
		}
	}

	/**
	 * Write integer parameter
	 * 
	 * @param key
	 *            the key field
	 * @param value
	 *            the integer value
	 */
	public void writeInteger(String key, Integer value) {
		writeInteger(key, value, true);
	}
	
	/**
	 * Write integer parameter
	 * 
	 * @param key
	 *            the key field
	 * @param value
	 *            the integer value
	 * @param updateCache
	 *            update the cache
	 */
	public void writeInteger(String key, Integer value, boolean updateCache) {
		// Purposely put in comments. Remove comment strongly impact performance.
		// if (logger.isActivated()) {
		// logger.debug("writeInteger "+key+"="+value);
		// }
		writeParameter(key, value.toString(), updateCache);
	}

	/**
	 * Read a parameter
	 *
	 * @param key
	 *            Key
	 * @return Value
	 */
	public String readParameter(String key) {
		return readParameter(key, true);
	}
	
	/**
	 * Read a parameter
	 *
	 * @param key
	 *            Key
	 * @param tryReadFromCache
	 *            Try to read parameter from cache
	 * @return Value
	 */
	public String readParameter(String key, boolean tryReadFromCache) {
		if (instance == null) {
			throw new IllegalStateException("RcsInstance not created");
		}
		String value = null;
		if (tryReadFromCache) {
			// First read value from cache
			value = mCache.get(key);
			if (value != null) {
				return value;
			}
		}
		// If value is null then query database
		Cursor c = null;
		try {
			String[] whereArg = new String[] { key };
			c = mContentResolver.query(RcsSettingsData.CONTENT_URI, null, WHERE_CLAUSE, whereArg, null);
			if (c.moveToFirst()) {
				value = c.getString(c.getColumnIndexOrThrow(RcsSettingsData.KEY_VALUE));
				// Update cache
				mCache.put(key, value);
				return value;
			} else {
				return null;
			}
		} catch (Exception e) {
			return null;
		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	/**
	 * Write a string setting parameter
	 * @param key
	 * @param value
	 * @return the number of rows updated
	 */
	public int writeParameter(String key, String value) {
		return writeParameter(key,value,true);
	}
	
	/**
	 * Write a parameter
	 *
	 * @param key
	 *            Key
	 * @param value
	 *            Value
	 * @param updateCache
	 * @return the number of rows updated
	 */
	public int writeParameter(String key, String value, boolean updateCache) {
		if (instance == null || value == null) {
			return 0;
		}
		ContentValues values = new ContentValues();
		values.put(RcsSettingsData.KEY_VALUE, value);
		String[] whereArgs = new String[] { key };
		int count = mContentResolver.update(RcsSettingsData.CONTENT_URI, values, WHERE_CLAUSE, whereArgs);
		if (count != 0 && updateCache) {
			// Put in cache
			mCache.put(key, value);
		}
		return count;
	}

	/**
	 * Is RCS service activated
	 *
	 * @return Boolean
	 */
	public boolean isServiceActivated() {
		return readBoolean(RcsSettingsData.SERVICE_ACTIVATED, RcsSettingsData.DEFAULT_SERVICE_ACTIVATED);
	}

	/**
	 * Set the RCS service activation state
	 *
	 * @param state
	 *            State
	 */
	public void setServiceActivationState(boolean state) {
		writeBoolean(RcsSettingsData.SERVICE_ACTIVATED, state);
	}

	/**
	 * Get the ringtone for presence invitation
	 *
	 * @return Ringtone URI or null if there is no ringtone
	 */
	public String getPresenceInvitationRingtone() {
		return readString(RcsSettingsData.PRESENCE_INVITATION_RINGTONE, RcsSettingsData.DEFAULT_PRESENCE_INVITATION_RINGTONE);
	}

	/**
	 * Set the presence invitation ringtone
	 *
	 * @param uri
	 *            Ringtone URI
	 */
	public void setPresenceInvitationRingtone(String uri) {
		writeParameter(RcsSettingsData.PRESENCE_INVITATION_RINGTONE, uri);
	}

	/**
	 * Is phone vibrate for presence invitation
	 *
	 * @return Boolean
	 */
	public boolean isPhoneVibrateForPresenceInvitation() {
		return readBoolean(RcsSettingsData.PRESENCE_INVITATION_VIBRATE, RcsSettingsData.DEFAULT_PRESENCE_INVITATION_VIBRATE);
	}

	/**
	 * Set phone vibrate for presence invitation
	 *
	 * @param vibrate
	 *            Vibrate state
	 */
	public void setPhoneVibrateForPresenceInvitation(boolean vibrate) {
		writeBoolean(RcsSettingsData.PRESENCE_INVITATION_VIBRATE, vibrate);
	}

	/**
	 * Get the ringtone for CSh invitation
	 *
	 * @return Ringtone URI or null if there is no ringtone
	 */
	public String getCShInvitationRingtone() {
		return readString(RcsSettingsData.CSH_INVITATION_RINGTONE, RcsSettingsData.DEFAULT_CSH_INVITATION_RINGTONE);
	}

	/**
	 * Set the CSh invitation ringtone
	 *
	 * @param uri
	 *            Ringtone URI
	 */
	public void setCShInvitationRingtone(String uri) {
		writeParameter(RcsSettingsData.CSH_INVITATION_RINGTONE, uri);
	}

	/**
	 * Is phone vibrate for CSh invitation
	 *
	 * @return Boolean
	 */
	public boolean isPhoneVibrateForCShInvitation() {
		return readBoolean(RcsSettingsData.CSH_INVITATION_VIBRATE, RcsSettingsData.DEFAULT_CSH_INVITATION_VIBRATE);
	}

	/**
	 * Set phone vibrate for CSh invitation
	 *
	 * @param vibrate
	 *            Vibrate state
	 */
	public void setPhoneVibrateForCShInvitation(boolean vibrate) {
		writeBoolean(RcsSettingsData.CSH_INVITATION_VIBRATE, vibrate);
	}

	/**
	 * Is phone beep if the CSh available
	 *
	 * @return Boolean
	 */
	public boolean isPhoneBeepIfCShAvailable() {
		return readBoolean(RcsSettingsData.CSH_AVAILABLE_BEEP, RcsSettingsData.DEFAULT_CSH_AVAILABLE_BEEP);
	}

	/**
	 * Set phone beep if CSh available
	 *
	 * @param beep
	 *            Beep state
	 */
	public void setPhoneBeepIfCShAvailable(boolean beep) {
		writeBoolean(RcsSettingsData.CSH_AVAILABLE_BEEP, beep);
	}

	/**
	 * Get the ringtone for file transfer invitation
	 *
	 * @return Ringtone URI or null if there is no ringtone
	 */
	public String getFileTransferInvitationRingtone() {
		return readString(RcsSettingsData.FILETRANSFER_INVITATION_RINGTONE, RcsSettingsData.DEFAULT_FT_INVITATION_RINGTONE);
	}

	/**
	 * Set the file transfer invitation ringtone
	 *
	 * @param uri
	 *            Ringtone URI
	 */
	public void setFileTransferInvitationRingtone(String uri) {
		writeParameter(RcsSettingsData.FILETRANSFER_INVITATION_RINGTONE, uri);
	}

	/**
	 * Is phone vibrate for file transfer invitation
	 *
	 * @return Boolean
	 */
	public boolean isPhoneVibrateForFileTransferInvitation() {
		return readBoolean(RcsSettingsData.FILETRANSFER_INVITATION_VIBRATE, RcsSettingsData.DEFAULT_FT_INVITATION_VIBRATE);
	}

	/**
	 * Set phone vibrate for file transfer invitation
	 *
	 * @param vibrate
	 *            Vibrate state
	 */
	public void setPhoneVibrateForFileTransferInvitation(boolean vibrate) {
		writeParameter(RcsSettingsData.FILETRANSFER_INVITATION_VIBRATE, Boolean.toString(vibrate));
	}

	/**
	 * Get the ringtone for chat invitation
	 *
	 * @return Ringtone URI or null if there is no ringtone
	 */
	public String getChatInvitationRingtone() {
		return readString(RcsSettingsData.CHAT_INVITATION_RINGTONE, RcsSettingsData.DEFAULT_CHAT_INVITATION_RINGTONE);
	}

	/**
	 * Set the chat invitation ringtone
	 *
	 * @param uri
	 *            Ringtone URI
	 */
	public void setChatInvitationRingtone(String uri) {
		writeParameter(RcsSettingsData.CHAT_INVITATION_RINGTONE, uri);
	}

	/**
	 * Is phone vibrate for chat invitation
	 *
	 * @return Boolean
	 */
	public boolean isPhoneVibrateForChatInvitation() {
		return readBoolean(RcsSettingsData.CHAT_INVITATION_VIBRATE, RcsSettingsData.DEFAULT_CHAT_INVITATION_VIBRATE);
	}

	/**
	 * Set phone vibrate for chat invitation
	 *
	 * @param vibrate
	 *            Vibrate state
	 */
	public void setPhoneVibrateForChatInvitation(boolean vibrate) {
		writeBoolean(RcsSettingsData.CHAT_INVITATION_VIBRATE, vibrate);
	}

	/**
	 * Is send displayed notification activated
	 *
	 * @return Boolean
	 */
	public boolean isRespondToDisplayReports() {
		return readBoolean(RcsSettingsData.CHAT_RESPOND_TO_DISPLAY_REPORTS, RcsSettingsData.DEFAULT_CHAT_RESPOND_TO_DISPLAY_REPORTS);
	}

	/**
	 * Set send displayed notification
	 *
	 * @param state
	 */
	public void setRespondToDisplayReports(boolean state) {
		writeBoolean(RcsSettingsData.CHAT_RESPOND_TO_DISPLAY_REPORTS, state);
	}

	/**
	 * Get the pre-defined freetext 1
	 *
	 * @return String
	 */
	public String getPredefinedFreetext1() {
		return readString(RcsSettingsData.FREETEXT1, null);
	}

	/**
	 * Set the pre-defined freetext 1
	 *
	 * @param txt
	 *            Text
	 */
	public void setPredefinedFreetext1(String txt) {
		writeParameter(RcsSettingsData.FREETEXT1, txt);
	}

	/**
	 * Get the pre-defined freetext 2
	 *
	 * @return String
	 */
	public String getPredefinedFreetext2() {
		return readString(RcsSettingsData.FREETEXT2, null);
	}

	/**
	 * Set the pre-defined freetext 2
	 *
	 * @param txt
	 *            Text
	 */
	public void setPredefinedFreetext2(String txt) {
		writeParameter(RcsSettingsData.FREETEXT2, txt);
	}

	/**
	 * Get the pre-defined freetext 3
	 *
	 * @return String
	 */
	public String getPredefinedFreetext3() {
		return readString(RcsSettingsData.FREETEXT3, null);
	}

	/**
	 * Set the pre-defined freetext 3
	 *
	 * @param txt
	 *            Text
	 */
	public void setPredefinedFreetext3(String txt) {
		writeParameter(RcsSettingsData.FREETEXT3, txt);
	}

	/**
	 * Get the pre-defined freetext 4
	 *
	 * @return String
	 */
	public String getPredefinedFreetext4() {
		return readString(RcsSettingsData.FREETEXT4, null);
	}

	/**
	 * Set the pre-defined freetext 4
	 *
	 * @param txt
	 *            Text
	 */
	public void setPredefinedFreetext4(String txt) {
		writeParameter(RcsSettingsData.FREETEXT4, txt);
	}

	/**
	 * Get the min battery level
	 *
	 * @return Battery level in percentage
	 */
	public int getMinBatteryLevel() {
		return readInteger(RcsSettingsData.MIN_BATTERY_LEVEL, RcsSettingsData.DEFAULT_MIN_BATTERY_LEVEL);
	}

	/**
	 * Set the min battery level
	 *
	 * @param level
	 *            Battery level in percentage
	 */
	public void setMinBatteryLevel(int level) {
		writeInteger(RcsSettingsData.MIN_BATTERY_LEVEL, level);
	}

	/**
	 * Get the min storage capacity
	 *
	 * @return Capacity in kilobytes
	 */
	public int getMinStorageCapacity() {
		return readInteger(RcsSettingsData.MIN_STORAGE_CAPACITY, RcsSettingsData.DEFAULT_MIN_STORAGE_CAPACITY);
	}

	/**
	 * Set the min storage capacity
	 *
	 * @param capacity
	 *            Capacity in kilobytes
	 */
	public void setMinStorageCapacity(int capacity) {
		writeInteger(RcsSettingsData.MIN_STORAGE_CAPACITY, capacity);
	}

	/**
	 * Get user profile username (i.e. username part of the IMPU)
	 *
	 * @return Username part of SIP-URI
	 */
	public String getUserProfileImsUserName() {
		return readString(RcsSettingsData.USERPROFILE_IMS_USERNAME, RcsSettingsData.DEFAULT_USERPROFILE_IMS_USERNAME);
	}

	/**
	 * Set user profile IMS username (i.e. username part of the IMPU)
	 *
	 * @param value
	 *            Value
	 */
	public void setUserProfileImsUserName(String value) {
		writeParameter(RcsSettingsData.USERPROFILE_IMS_USERNAME, value);
	}

	/**
	 * Get the value of the MSISDN
	 *
	 * @return MSISDN
	 */
	public String getMsisdn() {
		return readString(RcsSettingsData.MSISDN, RcsSettingsData.DEFAULT_MSISDN);
	}

	/**
	 * Set the value of the MSISDN
	 * @param value 
	 */
	public void setMsisdn(String value) {
		writeParameter(RcsSettingsData.MSISDN, value);
	}

	/**
	 * Get user profile IMS display name associated to IMPU
	 *
	 * @return String
	 */
	public String getUserProfileImsDisplayName() {
		return readString(RcsSettingsData.USERPROFILE_IMS_DISPLAY_NAME, RcsSettingsData.DEFAULT_USERPROFILE_IMS_DISPLAY_NAME, false);
	}

	/**
	 * Set user profile IMS display name associated to IMPU
	 *
	 * @param value
	 *            Value
	 */
	public void setUserProfileImsDisplayName(String value) {
		writeParameter(RcsSettingsData.USERPROFILE_IMS_DISPLAY_NAME, value, false);
	}

	/**
	 * Get user profile IMS private Id (i.e. IMPI)
	 *
	 * @return SIP-URI
	 */
	public String getUserProfileImsPrivateId() {
		return readString(RcsSettingsData.USERPROFILE_IMS_PRIVATE_ID, RcsSettingsData.DEFAULT_USERPROFILE_IMS_PRIVATE_ID);
	}

	/**
	 * Set user profile IMS private Id (i.e. IMPI)
	 *
	 * @param uri
	 *            SIP-URI
	 */
	public void setUserProfileImsPrivateId(String uri) {
		writeParameter(RcsSettingsData.USERPROFILE_IMS_PRIVATE_ID, uri);
	}

	/**
	 * Get user profile IMS password
	 *
	 * @return String
	 */
	public String getUserProfileImsPassword() {
		return readString(RcsSettingsData.USERPROFILE_IMS_PASSWORD, RcsSettingsData.DEFAULT_USERPROFILE_IMS_PASSWORD);
	}

	/**
	 * Set user profile IMS password
	 *
	 * @param pwd
	 *            Password
	 */
	public void setUserProfileImsPassword(String pwd) {
		writeParameter(RcsSettingsData.USERPROFILE_IMS_PASSWORD, pwd);
	}

	/**
	 * Get user profile IMS realm
	 *
	 * @return String
	 */
	public String getUserProfileImsRealm() {
		return readString(RcsSettingsData.USERPROFILE_IMS_REALM, RcsSettingsData.DEFAULT_USERPROFILE_IMS_REALM);
	}

	/**
	 * Set user profile IMS realm
	 *
	 * @param realm
	 *            Realm
	 */
	public void setUserProfileImsRealm(String realm) {
		writeParameter(RcsSettingsData.USERPROFILE_IMS_REALM, realm);
	}

	/**
	 * Get user profile IMS home domain
	 *
	 * @return Domain
	 */
	public String getUserProfileImsDomain() {
		return readString(RcsSettingsData.USERPROFILE_IMS_HOME_DOMAIN, RcsSettingsData.DEFAULT_USERPROFILE_IMS_HOME_DOMAIN);
	}

	/**
	 * Set user profile IMS home domain
	 *
	 * @param domain
	 *            Domain
	 */
	public void setUserProfileImsDomain(String domain) {
		writeParameter(RcsSettingsData.USERPROFILE_IMS_HOME_DOMAIN, domain);
	}

	/**
	 * Get IMS proxy address for mobile access
	 *
	 * @return Address
	 */
	public String getImsProxyAddrForMobile() {
		return readString(RcsSettingsData.IMS_PROXY_ADDR_MOBILE, RcsSettingsData.DEFAULT_IMS_PROXY_ADDR_MOBILE);
	}

	/**
	 * Set IMS proxy address for mobile access
	 *
	 * @param addr
	 *            Address
	 */
	public void setImsProxyAddrForMobile(String addr) {
		writeParameter(RcsSettingsData.IMS_PROXY_ADDR_MOBILE, addr);
	}

	/**
	 * Get IMS proxy port for mobile access
	 *
	 * @return Port
	 */
	public int getImsProxyPortForMobile() {
		return readInteger(RcsSettingsData.IMS_PROXY_PORT_MOBILE, RcsSettingsData.DEFAULT_IMS_PROXY_PORT_MOBILE);
	}

	/**
	 * Set IMS proxy port for mobile access
	 *
	 * @param port
	 *            Port number
	 */
	public void setImsProxyPortForMobile(int port) {
		writeInteger(RcsSettingsData.IMS_PROXY_PORT_MOBILE, port);
	}

	/**
	 * Get IMS proxy address for Wi-Fi access
	 *
	 * @return Address
	 */
	public String getImsProxyAddrForWifi() {
		return readString(RcsSettingsData.IMS_PROXY_ADDR_WIFI, RcsSettingsData.DEFAULT_IMS_PROXY_ADDR_WIFI);
	}

	/**
	 * Set IMS proxy address for Wi-Fi access
	 *
	 * @param addr
	 *            Address
	 */
	public void setImsProxyAddrForWifi(String addr) {
		writeParameter(RcsSettingsData.IMS_PROXY_ADDR_WIFI, addr);
	}

	/**
	 * Get IMS proxy port for Wi-Fi access
	 *
	 * @return Port
	 */
	public int getImsProxyPortForWifi() {
		return readInteger(RcsSettingsData.IMS_PROXY_PORT_WIFI, RcsSettingsData.DEFAULT_IMS_PROXY_PORT_WIFI);
	}

	/**
	 * Set IMS proxy port for Wi-Fi access
	 *
	 * @param port
	 *            Port number
	 */
	public void setImsProxyPortForWifi(int port) {
		writeInteger(RcsSettingsData.IMS_PROXY_PORT_WIFI, port);
	}

	/**
	 * Get XDM server address
	 *
	 * @return Address as <host>:<port>/<root>
	 */
	public String getXdmServer() {
		return readString(RcsSettingsData.XDM_SERVER, RcsSettingsData.DEFAULT_XDM_SERVER);
	}

	/**
	 * Set XDM server address
	 *
	 * @param addr
	 *            Address as <host>:<port>/<root>
	 */
	public void setXdmServer(String addr) {
		writeParameter(RcsSettingsData.XDM_SERVER, addr);
	}

	/**
	 * Get XDM server login
	 *
	 * @return String value
	 */
	public String getXdmLogin() {
		return readString(RcsSettingsData.XDM_LOGIN, RcsSettingsData.DEFAULT_XDM_LOGIN);
	}

	/**
	 * Set XDM server login
	 *
	 * @param value
	 *            Value
	 */
	public void setXdmLogin(String value) {
		writeParameter(RcsSettingsData.XDM_LOGIN, value);
	}

	/**
	 * Get XDM server password
	 *
	 * @return String value
	 */
	public String getXdmPassword() {
		return readString(RcsSettingsData.XDM_PASSWORD, RcsSettingsData.DEFAULT_XDM_PASSWORD);
	}

	/**
	 * Set XDM server password
	 *
	 * @param value
	 *            Value
	 */
	public void setXdmPassword(String value) {
		writeParameter(RcsSettingsData.XDM_PASSWORD, value);
	}

	/**
	 * Get file transfer HTTP server address
	 *
	 * @return Address
	 */
	public String getFtHttpServer() {
		return readString(RcsSettingsData.FT_HTTP_SERVER, RcsSettingsData.DEFAULT_FT_HTTP_SERVER);
	}

	/**
	 * Set file transfer HTTP server address
	 *
	 * @param addr
	 *            Address
	 */
	public void setFtHttpServer(String addr) {
		writeParameter(RcsSettingsData.FT_HTTP_SERVER, addr);
	}

	/**
	 * Get file transfer HTTP server login
	 *
	 * @return String value
	 */
	public String getFtHttpLogin() {
		return readString(RcsSettingsData.FT_HTTP_LOGIN, RcsSettingsData.DEFAULT_FT_HTTP_LOGIN);
	}

	/**
	 * Set file transfer HTTP server login
	 *
	 * @param value
	 *            Value
	 */
	public void setFtHttpLogin(String value) {
		writeParameter(RcsSettingsData.FT_HTTP_LOGIN, value);
	}

	/**
	 * Get file transfer HTTP server password
	 *
	 * @return String value
	 */
	public String getFtHttpPassword() {
		return readString(RcsSettingsData.FT_HTTP_PASSWORD, RcsSettingsData.DEFAULT_FT_HTTP_PASSWORD);
	}

	/**
	 * Set file transfer HTTP server password
	 *
	 * @param value
	 *            Value
	 */
	public void setFtHttpPassword(String value) {
		writeParameter(RcsSettingsData.FT_HTTP_PASSWORD, value);
	}

	/**
	 * Get file transfer protocol
	 *
	 * @return FileTransferProtocol
	 */
	public FileTransferProtocol getFtProtocol() {
		String protocol = readString(RcsSettingsData.FT_PROTOCOL, null);
		try {
			return FileTransferProtocol.valueOf(protocol);
		} catch (Exception e) {
			return FileTransferProtocol.valueOf(RcsSettingsData.DEFAULT_FT_PROTOCOL);
		}
	}

	/**
	 * Set file transfer protocol
	 *
	 * @param protocol
	 */
	public void setFtProtocol(FileTransferProtocol protocol) {
		writeParameter(RcsSettingsData.FT_PROTOCOL, protocol.name());
	}

	/**
	 * Get IM conference URI
	 *
	 * @return SIP-URI
	 */
	public String getImConferenceUri() {
		return readString(RcsSettingsData.IM_CONF_URI, RcsSettingsData.DEFAULT_IM_CONF_URI);
	}

	/**
	 * Set IM conference URI
	 *
	 * @param uri
	 *            SIP-URI
	 */
	public void setImConferenceUri(String uri) {
		writeParameter(RcsSettingsData.IM_CONF_URI, uri);
	}

	/**
	 * Get end user confirmation request URI
	 *
	 * @return SIP-URI
	 */
	public String getEndUserConfirmationRequestUri() {
		return readString(RcsSettingsData.ENDUSER_CONFIRMATION_URI, RcsSettingsData.DEFAULT_ENDUSER_CONFIRMATION_URI);
	}

	/**
	 * Set end user confirmation request
	 *
	 * @param uri
	 *            SIP-URI
	 */
	public void setEndUserConfirmationRequestUri(String uri) {
		writeParameter(RcsSettingsData.ENDUSER_CONFIRMATION_URI, uri);
	}

	/**
	 * Get my capabilities
	 *
	 * @return capability
	 */
	public Capabilities getMyCapabilities() {
		Capabilities capabilities = new Capabilities();

		// Add default capabilities
		capabilities.setCsVideoSupport(isCsVideoSupported());
		capabilities.setFileTransferSupport(isFileTransferSupported());
		capabilities.setFileTransferHttpSupport(isFileTransferHttpSupported());
		capabilities.setImageSharingSupport(isImageSharingSupported());
		capabilities.setImSessionSupport(isImSessionSupported());
		capabilities.setPresenceDiscoverySupport(isPresenceDiscoverySupported());
		capabilities.setSocialPresenceSupport(isSocialPresenceSupported());
		capabilities.setVideoSharingSupport(isVideoSharingSupported());
		capabilities.setGeolocationPushSupport(isGeoLocationPushSupported());
		capabilities.setFileTransferThumbnailSupport(isFileTransferThumbnailSupported());
		capabilities.setFileTransferStoreForwardSupport(isFileTransferStoreForwardSupported());
		capabilities.setIPVoiceCallSupport(isIPVoiceCallSupported());
		capabilities.setIPVideoCallSupport(isIPVideoCallSupported());
		capabilities.setGroupChatStoreForwardSupport(isGroupChatStoreForwardSupported());
		capabilities.setSipAutomata(isSipAutomata());
		capabilities.setTimestampOfLastRequest(Capabilities.INVALID_TIMESTAMP);
		capabilities.setTimestampOfLastRefresh(System.currentTimeMillis());
		// Add extensions
		capabilities.setSupportedExtensions(getSupportedRcsExtensions());
		return capabilities;
	}

	/**
	 * Get max photo-icon size
	 *
	 * @return Size in kilobytes
	 */
	public int getMaxPhotoIconSize() {
		return readInteger(RcsSettingsData.MAX_PHOTO_ICON_SIZE, RcsSettingsData.DEFAULT_MAX_PHOTO_ICON_SIZE);
	}

	/**
	 * Get max freetext length
	 *
	 * @return Number of char
	 */
	public int getMaxFreetextLength() {
		return readInteger(RcsSettingsData.MAX_FREETXT_LENGTH, RcsSettingsData.DEFAULT_MAX_FREETXT_LENGTH);
	}

	/**
	 * Get max number of participants in a group chat
	 *
	 * @return Number of participants
	 */
	public int getMaxChatParticipants() {
		return readInteger(RcsSettingsData.MAX_CHAT_PARTICIPANTS, RcsSettingsData.DEFAULT_MAX_CHAT_PARTICIPANTS);
	}

	/**
	 * @return minimum number of participants in a Group Chat
	 */
	public int getMinGroupChatParticipants() {
		return MINIMUM_GC_PARTICIPANTS;
	}

	/**
	 * Get max length of a chat message
	 *
	 * @return Number of char
	 */
	public int getMaxChatMessageLength() {
		return readInteger(RcsSettingsData.MAX_CHAT_MSG_LENGTH, RcsSettingsData.DEFAULT_MAX_CHAT_MSG_LENGTH);
	}

	/**
	 * Get max length of a group chat message
	 *
	 * @return Number of char
	 */
	public int getMaxGroupChatMessageLength() {
		return readInteger(RcsSettingsData.MAX_GROUPCHAT_MSG_LENGTH, RcsSettingsData.DEFAULT_MAX_GC_MSG_LENGTH);
	}

	/**
	 * Get idle duration of a chat session
	 *
	 * @return Duration in seconds
	 */
	public int getChatIdleDuration() {
		return readInteger(RcsSettingsData.CHAT_IDLE_DURATION, RcsSettingsData.DEFAULT_CHAT_IDLE_DURATION);
	}

	/**
	 * Get max file transfer size
	 *
	 * @return Size in kilobytes
	 */
	public int getMaxFileTransferSize() {
		return readInteger(RcsSettingsData.MAX_FILE_TRANSFER_SIZE, RcsSettingsData.DEFAULT_MAX_FT_SIZE);
	}

	/**
	 * Get warning threshold for max file transfer size
	 *
	 * @return Size in kilobytes
	 */
	public int getWarningMaxFileTransferSize() {
		return readInteger(RcsSettingsData.WARN_FILE_TRANSFER_SIZE, RcsSettingsData.DEFAULT_WARN_FT_SIZE);
	}

	/**
	 * Get max image share size
	 *
	 * @return Size in kilobytes
	 */
	public int getMaxImageSharingSize() {
		return readInteger(RcsSettingsData.MAX_IMAGE_SHARE_SIZE, RcsSettingsData.DEFAULT_MAX_ISH_SIZE);
	}

	/**
	 * Get max duration of a video share
	 *
	 * @return Duration in seconds
	 */
	public int getMaxVideoShareDuration() {
		return readInteger(RcsSettingsData.MAX_VIDEO_SHARE_DURATION, RcsSettingsData.DEFAULT_MAX_VSH_DURATION);
	}

	/**
	 * Get max number of simultaneous chat sessions
	 *
	 * @return Number of sessions
	 */
	public int getMaxChatSessions() {
		return readInteger(RcsSettingsData.MAX_CHAT_SESSIONS, RcsSettingsData.DEFAULT_MAX_CHAT_SESSIONS);
	}

	/**
	 * Get max number of simultaneous file transfer sessions
	 *
	 * @return Number of sessions
	 */
	public int getMaxFileTransferSessions() {
		return readInteger(RcsSettingsData.MAX_FILE_TRANSFER_SESSIONS, RcsSettingsData.DEFAULT_MAX_FT_SESSIONS);
	}

	/**
	 * Get max number of simultaneous outgoing file transfer sessions
	 *
	 * @return Number of sessions
	 */
	public int getMaxConcurrentOutgoingFileTransferSessions() {
		return readInteger(RcsSettingsData.MAX_CONCURRENT_OUTGOING_FILE_TRANSFERS,
				RcsSettingsData.DEFAULT_MAX_CONCURRENT_OUTGOING_FT_SESSIONS);
	}

	/**
	 * Get max number of simultaneous IP call sessions
	 *
	 * @return Number of sessions
	 */
	public int getMaxIPCallSessions() {
		return readInteger(RcsSettingsData.MAX_IP_CALL_SESSIONS, RcsSettingsData.DEFAULT_MAX_IP_CALL_SESSIONS);
	}

	/**
	 * Is SMS fallback service activated
	 *
	 * @return Boolean
	 */
	public boolean isSmsFallbackServiceActivated() {
		return readBoolean(RcsSettingsData.SMS_FALLBACK_SERVICE, RcsSettingsData.DEFAULT_SMS_FALLBACK_SERVICE);
	}

	/**
	 * Is chat invitation auto accepted
	 *
	 * @return Boolean
	 */
	public boolean isChatAutoAccepted() {
		return readBoolean(RcsSettingsData.AUTO_ACCEPT_CHAT, RcsSettingsData.DEFAULT_AUTO_ACCEPT_CHAT);
	}

	/**
	 * Is group chat invitation auto accepted
	 *
	 * @return Boolean
	 */
	public boolean isGroupChatAutoAccepted() {
		return readBoolean(RcsSettingsData.AUTO_ACCEPT_GROUP_CHAT, RcsSettingsData.DEFAULT_AUTO_ACCEPT_GC);
	}

	/**
	 * Is file transfer invitation auto accepted
	 *
	 * @return Boolean
	 */
	public boolean isFileTransferAutoAccepted() {
		return readBoolean(RcsSettingsData.AUTO_ACCEPT_FILE_TRANSFER, RcsSettingsData.DEFAULT_AUTO_ACCEPT_FT);
	}

	/**
	 * Is Store & Forward service warning activated
	 *
	 * @return Boolean
	 */
	public boolean isStoreForwardWarningActivated() {
		return readBoolean(RcsSettingsData.WARN_SF_SERVICE, RcsSettingsData.DEFAULT_WARN_SF_SERVICE);
	}

	/**
	 * Get IM session start mode
	 * 
	 * @return the IM session start mode
	 *         <p>
	 *         <ul>
	 *         <li>0 (RCS-e default): The 200 OK is sent when the receiver consumes the notification opening the chat window.
	 *         <li>1 (RCS default): The 200 OK is sent when the receiver starts to type a message back in the chat window.
	 *         <li>2: The 200 OK is sent when the receiver presses the button to send a message (that is the message will be
	 *         buffered in the client until the MSRP session is established). Note: as described in section 3.2, the parameter only
	 *         affects the behavior for 1-to-1 sessions in case no session between the parties has been established yet.
	 *         </ul>
	 * 
	 */
	public ImSessionStartMode getImSessionStartMode() {
		return ImSessionStartMode.valueOf(readInteger(RcsSettingsData.IM_SESSION_START,
				ImSessionStartMode.ON_OPENING.toInt()));
	}

	/**
	 * Get max number of entries per contact in the chat log
	 * 
	 * @return Number
	 */
	public int getMaxChatLogEntriesPerContact() {
		return readInteger(RcsSettingsData.MAX_CHAT_LOG_ENTRIES, RcsSettingsData.DEFAULT_MAX_CHAT_LOG_ENTRIES);
	}

	/**
	 * Get max number of entries per contact in the richcall log
	 * 
	 * @return Number
	 */
	public int getMaxRichcallLogEntriesPerContact() {
		return readInteger(RcsSettingsData.MAX_RICHCALL_LOG_ENTRIES, RcsSettingsData.DEFAULT_MAX_RICHCALL_LOG_ENTRIES);
	}

	/**
	 * Get max number of entries per contact in the IP call log
	 * 
	 * @return Number
	 */
	public int getMaxIPCallLogEntriesPerContact() {
		return readInteger(RcsSettingsData.MAX_IPCALL_LOG_ENTRIES, RcsSettingsData.DEFAULT_MAX_IPCALL_LOG_ENTRIES);
	}

	/**
	 * Get polling period used before each IMS service check (e.g. test subscription state for presence service)
	 *
	 * @return Period in seconds
	 */
	public int getImsServicePollingPeriod() {
		return readInteger(RcsSettingsData.IMS_SERVICE_POLLING_PERIOD, RcsSettingsData.DEFAULT_IMS_SERVICE_POLLING_PERIOD);
	}

	/**
	 * Get default SIP listening port
	 *
	 * @return Port
	 */
	public int getSipListeningPort() {
		return readInteger(RcsSettingsData.SIP_DEFAULT_PORT, RcsSettingsData.DEFAULT_SIP_DEFAULT_PORT);
	}

	/**
	 * Get default SIP protocol for mobile
	 * 
	 * @return Protocol (udp | tcp | tls)
	 */
	public String getSipDefaultProtocolForMobile() {
		return readString(RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_MOBILE, RcsSettingsData.DEFAULT_SIP_DEFAULT_PROTOCOL_FOR_MOBILE);
	}

	/**
	 * Get default SIP protocol for wifi
	 * 
	 * @return Protocol (udp | tcp | tls)
	 */
	public String getSipDefaultProtocolForWifi() {
		return readString(RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_WIFI, RcsSettingsData.DEFAULT_SIP_DEFAULT_PROTOCOL_FOR_WIFI);
	}

	/**
	 * Get TLS Certificate root
	 * 
	 * @return Path of the certificate
	 */
	public String getTlsCertificateRoot() {
		return readString(RcsSettingsData.TLS_CERTIFICATE_ROOT, RcsSettingsData.DEFAULT_TLS_CERTIFICATE_ROOT);
	}

	/**
	 * Get TLS Certificate intermediate
	 * 
	 * @return Path of the certificate
	 */
	public String getTlsCertificateIntermediate() {
		return readString(RcsSettingsData.TLS_CERTIFICATE_INTERMEDIATE,RcsSettingsData.DEFAULT_TLS_CERTIFICATE_INTERMEDIATE);
	}

	/**
	 * Get SIP transaction timeout used to wait SIP response
	 * 
	 * @return Timeout in seconds
	 */
	public int getSipTransactionTimeout() {
		return readInteger(RcsSettingsData.SIP_TRANSACTION_TIMEOUT, RcsSettingsData.DEFAULT_SIP_TRANSACTION_TIMEOUT);
	}

	/**
	 * Get default MSRP port
	 *
	 * @return Port
	 */
	public int getDefaultMsrpPort() {
		return readInteger(RcsSettingsData.MSRP_DEFAULT_PORT, RcsSettingsData.DEFAULT_MSRP_DEFAULT_PORT);
	}

	/**
	 * Get default RTP port
	 *
	 * @return Port
	 */
	public int getDefaultRtpPort() {
		return readInteger(RcsSettingsData.RTP_DEFAULT_PORT, RcsSettingsData.DEFAULT_RTP_DEFAULT_PORT);
	}

	/**
	 * Get MSRP transaction timeout used to wait MSRP response
	 *
	 * @return Timeout in seconds
	 */
	public int getMsrpTransactionTimeout() {
		return readInteger(RcsSettingsData.MSRP_TRANSACTION_TIMEOUT, RcsSettingsData.DEFAULT_MSRP_TRANSACTION_TIMEOUT);
	}

	/**
	 * Get default expire period for REGISTER
	 *
	 * @return Period in seconds
	 */
	public int getRegisterExpirePeriod() {
		return readInteger(RcsSettingsData.REGISTER_EXPIRE_PERIOD, RcsSettingsData.DEFAULT_REGISTER_EXPIRE_PERIOD);
	}

	/**
	 * Get registration retry base time
	 *
	 * @return Time in seconds
	 */
	public int getRegisterRetryBaseTime() {
		return readInteger(RcsSettingsData.REGISTER_RETRY_BASE_TIME, RcsSettingsData.DEFAULT_REGISTER_RETRY_BASE_TIME);
	}

	/**
	 * Get registration retry max time
	 *
	 * @return Time in seconds
	 */
	public int getRegisterRetryMaxTime() {
		return readInteger(RcsSettingsData.REGISTER_RETRY_MAX_TIME, RcsSettingsData.DEFAULT_REGISTER_RETRY_MAX_TIME);
	}

	/**
	 * Get default expire period for PUBLISH
	 *
	 * @return Period in seconds
	 */
	public int getPublishExpirePeriod() {
		return readInteger(RcsSettingsData.PUBLISH_EXPIRE_PERIOD, RcsSettingsData.DEFAULT_PUBLISH_EXPIRE_PERIOD);
	}

	/**
	 * Get revoke timeout before to unrevoke a revoked contact
	 *
	 * @return Timeout in seconds
	 */
	public int getRevokeTimeout() {
		return readInteger(RcsSettingsData.REVOKE_TIMEOUT, RcsSettingsData.DEFAULT_REVOKE_TIMEOUT);
	}

	/**
	 * Get IMS authentication procedure for mobile access
	 *
	 * @return Authentication procedure
	 */
	public AuthenticationProcedure getImsAuthenticationProcedureForMobile() {
		String procedure = readString(RcsSettingsData.IMS_AUTHENT_PROCEDURE_MOBILE, null);
		try {
			return AuthenticationProcedure.valueOf(procedure);
		} catch (Exception e) {
			return AuthenticationProcedure.valueOf(RcsSettingsData.DEFAULT_IMS_AUTHENT_PROCEDURE_MOBILE);
		}
	}

	/**
	 * Set the authentication procedure for mobile
	 * @param procedure
	 */
	public void setImsAuthenticationProcedureForMobile(AuthenticationProcedure procedure) {
		writeParameter(RcsSettingsData.IMS_AUTHENT_PROCEDURE_MOBILE, procedure.name());
	}

	/**
	 * Get IMS authentication procedure for Wi-Fi access
	 *
	 * @return Authentication procedure
	 */
	public AuthenticationProcedure getImsAuthenticationProcedureForWifi() {
		String procedure = readString(RcsSettingsData.IMS_AUTHENT_PROCEDURE_WIFI, null);
		try {
			return AuthenticationProcedure.valueOf(procedure);
		} catch (Exception e) {
			return AuthenticationProcedure.valueOf(RcsSettingsData.DEFAULT_IMS_AUTHENT_PROCEDURE_WIFI);
		}
	}

	/**
	 * Set the authentication procedure for Wi-Fi
	 * @param procedure
	 */
	public void setImsAuhtenticationProcedureForWifi(AuthenticationProcedure procedure) {
		writeParameter(RcsSettingsData.IMS_AUTHENT_PROCEDURE_WIFI, procedure.name());
	}
	
	/**
	 * Is Tel-URI format used
	 *
	 * @return Boolean
	 */
	public boolean isTelUriFormatUsed() {
		return readBoolean(RcsSettingsData.TEL_URI_FORMAT, RcsSettingsData.DEFAULT_TEL_URI_FORMAT);
	}

	/**
	 * Get ringing period
	 *
	 * @return Period in seconds
	 */
	public int getRingingPeriod() {
		return readInteger(RcsSettingsData.RINGING_SESSION_PERIOD, RcsSettingsData.DEFAULT_RINGING_SESSION_PERIOD);
	}

	/**
	 * Get default expire period for SUBSCRIBE
	 *
	 * @return Period in seconds
	 */
	public int getSubscribeExpirePeriod() {
		return readInteger(RcsSettingsData.SUBSCRIBE_EXPIRE_PERIOD, RcsSettingsData.DEFAULT_SUBSCRIBE_EXPIRE_PERIOD);
	}

	/**
	 * Get "Is-composing" timeout for chat service
	 *
	 * @return Timer in seconds
	 */
	public int getIsComposingTimeout() {
		return readInteger(RcsSettingsData.IS_COMPOSING_TIMEOUT, RcsSettingsData.DEFAULT_IS_COMPOSING_TIMEOUT);
	}

	/**
	 * Get default expire period for INVITE (session refresh)
	 *
	 * @return Period in seconds
	 */
	public int getSessionRefreshExpirePeriod() {
		return readInteger(RcsSettingsData.SESSION_REFRESH_EXPIRE_PERIOD, RcsSettingsData.DEFAULT_SESSION_REFRESH_EXPIRE_PERIOD);
	}

	/**
	 * Is permanente state mode activated
	 *
	 * @return Boolean
	 */
	public boolean isPermanentStateModeActivated() {
		return readBoolean(RcsSettingsData.PERMANENT_STATE_MODE, RcsSettingsData.DEFAULT_PERMANENT_STATE_MODE);
	}

	/**
	 * Is trace activated
	 *
	 * @return Boolean
	 */
	public boolean isTraceActivated() {
		return readBoolean(RcsSettingsData.TRACE_ACTIVATED, RcsSettingsData.DEFAULT_TRACE_ACTIVATED);
	}

	/**
	 * Get trace level
	 *
	 * @return trace level
	 */
	public int getTraceLevel() {
		return readInteger(RcsSettingsData.TRACE_LEVEL, RcsSettingsData.DEFAULT_TRACE_LEVEL);
	}

	/**
	 * Is media trace activated
	 *
	 * @return Boolean
	 */
	public boolean isSipTraceActivated() {
		return readBoolean(RcsSettingsData.SIP_TRACE_ACTIVATED, RcsSettingsData.DEFAULT_SIP_TRACE_ACTIVATED);
	}

	/**
	 * Get SIP trace file
	 *
	 * @return SIP trace file
	 */
	public String getSipTraceFile() {
		return readString(RcsSettingsData.SIP_TRACE_FILE, RcsSettingsData.DEFAULT_SIP_TRACE_FILE);
	}

	/**
	 * Is media trace activated
	 *
	 * @return Boolean
	 */
	public boolean isMediaTraceActivated() {
		return readBoolean(RcsSettingsData.MEDIA_TRACE_ACTIVATED, RcsSettingsData.DEFAULT_MEDIA_TRACE_ACTIVATED);
	}

	/**
	 * Get capability refresh timeout used to avoid too many requests in a short time
	 *
	 * @return Timeout in seconds
	 */
	public int getCapabilityRefreshTimeout() {
		return readInteger(RcsSettingsData.CAPABILITY_REFRESH_TIMEOUT, RcsSettingsData.DEFAULT_CAPABILITY_REFRESH_TIMEOUT);
	}

	/**
	 * Get capability expiry timeout used to decide when to refresh contact capabilities
	 *
	 * @return Timeout in seconds
	 */
	public int getCapabilityExpiryTimeout() {
		return readInteger(RcsSettingsData.CAPABILITY_EXPIRY_TIMEOUT, RcsSettingsData.DEFAULT_CAPABILITY_EXPIRY_TIMEOUT);
	}

	/**
	 * Get capability polling period used to refresh contacts capabilities
	 *
	 * @return Timeout in seconds
	 */
	public int getCapabilityPollingPeriod() {
		return readInteger(RcsSettingsData.CAPABILITY_POLLING_PERIOD, RcsSettingsData.DEFAULT_CAPABILITY_POLLING_PERIOD);
	}

	/**
	 * Is CS video supported
	 *
	 * @return Boolean
	 */
	public boolean isCsVideoSupported() {
		return readBoolean(RcsSettingsData.CAPABILITY_CS_VIDEO, RcsSettingsData.DEFAULT_CAPABILITY_CS_VIDEO);
	}

	/**
	 * Is file transfer supported
	 *
	 * @return Boolean
	 */
	public boolean isFileTransferSupported() {
		return readBoolean(RcsSettingsData.CAPABILITY_FILE_TRANSFER, RcsSettingsData.DEFAULT_CAPABILITY_FT);
	}

	/**
	 * Is file transfer via HTTP supported
	 *
	 * @return Boolean
	 */
	public boolean isFileTransferHttpSupported() {
		if ((getFtHttpServer().length() > 0) && (getFtHttpLogin().length() > 0) && (getFtHttpPassword().length() > 0)) {
			return readBoolean(RcsSettingsData.CAPABILITY_FILE_TRANSFER_HTTP, RcsSettingsData.DEFAULT_CAPABILITY_FT_HTTP);
		}
		return false;
	}

	/**
	 * Is IM session supported
	 *
	 * @return Boolean
	 */
	public boolean isImSessionSupported() {
		return readBoolean(RcsSettingsData.CAPABILITY_IM_SESSION, RcsSettingsData.DEFAULT_CAPABILITY_IM_SESSION);
	}

	/**
	 * Is IM group session supported
	 *
	 * @return Boolean
	 */
	public boolean isImGroupSessionSupported() {
		return readBoolean(RcsSettingsData.CAPABILITY_IM_GROUP_SESSION, RcsSettingsData.DEFAULT_CAPABILITY_IM_GROUP_SESSION);
	}

	/**
	 * Is image sharing supported
	 *
	 * @return Boolean
	 */
	public boolean isImageSharingSupported() {
		return readBoolean(RcsSettingsData.CAPABILITY_IMAGE_SHARING, RcsSettingsData.DEFAULT_CAPABILITY_ISH);
	}

	/**
	 * Is video sharing supported
	 *
	 * @return Boolean
	 */
	public boolean isVideoSharingSupported() {
		return readBoolean(RcsSettingsData.CAPABILITY_VIDEO_SHARING, RcsSettingsData.DEFAULT_CAPABILITY_VSH);
	}

	/**
	 * Is presence discovery supported
	 *
	 * @return Boolean
	 */
	public boolean isPresenceDiscoverySupported() {
		if (getXdmServer().length() > 0) {
			return readBoolean(RcsSettingsData.CAPABILITY_PRESENCE_DISCOVERY, RcsSettingsData.DEFAULT_CAPABILITY_PRESENCE_DISCOVERY);
		}
		return false;
	}

	/**
	 * Is social presence supported
	 *
	 * @return Boolean
	 */
	public boolean isSocialPresenceSupported() {
		if (getXdmServer().length() > 0) {
			return readBoolean(RcsSettingsData.CAPABILITY_SOCIAL_PRESENCE, RcsSettingsData.DEFAULT_CAPABILITY_SOCIAL_PRESENCE);
		}
		return false;
	}

	/**
	 * Is geolocation push supported
	 *
	 * @return Boolean
	 */
	public boolean isGeoLocationPushSupported() {
		return readBoolean(RcsSettingsData.CAPABILITY_GEOLOCATION_PUSH, RcsSettingsData.DEFAULT_CAPABILITY_GEOLOCATION_PUSH);
	}

	/**
	 * Is file transfer thumbnail supported
	 *
	 * @return Boolean
	 */
	public boolean isFileTransferThumbnailSupported() {
		// Thumbnail is only supported in HTPP.
		// The thThumbnail configuration settings is fixed 0 for MSRP per specification.
		// Refer to PDD v3 page 106.
		return isFileTransferHttpSupported();
	}

	/**
	 * Is file transfer Store & Forward supported
	 *
	 * @return Boolean
	 */
	public boolean isFileTransferStoreForwardSupported() {
		return readBoolean(RcsSettingsData.CAPABILITY_FILE_TRANSFER_SF, RcsSettingsData.DEFAULT_CAPABILITY_FT_SF);
	}

	/**
	 * Is IP voice call supported
	 *
	 * @return Boolean
	 */
	public boolean isIPVoiceCallSupported() {
		return readBoolean(RcsSettingsData.CAPABILITY_IP_VOICE_CALL, RcsSettingsData.DEFAULT_CAPABILITY_IP_VOICE_CALL);
	}

	/**
	 * Is IP video call supported
	 *
	 * @return Boolean
	 */
	public boolean isIPVideoCallSupported() {
		return readBoolean(RcsSettingsData.CAPABILITY_IP_VIDEO_CALL, RcsSettingsData.DEFAULT_CAPABILITY_IP_VIDEO_CALL);
	}

	/**
	 * Is group chat Store & Forward supported
	 *
	 * @return Boolean
	 */
	public boolean isGroupChatStoreForwardSupported() {
		return readBoolean(RcsSettingsData.CAPABILITY_GROUP_CHAT_SF, RcsSettingsData.DEFAULT_CAPABILITY_GC_SF);
	}

	/**
	 * Get set of supported RCS extensions
	 *
	 * @return the set of extensions
	 */
	public Set<String> getSupportedRcsExtensions() {
		return ServiceExtensionManager.getExtensions(readString(RcsSettingsData.CAPABILITY_RCS_EXTENSIONS,RcsSettingsData.DEFAULT_CAPABILITY_RCS_EXTENSIONS));
	}

	/**
	 * Set the set of supported RCS extensions
	 *
	 * @param extensions
	 *            Set of extensions
	 */
	public void setSupportedRcsExtensions(Set<String> extensions) {
		writeParameter(RcsSettingsData.CAPABILITY_RCS_EXTENSIONS, ServiceExtensionManager.getExtensions(extensions));
	}

	/**
	 * Is IM always-on thanks to the Store & Forward functionality
	 *
	 * @return Boolean
	 */
	public boolean isImAlwaysOn() {
		return readBoolean(RcsSettingsData.IM_CAPABILITY_ALWAYS_ON, RcsSettingsData.DEFAULT_IM_CAPABILITY_ALWAYS_ON);
	}

	/**
	 * Is File Transfer always-on thanks to the Store & Forward functionality
	 *
	 * @return Boolean
	 */
	public boolean isFtAlwaysOn() {
		return readBoolean(RcsSettingsData.FT_CAPABILITY_ALWAYS_ON, RcsSettingsData.DEFAULT_FT_CAPABILITY_ALWAYS_ON);
	}

	/**
	 * Is IM reports activated
	 *
	 * @return Boolean
	 */
	public boolean isImReportsActivated() {
		return readBoolean(RcsSettingsData.IM_USE_REPORTS, RcsSettingsData.DEFAULT_IM_USE_REPORTS);
	}

	/**
	 * Get network access
	 *
	 * @return Network type
	 */
	public NetworkAccessType getNetworkAccess() {
		int type = readInteger(RcsSettingsData.NETWORK_ACCESS, RcsSettingsData.DEFAULT_NETWORK_ACCESS);
		try {
			return NetworkAccessType.valueOf(type);
		} catch (Exception e) {
			return NetworkAccessType.valueOf(RcsSettingsData.DEFAULT_NETWORK_ACCESS);
		}
	}

	/**
	 * Set network access type
	 * @param networkAccess
	 */
	public void setNetworkAccess(NetworkAccessType networkAccess) {
		writeInteger(RcsSettingsData.NETWORK_ACCESS, networkAccess.toInt());
	}
	
	/**
	 * Get SIP timer T1
	 *
	 * @return Timer in milliseconds
	 */
	public int getSipTimerT1() {
		return readInteger(RcsSettingsData.SIP_TIMER_T1, RcsSettingsData.DEFAULT_SIP_TIMER_T1);
	}

	/**
	 * Get SIP timer T2
	 *
	 * @return Timer in milliseconds
	 */
	public int getSipTimerT2() {
		return readInteger(RcsSettingsData.SIP_TIMER_T2, RcsSettingsData.DEFAULT_SIP_TIMER_T2);
	}

	/**
	 * Get SIP timer T4
	 *
	 * @return Timer in milliseconds
	 */
	public int getSipTimerT4() {
		return readInteger(RcsSettingsData.SIP_TIMER_T4, RcsSettingsData.DEFAULT_SIP_TIMER_T4);
	}

	/**
	 * Is SIP keep-alive enabled
	 *
	 * @return Boolean
	 */
	public boolean isSipKeepAliveEnabled() {
		return readBoolean(RcsSettingsData.SIP_KEEP_ALIVE, RcsSettingsData.DEFAULT_SIP_KEEP_ALIVE);
	}

	/**
	 * Get SIP keep-alive period
	 *
	 * @return Period in seconds
	 */
	public int getSipKeepAlivePeriod() {
		return readInteger(RcsSettingsData.SIP_KEEP_ALIVE_PERIOD, RcsSettingsData.DEFAULT_SIP_KEEP_ALIVE_PERIOD);
	}

	/**
	 * Get APN used to connect to RCS platform
	 *
	 * @return APN (null means any APN may be used to connect to RCS)
	 */
	public String getNetworkApn() {
		return readString(RcsSettingsData.RCS_APN, RcsSettingsData.DEFAULT_RCS_APN);
	}

	/**
	 * Get operator authorized to connect to RCS platform
	 *
	 * @return SIM operator name (null means any SIM operator is authorized to connect to RCS)
	 */
	public String getNetworkOperator() {
		return readString(RcsSettingsData.RCS_OPERATOR, RcsSettingsData.DEFAULT_RCS_OPERATOR);
	}

	/**
	 * Is GRUU supported
	 *
	 * @return Boolean
	 */
	public boolean isGruuSupported() {
		return readBoolean(RcsSettingsData.GRUU, RcsSettingsData.DEFAULT_GRUU);
	}

	/**
	 * Is IMEI used as device ID
	 *
	 * @return Boolean
	 */
	public boolean isImeiUsedAsDeviceId() {
		return readBoolean(RcsSettingsData.USE_IMEI_AS_DEVICE_ID, RcsSettingsData.DEFAULT_USE_IMEI_AS_DEVICE_ID);
	}

	/**
	 * Is CPU Always_on activated
	 *
	 * @return Boolean
	 */
	public boolean isCpuAlwaysOn() {
		return readBoolean(RcsSettingsData.CPU_ALWAYS_ON, RcsSettingsData.DEFAULT_CPU_ALWAYS_ON);
	}

	/**
	 * Get configuration mode
	 *
	 * @return Mode MANUAL | AUTO
	 */
	public ConfigurationMode getConfigurationMode() {
		int mode = readInteger(RcsSettingsData.CONFIG_MODE, RcsSettingsData.DEFAULT_CONFIG_MODE);
		try {
			return ConfigurationMode.valueOf(mode);
		} catch (Exception e) {
			return ConfigurationMode.valueOf(RcsSettingsData.DEFAULT_CONFIG_MODE);
		}
	}
	
	/**
	 * Set configuration mode
	 *
	 * @param mode MANUAL | AUTO
	 * 
	 */
	public void setConfigurationMode(ConfigurationMode mode) {
		writeInteger(RcsSettingsData.CONFIG_MODE, mode.toInt());
	}

	/**
	 * Is Terms and conditions via provisioning is accepted
	 * 
	 * @return Boolean
	 */
	public boolean isProvisioningTermsAccepted() {
		return readBoolean(RcsSettingsData.PROVISIONING_TERMS_ACCEPTED, RcsSettingsData.DEFAULT_PROVISIONING_TERMS_ACCEPTED);
	}

	/**
	 * Get provisioning version
	 * 
	 * @return Version
	 */
	public String getProvisioningVersion() {
		return readString(RcsSettingsData.PROVISIONING_VERSION, RcsSettingsData.DEFAULT_PROVISIONING_VERSION);
	}

	/**
	 * Set provisioning version
	 * 
	 * @param version
	 *            Version
	 */
	public void setProvisioningVersion(String version) {
		writeParameter(RcsSettingsData.PROVISIONING_VERSION, version);
	}

	/**
	 * Set Terms and conditions via provisioning accepted
	 * 
	 * @param state
	 *            State
	 */
	public void setProvisioningTermsAccepted(boolean state) {
		writeBoolean(RcsSettingsData.PROVISIONING_TERMS_ACCEPTED, state);
	}

	/**
	 * Get secondary provisioning address
	 *
	 * @return Address
	 */
	public String getSecondaryProvisioningAddress() {
		return readString(RcsSettingsData.SECONDARY_PROVISIONING_ADDRESS, RcsSettingsData.DEFAULT_SECONDARY_PROV_ADDR);
	}

	/**
	 * Set secondary provisioning address
	 *
	 * @param address
	 */
	public void setSecondaryProvisioningAddress(String address) {
		writeParameter(RcsSettingsData.SECONDARY_PROVISIONING_ADDRESS, address);
	}

	/**
	 * Is secondary provisioning address only used
	 *
	 * @return Boolean
	 */
	public boolean isSecondaryProvisioningAddressOnly() {
		return readBoolean(RcsSettingsData.SECONDARY_PROVISIONING_ADDRESS_ONLY, RcsSettingsData.DEFAULT_SECONDARY_PROV_ADDR_ONLY);
	}

	/**
	 * Set secondary provisioning address only used
	 * @param value 
	 */
	public void setSecondaryProvisioningAddressOnly(boolean value) {
		writeBoolean(RcsSettingsData.SECONDARY_PROVISIONING_ADDRESS_ONLY, value);
	}

	/**
	 * Reset user profile settings
	 */
	public void resetUserProfile() {
		setUserProfileImsUserName("");
		setUserProfileImsDomain("");
		setUserProfileImsPassword("");
		setImsProxyAddrForMobile("");
		setImsProxyAddrForWifi("");
		setUserProfileImsDisplayName("");
		setUserProfileImsPrivateId("");
		setXdmLogin("");
		setXdmPassword("");
		setXdmServer("");
		setProvisioningVersion("0");
		setProvisioningToken("");
		setMsisdn("");
	}

	/**
	 * Is user profile configured
	 *
	 * @return Returns true if the configuration is valid
	 */
	public boolean isUserProfileConfigured() {
		// Check platform settings
		if (TextUtils.isEmpty(getImsProxyAddrForMobile())) {
			return false;
		}

		// Check user profile settings
		if (TextUtils.isEmpty(getUserProfileImsDomain())) {
			return false;
		}
		AuthenticationProcedure mode = getImsAuthenticationProcedureForMobile();
		switch (mode) {
		case DIGEST:
			if (TextUtils.isEmpty(getUserProfileImsUserName())) {
				return false;
			}
			if (TextUtils.isEmpty(getUserProfileImsPassword())) {
				return false;
			}
			if (TextUtils.isEmpty(getUserProfileImsPrivateId())) {
				return false;
			}
			break;
		default:
			break;
		}
		return true;
	}

	/**
	 * Is group chat activated
	 *
	 * @return Boolean
	 */
	public boolean isGroupChatActivated() {
		String value = getImConferenceUri();
		if (!TextUtils.isEmpty(value) && !value.equals(RcsSettingsData.DEFAULT_GROUP_CHAT_URI)) {
			return true;
		}
		return false;
	}

	/**
	 * Get the root directory for photos
	 * 
	 * @return Directory path
	 */
	public String getPhotoRootDirectory() {
		return readString(RcsSettingsData.DIRECTORY_PATH_PHOTOS, RcsSettingsData.DEFAULT_DIRECTORY_PATH_PHOTOS);
	}

	/**
	 * Set the root directory for photos
	 * 
	 * @param path
	 *            Directory path
	 */
	public void setPhotoRootDirectory(String path) {
		writeParameter(RcsSettingsData.DIRECTORY_PATH_PHOTOS, path);
	}

	/**
	 * Get the root directory for videos
	 * 
	 * @return Directory path
	 */
	public String getVideoRootDirectory() {
		return readString(RcsSettingsData.DIRECTORY_PATH_VIDEOS, RcsSettingsData.DEFAULT_DIRECTORY_PATH_VIDEOS);
	}

	/**
	 * Set the root directory for videos
	 * 
	 * @param path
	 *            Directory path
	 */
	public void setVideoRootDirectory(String path) {
		writeParameter(RcsSettingsData.DIRECTORY_PATH_VIDEOS, path);
	}

	/**
	 * Get the root directory for files
	 * 
	 * @return Directory path
	 */
	public String getFileRootDirectory() {
		return readString(RcsSettingsData.DIRECTORY_PATH_FILES, RcsSettingsData.DEFAULT_DIRECTORY_PATH_FILES);
	}

	/**
	 * Set the root directory for files
	 * 
	 * @param path
	 *            Directory path
	 */
	public void setFileRootDirectory(String path) {
		writeParameter(RcsSettingsData.DIRECTORY_PATH_FILES, path);
	}

	/**
	 * Is secure MSRP media over Wi-Fi
	 * 
	 * @return Boolean
	 */
	public boolean isSecureMsrpOverWifi() {
		return readBoolean(RcsSettingsData.SECURE_MSRP_OVER_WIFI, RcsSettingsData.DEFAULT_SECURE_MSRP_OVER_WIFI);
	}

	/**
	 * Is secure RTP media over Wi-Fi
	 * 
	 * @return Boolean
	 */
	public boolean isSecureRtpOverWifi() {
		return readBoolean(RcsSettingsData.SECURE_RTP_OVER_WIFI, RcsSettingsData.DEFAULT_SECURE_RTP_OVER_WIFI);
	}

	/**
	 * Get max geolocation label length
	 *
	 * @return Number of char
	 */
	public int getMaxGeolocLabelLength() {
		return readInteger(RcsSettingsData.MAX_GEOLOC_LABEL_LENGTH, RcsSettingsData.DEFAULT_MAX_GEOLOC_LABEL_LENGTH);
	}

	/**
	 * Get geolocation expiration time
	 *
	 * @return Time in seconds
	 */
	public int getGeolocExpirationTime() {
		return readInteger(RcsSettingsData.GEOLOC_EXPIRATION_TIME, RcsSettingsData.DEFAULT_GEOLOC_EXPIRATION_TIME);
	}

	/**
	 * Set provisioning token
	 * @param token
	 */
	public void setProvisioningToken(String token) {
		writeParameter(RcsSettingsData.PROVISIONING_TOKEN, token);
	}

	/**
	 * @return provisioning token
	 */
	public String getProvisioningToken() {
		return readString(RcsSettingsData.PROVISIONING_TOKEN, RcsSettingsData.DEFAULT_PROVISIONING_TOKEN);
	}

	/**
	 * Is SIP device an automata ?
	 *
	 * @return Boolean
	 */
	public boolean isSipAutomata() {
		return readBoolean(RcsSettingsData.CAPABILITY_SIP_AUTOMATA, RcsSettingsData.DEFAULT_CAPABILITY_SIP_AUTOMATA);
	}

	/**
	 * Get max file-icon size
	 *
	 * @return Size in kilobytes
	 */
	public int getMaxFileIconSize() {
		return readInteger(RcsSettingsData.MAX_FILE_ICON_SIZE, RcsSettingsData.DEFAULT_MAX_FILE_ICON_SIZE);
	}

	/**
	 * Get the GSMA release
	 * 
	 * @return the GSMA release
	 */
	public GsmaRelease getGsmaRelease() {
		int release = readInteger(RcsSettingsData.KEY_GSMA_RELEASE, RcsSettingsData.DEFAULT_KEY_GSMA_RELEASE);
		try {
			return GsmaRelease.valueOf(release);
		} catch (Exception e) {
			return GsmaRelease.valueOf(RcsSettingsData.DEFAULT_KEY_GSMA_RELEASE);
		}
	}

	/**
	 * Set the GSMA release
	 * 
	 * @param release
	 *            Release
	 */
	public void setGsmaRelease(GsmaRelease release) {
		writeInteger(RcsSettingsData.KEY_GSMA_RELEASE, release.toInt());
	}

	/**
	 * Is Albatros GSMA release
	 * 
	 * @return Boolean
	 */
	public boolean isAlbatrosRelease() {
		return (GsmaRelease.ALBATROS.equals(getGsmaRelease()));
	}

	/**
	 * Is Blackbird GSMA release
	 * 
	 * @return Boolean
	 */
	public boolean isBlackbirdRelease() {
		return (GsmaRelease.BLACKBIRD.equals(getGsmaRelease()));
	}

	/**
	 * Is IP voice call breakout supported in RCS-AA mode
	 *
	 * @return Boolean
	 */
	public boolean isIPVoiceCallBreakoutAA() {
		return readBoolean(RcsSettingsData.IPVOICECALL_BREAKOUT_AA, RcsSettingsData.DEFAULT_IPVOICECALL_BREAKOUT_AA);
	}

	/**
	 * Is IP voice call breakout supported in RCS-CS mode
	 *
	 * @return Boolean
	 */
	public boolean isIPVoiceCallBreakoutCS() {
		return readBoolean(RcsSettingsData.IPVOICECALL_BREAKOUT_CS, RcsSettingsData.DEFAULT_IPVOICECALL_BREAKOUT_CS);
	}

	/**
	 * Is IP Video Call upgrade without first tearing down the CS voice call authorized
	 *
	 * @return Boolean
	 */
	public boolean isIPVideoCallUpgradeFromCS() {
		return readBoolean(RcsSettingsData.IPVIDEOCALL_UPGRADE_FROM_CS, RcsSettingsData.DEFAULT_IPVIDEOCALL_UPGRADE_FROM_CS);
	}

	/**
	 * Is IP Video Call upgrade on capability error
	 *
	 * @return Boolean
	 */
	public boolean isIPVideoCallUpgradeOnCapError() {
		return readBoolean(RcsSettingsData.IPVIDEOCALL_UPGRADE_ON_CAPERROR, RcsSettingsData.DEFAULT_IPVIDEOCALL_UPGRADE_ON_CAPERROR);
	}

	/**
	 * Is device in RCS-CS mode authorized to upgrade to video without first tearing down CS call?
	 *
	 * @return Boolean
	 */
	public boolean isIPVideoCallAttemptEarly() {
		return readBoolean(RcsSettingsData.IPVIDEOCALL_UPGRADE_ATTEMPT_EARLY,
				RcsSettingsData.DEFAULT_IPVIDEOCALL_UPGRADE_ATTEMPT_EARLY);
	}

	/**
	 * Is TCP fallback enabled according to RFC3261 chapter 18.1.1
	 * 
	 * @return Boolean
	 */
	public boolean isTcpFallback() {
		return readBoolean(RcsSettingsData.TCP_FALLBACK, RcsSettingsData.DEFAULT_TCP_FALLBACK);
	}

	/**
	 * Get vendor name of the client
	 *
	 * @return Vendor
	 */
	public String getVendor() {
		return readString(RcsSettingsData.VENDOR_NAME, RcsSettingsData.DEFAULT_VENDOR_NAME);
	}

	/**
	 * Is RCS extensions controlled
	 * 
	 * @return Boolean
	 */
	public boolean isExtensionsControlled() {
		return readBoolean(RcsSettingsData.CONTROL_EXTENSIONS, RcsSettingsData.DEFAULT_CONTROL_EXTENSIONS);
	}

	/**
	 * Is RCS extensions allowed
	 * 
	 * @return Boolean
	 */
	public boolean isExtensionsAllowed() {
		return readBoolean(RcsSettingsData.ALLOW_EXTENSIONS, RcsSettingsData.DEFAULT_ALLOW_EXTENSIONS);
	}

	/**
	 * Get max lenght for extensions using real time messaging (MSRP)
	 * 
	 * @return Max length
	 */
	public int getMaxMsrpLengthForExtensions() {
		return readInteger(RcsSettingsData.MAX_MSRP_SIZE_EXTENSIONS, RcsSettingsData.DEFAULT_MAX_MSRP_SIZE_EXTENSIONS);
	}

	/**
	 * Set the client messaging mode
	 * 
	 * @param mode
	 *            the client messaging mode (0: CONVERGED, 1: INTEGRATED, 2: SEAMLESS, 3: NONE)
	 */
	public void setMessagingMode(MessagingMode mode) {
		writeInteger(RcsSettingsData.KEY_MESSAGING_MODE, mode.toInt());
	}

	/**
	 * Get the client messaging mode
	 * 
	 * @return the client messaging mode (0: CONVERGED, 1: INTEGRATED, 2: SEAMLESS, 3: NONE)
	 */
	public MessagingMode getMessagingMode() {
		int mode = readInteger(RcsSettingsData.KEY_MESSAGING_MODE, RcsSettingsData.DEFAULT_KEY_MESSAGING_MODE);
		try {
			return MessagingMode.valueOf(mode);
		} catch (Exception e) {
			return MessagingMode.valueOf(RcsSettingsData.DEFAULT_KEY_MESSAGING_MODE);
		}
	}

	/**
	 * Is file transfer invitation auto accepted in roaming
	 *
	 * @return Boolean
	 */
	public boolean isFileTransferAutoAcceptedInRoaming() {
		return readBoolean(RcsSettingsData.AUTO_ACCEPT_FT_IN_ROAMING, RcsSettingsData.DEFAULT_AUTO_ACCEPT_FT_IN_ROAMING);
	}

	/**
	 * Set File Transfer Auto Accepted in roaming
	 * 
	 * @param option
	 *            Option
	 */
	public void setFileTransferAutoAcceptedInRoaming(boolean option) {
		writeBoolean(RcsSettingsData.AUTO_ACCEPT_FT_IN_ROAMING, option);
	}

	/**
	 * Set File Transfer Auto Accepted in normal conditions
	 * 
	 * @param option
	 *            Option
	 */
	public void setFileTransferAutoAccepted(boolean option) {
		writeBoolean(RcsSettingsData.AUTO_ACCEPT_FILE_TRANSFER, option);
	}

	/**
	 * Is file transfer invitation auto accepted enabled (by the network)
	 *
	 * @return Boolean
	 */
	public boolean isFtAutoAcceptedModeChangeable() {
		return readBoolean(RcsSettingsData.AUTO_ACCEPT_FT_CHANGEABLE, RcsSettingsData.DEFAULT_AUTO_ACCEPT_FT_CHANGEABLE);
	}

	/**
	 * Set File Transfer Auto Accepted Mode changeable option
	 *
	 * @param option
	 *            Option
	 */
	public void setFtAutoAcceptedModeChangeable(boolean option) {
		writeBoolean(RcsSettingsData.AUTO_ACCEPT_FT_CHANGEABLE, option);
	}

	/**
	 * returns the image resize option for file transfer in the range [ALWAYS_PERFORM, ONLY_ABOVE_MAX_SIZE, ASK]
	 * 
	 * @return image resize option (0: ALWAYS_PERFORM, 1: ONLY_ABOVE_MAX_SIZE, 2: ASK)
	 */
	public ImageResizeOption getImageResizeOption() {
		int option = readInteger(RcsSettingsData.KEY_IMAGE_RESIZE_OPTION, RcsSettingsData.DEFAULT_KEY_IMAGE_RESIZE_OPTION);
		Log.w("RCS","getImageResizeOption "+option);
		try {
			return ImageResizeOption.valueOf(option);
		} catch (Exception e) {
			Log.e("RCS","getImageResizeOption "+option);
			return ImageResizeOption.valueOf(RcsSettingsData.DEFAULT_KEY_IMAGE_RESIZE_OPTION);
		}
	}

	/**
	 * Set the image resize option
	 * 
	 * @param option
	 *            the image resize option (0: ALWAYS_PERFORM, 1: ONLY_ABOVE_MAX_SIZE, 2: ASK)
	 */
	public void setImageResizeOption(ImageResizeOption option) {
		writeInteger(RcsSettingsData.KEY_IMAGE_RESIZE_OPTION, option.toInt());
	}

	/**
	 * Get the default messaging method
	 * 
	 * @return the default messaging method (0: AUTOMATIC, 1: RCS, 2: NON_RCS)
	 */
	public DefaultMessagingMethod getDefaultMessagingMethod() {
		int method = readInteger(RcsSettingsData.KEY_DEFAULT_MESSAGING_METHOD, RcsSettingsData.DEFAULT_KEY_DEFAULT_MESSAGING_METHOD, false);
		try {
			return DefaultMessagingMethod.valueOf(method);
		} catch (Exception e) {
			return DefaultMessagingMethod.valueOf(RcsSettingsData.DEFAULT_KEY_DEFAULT_MESSAGING_METHOD);
		}
	}

	/**
	 * Set default messaging method
	 * 
	 * @param method
	 *            the default messaging method (0: AUTOMATIC, 1: RCS, 2: NON_RCS)
	 */
	public void setDefaultMessagingMethod(DefaultMessagingMethod method) {
		writeInteger(RcsSettingsData.KEY_DEFAULT_MESSAGING_METHOD, method.toInt(), false);
	}

	/**
	 * Is configuration valid
	 *
	 * @return Boolean
	 */
	public boolean isConfigurationValid() {
		return readBoolean(RcsSettingsData.CONFIGURATION_VALID, RcsSettingsData.DEFAULT_CONFIGURATION_VALID);
	}

	/**
	 * Set configuration valid
	 *
	 * @param valid
	 */
	public void setConfigurationValid(boolean valid) {
		writeBoolean(RcsSettingsData.CONFIGURATION_VALID, valid);
	}

	/**
	 * @return the maximum length of subject in Group Chat
	 */
	public int getGroupChatSubjectMaxLength() {
		return GROUP_CHAT_SUBJECT_MAX_LENGTH;
	}
}
