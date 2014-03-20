/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications AB.
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
 * NOTE: This file has been modified by Sony Mobile Communications AB.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.orangelabs.rcs.provider.settings;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import com.orangelabs.rcs.core.ims.service.capability.Capabilities;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * RCS settings
 *
 * @author jexa7410
 */
public class RcsSettings {

	/**
	 * Current instance
	 */
	private static RcsSettings instance = null;
	
	/**
	 * Content resolver
	 */
	private ContentResolver cr;

	/**
	 * Database URI
	 */
	private Uri databaseUri = RcsSettingsData.CONTENT_URI;
	
    /**
     * Create instance
     *
     * @param ctx Context
     */
	public static synchronized void createInstance(Context ctx) {
		if (instance == null) {
			instance = new RcsSettings(ctx);
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
     * @param ctx Application context
     */
	private RcsSettings(Context ctx) {
		super();

        this.cr = ctx.getContentResolver();
	}

	/**
     * Read a parameter
     *
     * @param key Key
     * @return Value
     */
	public String readParameter(String key) {
		if (key == null) {
			return null;
		}

		String result = null;
        Cursor c = cr.query(databaseUri, null, RcsSettingsData.KEY_KEY + "='" + key + "'", null, null);
        if (c != null) {
        	if ((c.getCount() > 0) && c.moveToFirst()) {
	        	result = c.getString(2);
        	}
	        c.close();
        }
        return result;
	}

	/**
     * Write a parameter
     *
     * @param key Key
     * @param value Value
     */
	public void writeParameter(String key, String value) {
		if ((key == null) || (value == null)) {
			return;
		}
		
        ContentValues values = new ContentValues();
        values.put(RcsSettingsData.KEY_VALUE, value);
        String where = RcsSettingsData.KEY_KEY + "='" + key + "'";
        cr.update(databaseUri, values, where, null);
	}

	/**
     * Insert a parameter
     *
     * @param key Key
     * @param value Value
     */
	public void insertParameter(String key, String value) {
		if ((key == null) || (value == null)) {
			return;
		}

		ContentValues values = new ContentValues();
        values.put(RcsSettingsData.KEY_KEY, key);
        values.put(RcsSettingsData.KEY_VALUE, value);
        cr.insert(databaseUri, values);
	}

	/**
     * Is RCS service activated
     *
     * @return Boolean
     */
	public boolean isServiceActivated() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.SERVICE_ACTIVATED));
		}
		return result;
    }

	/**
     * Set the RCS service activation state
     *
     * @param state State
     */
	public void setServiceActivationState(boolean state) {
		if (instance != null) {
			writeParameter(RcsSettingsData.SERVICE_ACTIVATED, Boolean.toString(state));
		}
    }

	/**
     * Get the ringtone for presence invitation
     *
     * @return Ringtone URI or null if there is no ringtone
     */
	public String getPresenceInvitationRingtone() {
		String result = null;
		if (instance != null) {
			result = readParameter(RcsSettingsData.PRESENCE_INVITATION_RINGTONE);
		}
		return result;
	}

	/**
     * Set the presence invitation ringtone
     *
     * @param uri Ringtone URI
     */
	public void setPresenceInvitationRingtone(String uri) {
		if (instance != null) {
			writeParameter(RcsSettingsData.PRESENCE_INVITATION_RINGTONE, uri);
		}
	}

    /**
     * Is phone vibrate for presence invitation
     *
     * @return Boolean
     */
	public boolean isPhoneVibrateForPresenceInvitation() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.PRESENCE_INVITATION_VIBRATE));
		}
		return result;
    }

	/**
     * Set phone vibrate for presence invitation
     *
     * @param vibrate Vibrate state
     */
	public void setPhoneVibrateForPresenceInvitation(boolean vibrate) {
		if (instance != null) {
			writeParameter(RcsSettingsData.PRESENCE_INVITATION_VIBRATE, Boolean.toString(vibrate));
		}
    }

	/**
     * Get the ringtone for CSh invitation
     *
     * @return Ringtone URI or null if there is no ringtone
     */
	public String getCShInvitationRingtone() {
		String result = null;
		if (instance != null) {
			result = readParameter(RcsSettingsData.CSH_INVITATION_RINGTONE);
		}
		return result;
	}

	/**
     * Set the CSh invitation ringtone
     *
     * @param uri Ringtone URI
     */
	public void setCShInvitationRingtone(String uri) {
		if (instance != null) {
			writeParameter(RcsSettingsData.CSH_INVITATION_RINGTONE, uri);
		}
	}

    /**
     * Is phone vibrate for CSh invitation
     *
     * @return Boolean
     */
	public boolean isPhoneVibrateForCShInvitation() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.CSH_INVITATION_VIBRATE));
		}
		return result;
    }

	/**
     * Set phone vibrate for CSh invitation
     *
     * @param vibrate Vibrate state
     */
	public void setPhoneVibrateForCShInvitation(boolean vibrate) {
		if (instance != null) {
			writeParameter(RcsSettingsData.CSH_INVITATION_VIBRATE, Boolean.toString(vibrate));
		}
    }

	/**
     * Is phone beep if the CSh available
     *
     * @return Boolean
     */
	public boolean isPhoneBeepIfCShAvailable() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.CSH_AVAILABLE_BEEP));
		}
		return result;
    }

	/**
     * Set phone beep if CSh available
     *
     * @param beep Beep state
     */
	public void setPhoneBeepIfCShAvailable(boolean beep) {
		if (instance != null) {
			writeParameter(RcsSettingsData.CSH_AVAILABLE_BEEP, Boolean.toString(beep));
		}
    }

	/**
     * Get the ringtone for file transfer invitation
     *
     * @return Ringtone URI or null if there is no ringtone
     */
	public String getFileTransferInvitationRingtone() {
		String result = null;
		if (instance != null) {
			result = readParameter(RcsSettingsData.FILETRANSFER_INVITATION_RINGTONE);
		}
		return result;
	}

	/**
     * Set the file transfer invitation ringtone
     *
     * @param uri Ringtone URI
     */
	public void setFileTransferInvitationRingtone(String uri) {
		if (instance != null) {
			writeParameter(RcsSettingsData.FILETRANSFER_INVITATION_RINGTONE, uri);
		}
	}

    /**
     * Is phone vibrate for file transfer invitation
     *
     * @return Boolean
     */
	public boolean isPhoneVibrateForFileTransferInvitation() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.FILETRANSFER_INVITATION_VIBRATE));
		}
		return result;
    }

	/**
     * Set phone vibrate for file transfer invitation
     *
     * @param vibrate Vibrate state
     */
	public void setPhoneVibrateForFileTransferInvitation(boolean vibrate) {
		if (instance != null) {
			writeParameter(RcsSettingsData.FILETRANSFER_INVITATION_VIBRATE, Boolean.toString(vibrate));
		}
    }

	/**
     * Get the ringtone for chat invitation
     *
     * @return Ringtone URI or null if there is no ringtone
     */
	public String getChatInvitationRingtone() {
		String result = null;
		if (instance != null) {
			result = readParameter(RcsSettingsData.CHAT_INVITATION_RINGTONE);
		}
		return result;
	}

	/**
     * Set the chat invitation ringtone
     *
     * @param uri Ringtone URI
     */
	public void setChatInvitationRingtone(String uri) {
		if (instance != null) {
			writeParameter(RcsSettingsData.CHAT_INVITATION_RINGTONE, uri);
		}
	}

    /**
     * Is phone vibrate for chat invitation
     *
     * @return Boolean
     */
	public boolean isPhoneVibrateForChatInvitation() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.CHAT_INVITATION_VIBRATE));
		}
		return result;
    }

	/**
     * Set phone vibrate for chat invitation
     *
     * @param vibrate Vibrate state
     */
	public void setPhoneVibrateForChatInvitation(boolean vibrate) {
		if (instance != null) {
			writeParameter(RcsSettingsData.CHAT_INVITATION_VIBRATE, Boolean.toString(vibrate));
		}
	}

	/**
	 * Is send displayed notification activated
	 *
	 * @return Boolean
	 */
    public boolean isImDisplayedNotificationActivated() {
        boolean result = false;
        if (instance != null) {
            result = Boolean.parseBoolean(readParameter(RcsSettingsData.CHAT_DISPLAYED_NOTIFICATION));
        }
        return result;
    }

    /**
     * Set send displayed notification
     *
     * @param state
     */
    public void setImDisplayedNotificationActivated(boolean state) {
        if (instance != null) {
            writeParameter(RcsSettingsData.CHAT_DISPLAYED_NOTIFICATION, Boolean.toString(state));
        }
    }

    /**
     * Get the pre-defined freetext 1
     *
     * @return String
     */
	public String getPredefinedFreetext1() {
		String result = null;
		if (instance != null) {
			result = readParameter(RcsSettingsData.FREETEXT1);
		}
		return result;
	}

    /**
     * Set the pre-defined freetext 1
     *
     * @param txt Text
     */
	public void setPredefinedFreetext1(String txt) {
		if (instance != null) {
			writeParameter(RcsSettingsData.FREETEXT1, txt);
		}
	}

    /**
     * Get the pre-defined freetext 2
     *
     * @return String
     */
	public String getPredefinedFreetext2() {
		String result = null;
		if (instance != null) {
			result = readParameter(RcsSettingsData.FREETEXT2);
		}
		return result;
	}

	/**
     * Set the pre-defined freetext 2
     *
     * @param txt Text
     */
	public void setPredefinedFreetext2(String txt) {
        if (instance != null) {
            writeParameter(RcsSettingsData.FREETEXT2, txt);
        }
	}

    /**
     * Get the pre-defined freetext 3
     *
     * @return String
     */
	public String getPredefinedFreetext3() {
		String result = null;
		if (instance != null) {
			result = readParameter(RcsSettingsData.FREETEXT3);
		}
		return result;
	}

    /**
     * Set the pre-defined freetext 3
     *
     * @param txt Text
     */
	public void setPredefinedFreetext3(String txt) {
        if (instance != null) {
            writeParameter(RcsSettingsData.FREETEXT3, txt);
        }
	}

    /**
     * Get the pre-defined freetext 4
     *
     * @return String
     */
	public String getPredefinedFreetext4() {
		String result = null;
		if (instance != null) {
			result = readParameter(RcsSettingsData.FREETEXT4);
		}
		return result;
	}

	/**
     * Set the pre-defined freetext 4
     *
     * @param txt Text
     */
	public void setPredefinedFreetext4(String txt) {
        if (instance != null) {
            writeParameter(RcsSettingsData.FREETEXT4, txt);
        }
	}

    /**
     * Get the min battery level
     *
     * @return Battery level in percentage
     */
    public int getMinBatteryLevel() {
        int result = 0;
        if (instance != null) {
            try {
                result = Integer.parseInt(readParameter(RcsSettingsData.MIN_BATTERY_LEVEL));
            } catch(Exception e) {}
        }
        return result;
    }

    /**
     * Set the min battery level
     *
     * @param level Battery level in percentage
     */
    public void setMinBatteryLevel(int level) {
        if (instance != null) {
            writeParameter(RcsSettingsData.MIN_BATTERY_LEVEL, "" + level);
        }
    }

    /**
     * Get the min storage capacity
     *
     * @return Capacity in kilobytes
     */
    public int getMinStorageCapacity() {
        int result = 0;
        if (instance != null) {
            try {
                result = Integer.parseInt(readParameter(RcsSettingsData.MIN_STORAGE_CAPACITY));
            } catch(Exception e) {}
        }
        return result;
    }

    /**
     * Set the min storage capacity
     *
     * @param capacity Capacity in kilobytes
     */
    public void setMinStorageCapacity(int capacity) {
        if (instance != null) {
            writeParameter(RcsSettingsData.MIN_STORAGE_CAPACITY, "" + capacity);
        }
    }

    /**
     * Get user profile username (i.e. username part of the IMPU)
     *
     * @return Username part of SIP-URI
     */
	public String getUserProfileImsUserName() {
		String result = null;
		if (instance != null) {
			result = readParameter(RcsSettingsData.USERPROFILE_IMS_USERNAME);
		}
		return result;
    }

	/**
     * Set user profile IMS username (i.e. username part of the IMPU)
     *
     * @param value Value
     */
	public void setUserProfileImsUserName(String value) {
		if (instance != null) {
			writeParameter(RcsSettingsData.USERPROFILE_IMS_USERNAME, value);
		}
    }

    /**
     * Get the value of the MSISDN
     *
     * @return MSISDN
     */
	public String getMsisdn() {
		String result = null;
		if (instance != null) {
			result = readParameter(RcsSettingsData.MSISDN);
		}
		return result;
    }
	
	/**
     * Set the value of the MSISDN
     */
	public void setMsisdn(String value) {
		if (instance != null) {
			writeParameter(RcsSettingsData.MSISDN, value);
		}
    }

	/**
     * Get user profile IMS display name associated to IMPU
     *
     * @return String
     */
	public String getUserProfileImsDisplayName() {
		String result = null;
		if (instance != null) {
			result = readParameter(RcsSettingsData.USERPROFILE_IMS_DISPLAY_NAME);
		}
		return result;
    }

	/**
     * Set user profile IMS display name associated to IMPU
     *
     * @param value Value
     */
	public void setUserProfileImsDisplayName(String value) {
		if (instance != null) {
			writeParameter(RcsSettingsData.USERPROFILE_IMS_DISPLAY_NAME, value);
		}
    }

	/**
     * Get user profile IMS private Id (i.e. IMPI)
     *
     * @return SIP-URI
     */
	public String getUserProfileImsPrivateId() {
		String result = null;
		if (instance != null) {
			result = readParameter(RcsSettingsData.USERPROFILE_IMS_PRIVATE_ID);
		}
		return result;
    }

	/**
     * Set user profile IMS private Id (i.e. IMPI)
     *
     * @param uri SIP-URI
     */
	public void setUserProfileImsPrivateId(String uri) {
		if (instance != null) {
			writeParameter(RcsSettingsData.USERPROFILE_IMS_PRIVATE_ID, uri);
		}
    }

	/**
     * Get user profile IMS password
     *
     * @return String
     */
	public String getUserProfileImsPassword() {
		String result = null;
		if (instance != null) {
			result = readParameter(RcsSettingsData.USERPROFILE_IMS_PASSWORD);
		}
		return result;
    }

	/**
     * Set user profile IMS password
     *
     * @param pwd Password
     */
	public void setUserProfileImsPassword(String pwd) {
		if (instance != null) {
			writeParameter(RcsSettingsData.USERPROFILE_IMS_PASSWORD, pwd);
		}
    }

	/**
     * Get user profile IMS realm
     *
     * @return String
     */
	public String getUserProfileImsRealm() {
		String result = null;
		if (instance != null) {
			result = readParameter(RcsSettingsData.USERPROFILE_IMS_REALM);
		}
		return result;
    }

	/**
     * Set user profile IMS realm
     *
     * @param realm Realm
     */
	public void setUserProfileImsRealm(String realm) {
		if (instance != null) {
			writeParameter(RcsSettingsData.USERPROFILE_IMS_REALM, realm);
		}
    }

	/**
     * Get user profile IMS home domain
     *
     * @return Domain
     */
	public String getUserProfileImsDomain() {
		String result = null;
		if (instance != null) {
			result = readParameter(RcsSettingsData.USERPROFILE_IMS_HOME_DOMAIN);
		}
		return result;
    }

	/**
     * Set user profile IMS home domain
     *
     * @param domain Domain
     */
	public void setUserProfileImsDomain(String domain) {
		if (instance != null) {
			writeParameter(RcsSettingsData.USERPROFILE_IMS_HOME_DOMAIN, domain);
		}
	}

    /**
     * Get IMS proxy address for mobile access
     *
     * @return Address
     */
	public String getImsProxyAddrForMobile() {
		String result = null;
		if (instance != null) {
			result = readParameter(RcsSettingsData.IMS_PROXY_ADDR_MOBILE);
		}
		return result;
    }

	/**
     * Set IMS proxy address for mobile access
     *
     * @param addr Address
     */
	public void setImsProxyAddrForMobile(String addr) {
		if (instance != null) {
			writeParameter(RcsSettingsData.IMS_PROXY_ADDR_MOBILE, addr);
		}
	}

    /**
     * Get IMS proxy port for mobile access
     *
     * @return Port
     */
	public int getImsProxyPortForMobile() {
		int result = 5060;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.IMS_PROXY_PORT_MOBILE));
			} catch(Exception e) {}
		}
		return result;
    }

	/**
     * Set IMS proxy port for mobile access
     *
     * @param port Port number
     */
	public void setImsProxyPortForMobile(int port) {
		if (instance != null) {
			writeParameter(RcsSettingsData.IMS_PROXY_PORT_MOBILE, "" + port);
		}
	}

	/**
     * Get IMS proxy address for Wi-Fi access
     *
     * @return Address
     */
	public String getImsProxyAddrForWifi() {
		String result = null;
		if (instance != null) {
			result = readParameter(RcsSettingsData.IMS_PROXY_ADDR_WIFI);
		}
		return result;
    }

	/**
     * Set IMS proxy address for Wi-Fi access
     *
     * @param addr Address
     */
	public void setImsProxyAddrForWifi(String addr) {
		if (instance != null) {
			writeParameter(RcsSettingsData.IMS_PROXY_ADDR_WIFI, addr);
		}
	}

	/**
     * Get IMS proxy port for Wi-Fi access
     *
     * @return Port
     */
	public int getImsProxyPortForWifi() {
		int result = 5060;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.IMS_PROXY_PORT_WIFI));
			} catch(Exception e) {}
		}
		return result;
    }

	/**
     * Set IMS proxy port for Wi-Fi access
     *
     * @param port Port number
     */
	public void setImsProxyPortForWifi(int port) {
		if (instance != null) {
			writeParameter(RcsSettingsData.IMS_PROXY_PORT_WIFI, "" + port);
		}
	}

	/**
     * Get XDM server address
     *
     * @return Address as <host>:<port>/<root>
     */
	public String getXdmServer() {
		String result = null;
		if (instance != null) {
			result = readParameter(RcsSettingsData.XDM_SERVER);
		}
		return result;
    }

	/**
     * Set XDM server address
     *
     * @param addr Address as <host>:<port>/<root>
     */
	public void setXdmServer(String addr) {
		if (instance != null) {
			writeParameter(RcsSettingsData.XDM_SERVER, addr);
		}
	}

    /**
     * Get XDM server login
     *
     * @return String value
     */
	public String getXdmLogin() {
		String result = null;
		if (instance != null) {
			result = readParameter(RcsSettingsData.XDM_LOGIN);
		}
		return result;
    }

	/**
     * Set XDM server login
     *
     * @param value Value
     */
	public void setXdmLogin(String value) {
		if (instance != null) {
			writeParameter(RcsSettingsData.XDM_LOGIN, value);
		}
	}

    /**
     * Get XDM server password
     *
     * @return String value
     */
	public String getXdmPassword() {
		String result = null;
		if (instance != null) {
			result = readParameter(RcsSettingsData.XDM_PASSWORD);
		}
		return result;
    }

	/**
     * Set XDM server password
     *
     * @param value Value
     */
	public void setXdmPassword(String value) {
		if (instance != null) {
			writeParameter(RcsSettingsData.XDM_PASSWORD, value);
		}
	}

	/**
     * Get file transfer HTTP server address
     *
     * @return Address
     */
	public String getFtHttpServer() {
		String result = null;
		if (instance != null) {
			result = readParameter(RcsSettingsData.FT_HTTP_SERVER);
		}
		return result;
    }

	/**
     * Set file transfer HTTP server address
     *
     * @param addr Address 
     */
	public void setFtHttpServer(String addr) {
		if (instance != null) {
			writeParameter(RcsSettingsData.FT_HTTP_SERVER, addr);
		}
	}

    /**
     * Get file transfer HTTP server login
     *
     * @return String value
     */
	public String getFtHttpLogin() {
		String result = null;
		if (instance != null) {
			result = readParameter(RcsSettingsData.FT_HTTP_LOGIN);
		}
		return result;
    }

	/**
     * Set file transfer HTTP server login
     *
     * @param value Value
     */
	public void setFtHttpLogin(String value) {
		if (instance != null) {
			writeParameter(RcsSettingsData.FT_HTTP_LOGIN, value);
		}
	}

    /**
     * Get file transfer HTTP server password
     *
     * @return String value
     */
	public String getFtHttpPassword() {
		String result = null;
		if (instance != null) {
			result = readParameter(RcsSettingsData.FT_HTTP_PASSWORD);
		}
		return result;
    }

	/**
     * Set file transfer HTTP server password
     *
     * @param value Value
     */
	public void setFtHttpPassword(String value) {
		if (instance != null) {
			writeParameter(RcsSettingsData.FT_HTTP_PASSWORD, value);
		}
	}

    /**
     * Get file transfer protocol
     *
     * @return String value
     */
    public String getFtProtocol() {
        String result = null;
        if (instance != null) {
            result = readParameter(RcsSettingsData.FT_PROTOCOL);
        }
        return result;
    }

    /**
     * Set file transfer protocol
     *
     * @param value Value
     */
    public void setFtProtocol(String value) {
        if (instance != null) {
            writeParameter(RcsSettingsData.FT_PROTOCOL, value);
        }
    }

    /**
     * Get IM conference URI
     *
     * @return SIP-URI
     */
	public String getImConferenceUri() {
		String result = null;
		if (instance != null) {
			result = readParameter(RcsSettingsData.IM_CONF_URI);
		}
		return result;
    }

	/**
     * Set IM conference URI
     *
     * @param uri SIP-URI
     */
	public void setImConferenceUri(String uri) {
		if (instance != null) {
			writeParameter(RcsSettingsData.IM_CONF_URI, uri);
		}
	}

    /**
     * Get end user confirmation request URI
     *
     * @return SIP-URI
     */
	public String getEndUserConfirmationRequestUri() {
		String result = null;
		if (instance != null) {
			result = readParameter(RcsSettingsData.ENDUSER_CONFIRMATION_URI);
		}
		return result;
    }

	/**
     * Set end user confirmation request
     *
     * @param uri SIP-URI
     */
	public void setEndUserConfirmationRequestUri(String uri) {
		if (instance != null) {
			writeParameter(RcsSettingsData.ENDUSER_CONFIRMATION_URI, uri);
		}
	}
	
	/**
     * Get country code
     *
     * @return Country code
     */
	public String getCountryCode() {
		String result = null;
		if (instance != null) {
			result = readParameter(RcsSettingsData.COUNTRY_CODE);
		}
		return result;
    }

	/**
     * Set country code
     *
     * @param code Country code
     */
	public void setCountryCode(String code) {
		if (instance != null) {
			writeParameter(RcsSettingsData.COUNTRY_CODE, code);
		}
    }

	/**
     * Get country area code
     *
     * @return Area code
     */
	public String getCountryAreaCode() {
		String result = null;
		if (instance != null) {
			result = readParameter(RcsSettingsData.COUNTRY_AREA_CODE);
		}
		return result;
    }

	/**
     * Set country area code
     *
     * @param code Area code
     */
	public void setCountryAreaCode(String code) {
		if (instance != null) {
			writeParameter(RcsSettingsData.COUNTRY_AREA_CODE, code);
		}
    }

	/**
     * Get my capabilities
     *
     * @return capability
     */
	public Capabilities getMyCapabilities(){
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
		capabilities.setTimestamp(System.currentTimeMillis());

		// Add extensions
		String exts = getSupportedRcsExtensions();
		if ((exts != null) && (exts.length() > 0)) {
			String[] ext = exts.split(",");
			for(int i=0; i < ext.length; i++) {
				capabilities.addSupportedExtension(ext[i]);
			}
		}

		return capabilities;
	}

	/**
     * Get max photo-icon size
     *
     * @return Size in kilobytes
     */
	public int getMaxPhotoIconSize() {
		int result = 256;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.MAX_PHOTO_ICON_SIZE));
			} catch(Exception e) {}
		}
		return result;
	}

    /**
     * Get max freetext length
     *
     * @return Number of char
     */
	public int getMaxFreetextLength() {
		int result = 100;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.MAX_FREETXT_LENGTH));
			} catch(Exception e) {}
		}
		return result;
	}

    /**
     * Get max number of participants in a group chat
     *
     * @return Number of participants
     */
	public int getMaxChatParticipants() {
		int result = 10;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.MAX_CHAT_PARTICIPANTS));
			} catch(Exception e) {}
		}
		return result;
	}

	/**
     * Get max length of a chat message
     *
     * @return Number of char
     */
	public int getMaxChatMessageLength() {
		int result = 100;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.MAX_CHAT_MSG_LENGTH));
			} catch(Exception e) {}
		}
		return result;
	}

	/**
     * Get max length of a group chat message
     *
     * @return Number of char
     */
	public int getMaxGroupChatMessageLength() {
		int result = 100;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.MAX_GROUPCHAT_MSG_LENGTH));
			} catch(Exception e) {}
		}
		return result;
	}
	
	/**
     * Get idle duration of a chat session
     *
     * @return Duration in seconds
     */
	public int getChatIdleDuration() {
		int result = 120;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.CHAT_IDLE_DURATION));
			} catch(Exception e) {}
		}
		return result;
	}

    /**
     * Get max file transfer size
     *
     * @return Size in kilobytes
     */
	public int getMaxFileTransferSize() {
		int result = 2048;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.MAX_FILE_TRANSFER_SIZE));
			} catch(Exception e) {}
		}
		return result;
	}

    /**
     * Get warning threshold for max file transfer size
     *
     * @return Size in kilobytes
     */
	public int getWarningMaxFileTransferSize() {
		int result = 2048;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.WARN_FILE_TRANSFER_SIZE));
			} catch(Exception e) {}
		}
		return result;
	}

	/**
     * Get max image share size
     *
     * @return Size in kilobytes
     */
	public int getMaxImageSharingSize() {
		int result = 2048;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.MAX_IMAGE_SHARE_SIZE));
			} catch(Exception e) {}
		}
		return result;
	}

    /**
     * Get max duration of a video share
     *
     * @return Duration in seconds
     */
	public int getMaxVideoShareDuration() {
		int result = 600;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.MAX_VIDEO_SHARE_DURATION));
			} catch(Exception e) {}
		}
		return result;
	}

    /**
     * Get max number of simultaneous chat sessions
     *
     * @return Number of sessions
     */
	public int getMaxChatSessions() {
		int result = 1;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.MAX_CHAT_SESSIONS));
			} catch(Exception e) {}
		}
		return result;
	}

    /**
     * Get max number of simultaneous file transfer sessions
     *
     * @return Number of sessions
     */
	public int getMaxFileTransferSessions() {
		int result = 1;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.MAX_FILE_TRANSFER_SESSIONS));
			} catch(Exception e) {}
		}
		return result;
	}
	
    /**
     * Get max number of simultaneous IP call sessions
     *
     * @return Number of sessions
     */
	public int getMaxIPCallSessions() {
		int result = 1;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.MAX_IP_CALL_SESSIONS));
			} catch(Exception e) {}
		}
		return result;
	}
	
	/**
     * Is SMS fallback service activated
     *
     * @return Boolean
     */
	public boolean isSmsFallbackServiceActivated() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.SMS_FALLBACK_SERVICE));
		}
		return result;
	}

	/**
     * Is chat invitation auto accepted
     *
     * @return Boolean
     */
	public boolean isChatAutoAccepted(){
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.AUTO_ACCEPT_CHAT));
		}
		return result;
	}

    /**
     * Is group chat invitation auto accepted
     *
     * @return Boolean
     */
    public boolean isGroupChatAutoAccepted(){
        boolean result = false;
        if (instance != null) {
            result = Boolean.parseBoolean(readParameter(RcsSettingsData.AUTO_ACCEPT_GROUP_CHAT));
        }
        return result;
    }

	/**
     * Is file transfer invitation auto accepted
     *
     * @return Boolean
     */
	public boolean isFileTransferAutoAccepted() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.AUTO_ACCEPT_FILE_TRANSFER));
		}
		return result;
	}

	/**
     * Is Store & Forward service warning activated
     *
     * @return Boolean
     */
	public boolean isStoreForwardWarningActivated() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.WARN_SF_SERVICE));
		}
		return result;
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
	public int getImSessionStartMode() {
		int result = 1;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.IM_SESSION_START));
			} catch (Exception e) {
			}
		}
		return result;
	}

	/**
	 * Get max number of entries per contact in the chat log
	 * 
	 * @return Number
	 */
	public int getMaxChatLogEntriesPerContact() {
		int result = 200;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.MAX_CHAT_LOG_ENTRIES));
			} catch(Exception e) {}
		}
		return result;
	}

	/**
	 * Get max number of entries per contact in the richcall log
	 * 
	 * @return Number
	 */
	public int getMaxRichcallLogEntriesPerContact() {
		int result = 200;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.MAX_RICHCALL_LOG_ENTRIES));
			} catch(Exception e) {}
		}
		return result;
	}
	
	/**
	 * Get max number of entries per contact in the IP call log
	 * 
	 * @return Number
	 */
	public int getMaxIPCallLogEntriesPerContact() {
		int result = 200;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.MAX_IPCALL_LOG_ENTRIES));
			} catch(Exception e) {}
		}
		return result;
	}
	
	
    /**
     * Get polling period used before each IMS service check (e.g. test subscription state for presence service)
     *
     * @return Period in seconds
     */
	public int getImsServicePollingPeriod(){
		int result = 300;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.IMS_SERVICE_POLLING_PERIOD));
			} catch(Exception e) {}
		}
		return result;
	}

	/**
     * Get default SIP listening port
     *
     * @return Port
     */
	public int getSipListeningPort() {
		int result = 5060;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.SIP_DEFAULT_PORT));
			} catch(Exception e) {}
		}
		return result;
	}

    /**
     * Get default SIP protocol for mobile
     * 
     * @return Protocol (udp | tcp | tls)
     */
	public String getSipDefaultProtocolForMobile() {
		String result = null;
		if (instance != null) {
            result = readParameter(RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_MOBILE);
		}
		return result;
	}

    /**
     * Get default SIP protocol for wifi
     * 
     * @return Protocol (udp | tcp | tls)
     */
    public String getSipDefaultProtocolForWifi() {
        String result = null;
        if (instance != null) {
            result = readParameter(RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_WIFI);
        }
        return result;
    }

    /**
     * Get TLS Certificate root
     * 
     * @return Path of the certificate
     */
    public String getTlsCertificateRoot() {
        String result = null;
        if (instance != null) {
            result = readParameter(RcsSettingsData.TLS_CERTIFICATE_ROOT);
        }
        return result;
    }

    /**
     * Get TLS Certificate intermediate
     * 
     * @return Path of the certificate
     */
    public String getTlsCertificateIntermediate() {
        String result = null;
        if (instance != null) {
            result = readParameter(RcsSettingsData.TLS_CERTIFICATE_INTERMEDIATE);
        }
        return result;
    }

    /**
     * Get SIP transaction timeout used to wait SIP response
     * 
     * @return Timeout in seconds
     */
	public int getSipTransactionTimeout() {
		int result = 30;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.SIP_TRANSACTION_TIMEOUT));
			} catch(Exception e) {}
		}
		return result;
	}

	/**
     * Get default MSRP port
     *
     * @return Port
     */
	public int getDefaultMsrpPort() {
		int result = 20000;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.MSRP_DEFAULT_PORT));
			} catch(Exception e) {}
		}
		return result;
	}

	/**
     * Get default RTP port
     *
     * @return Port
     */
	public int getDefaultRtpPort() {
		int result = 10000;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.RTP_DEFAULT_PORT));
			} catch(Exception e) {}
		}
		return result;
	}

    /**
     * Get MSRP transaction timeout used to wait MSRP response
     *
     * @return Timeout in seconds
     */
	public int getMsrpTransactionTimeout() {
		int result = 5;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.MSRP_TRANSACTION_TIMEOUT));
			} catch(Exception e) {}
		}
		return result;
	}

	/**
     * Get default expire period for REGISTER
     *
     * @return Period in seconds
     */
	public int getRegisterExpirePeriod() {
		int result = 3600;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.REGISTER_EXPIRE_PERIOD));
			} catch(Exception e) {}
		}
		return result;
	}

	/**
     * Get registration retry base time
     *
     * @return Time in seconds
     */
	public int getRegisterRetryBaseTime() {
		int result = 30;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.REGISTER_RETRY_BASE_TIME));
			} catch(Exception e) {}
		}
		return result;
	}

	/**
     * Get registration retry max time
     *
     * @return Time in seconds
     */
	public int getRegisterRetryMaxTime() {
		int result = 1800;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.REGISTER_RETRY_MAX_TIME));
			} catch(Exception e) {}
		}
		return result;
	}

	/**
     * Get default expire period for PUBLISH
     *
     * @return Period in seconds
     */
	public int getPublishExpirePeriod() {
		int result = 3600;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.PUBLISH_EXPIRE_PERIOD));
			} catch(Exception e) {}
		}
		return result;
	}

	/**
     * Get revoke timeout before to unrevoke a revoked contact
     *
     * @return Timeout in seconds
     */
	public int getRevokeTimeout() {
		int result = 300;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.REVOKE_TIMEOUT));
			} catch(Exception e) {}
		}
		return result;
	}

	/**
     * Get IMS authentication procedure for mobile access
     *
     * @return Authentication procedure
     */
	public String getImsAuhtenticationProcedureForMobile() {
		String result = null;
		if (instance != null) {
			result = readParameter(RcsSettingsData.IMS_AUTHENT_PROCEDURE_MOBILE);
		}
		return result;
	}

	/**
     * Get IMS authentication procedure for Wi-Fi access
     *
     * @return Authentication procedure
     */
	public String getImsAuhtenticationProcedureForWifi() {
		String result = null;
		if (instance != null) {
			result = readParameter(RcsSettingsData.IMS_AUTHENT_PROCEDURE_WIFI);
		}
		return result;
	}

    /**
     * Is Tel-URI format used
     *
     * @return Boolean
     */
	public boolean isTelUriFormatUsed() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.TEL_URI_FORMAT));
		}
		return result;
	}

	/**
     * Get ringing period
     *
     * @return Period in seconds
     */
	public int getRingingPeriod() {
		int result = 120;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.RINGING_SESSION_PERIOD));
			} catch(Exception e) {}
		}
		return result;
	}

	/**
     * Get default expire period for SUBSCRIBE
     *
     * @return Period in seconds
     */
	public int getSubscribeExpirePeriod() {
		int result = 3600;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.SUBSCRIBE_EXPIRE_PERIOD));
			} catch(Exception e) {}
		}
		return result;
	}

	/**
     * Get "Is-composing" timeout for chat service
     *
     * @return Timer in seconds
     */
	public int getIsComposingTimeout() {
		int result = 15;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.IS_COMPOSING_TIMEOUT));
			} catch(Exception e) {}
		}
		return result;
	}

	/**
     * Get default expire period for INVITE (session refresh)
     *
     * @return Period in seconds
     */
	public int getSessionRefreshExpirePeriod() {
		int result = 3600;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.SESSION_REFRESH_EXPIRE_PERIOD));
			} catch(Exception e) {}
		}
		return result;
	}

    /**
     * Is permanente state mode activated
     *
     * @return Boolean
     */
	public boolean isPermanentStateModeActivated() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.PERMANENT_STATE_MODE));
		}
		return result;
	}

    /**
     * Is trace activated
     *
     * @return Boolean
     */
	public boolean isTraceActivated() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.TRACE_ACTIVATED));
		}
		return result;
	}

	/**
     * Get trace level
     *
     * @return trace level
     */
	public int getTraceLevel() {
		int result = Logger.ERROR_LEVEL;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.TRACE_LEVEL));
			} catch(Exception e) {}
		}
		return result;
	}

    /**
     * Is media trace activated
     *
     * @return Boolean
     */
	public boolean isSipTraceActivated() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.SIP_TRACE_ACTIVATED));
		}
		return result;
	}

    /**
     * Get SIP trace file
     *
     * @return SIP trace file
     */
    public String getSipTraceFile() {
        String result = Environment.getExternalStorageDirectory().getPath() + "sip.txt";
        if (instance != null) {
            try {
                result = readParameter(RcsSettingsData.SIP_TRACE_FILE);
            } catch(Exception e) {}
        }
        return result;
    }
	
    /**
     * Is media trace activated
     *
     * @return Boolean
     */
	public boolean isMediaTraceActivated() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.MEDIA_TRACE_ACTIVATED));
		}
		return result;
	}

    /**
     * Get capability refresh timeout used to avoid too many requests in a short time
     *
     * @return Timeout in seconds
     */
	public int getCapabilityRefreshTimeout() {
		int result = 1;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.CAPABILITY_REFRESH_TIMEOUT));
			} catch(Exception e) {}
		}
		return result;
	}

	/**
     * Get capability expiry timeout used to decide when to refresh contact capabilities
     *
     * @return Timeout in seconds
     */
	public int getCapabilityExpiryTimeout() {
		int result = 3600;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.CAPABILITY_EXPIRY_TIMEOUT));
			} catch(Exception e) {}
		}
		return result;
	}

    /**
     * Get capability polling period used to refresh contacts capabilities
     *
     * @return Timeout in seconds
     */
	public int getCapabilityPollingPeriod() {
		int result = 3600;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.CAPABILITY_POLLING_PERIOD));
			} catch(Exception e) {}
		}
		return result;
	}

    /**
     * Is CS video supported
     *
     * @return Boolean
     */
	public boolean isCsVideoSupported() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.CAPABILITY_CS_VIDEO));
		}
		return result;
	}

	/**
     * Is file transfer supported
     *
     * @return Boolean
     */
	public boolean isFileTransferSupported() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.CAPABILITY_FILE_TRANSFER));
		}
		return result;
	}

	/**
     * Is file transfer via HTTP supported
     *
     * @return Boolean
     */
	public boolean isFileTransferHttpSupported() {
		boolean result = false;
		if (instance != null) {
            if ((getFtHttpServer().length() > 0) && (getFtHttpLogin().length() > 0) && (getFtHttpPassword().length() > 0)) {
                result = Boolean.parseBoolean(readParameter(RcsSettingsData.CAPABILITY_FILE_TRANSFER_HTTP));
            }
		}
		return result;
	}

	/**
     * Is IM session supported
     *
     * @return Boolean
     */
	public boolean isImSessionSupported() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.CAPABILITY_IM_SESSION));
		}
		return result;
	}
	
	/**
     * Is IM group session supported
     *
     * @return Boolean
     */
	public boolean isImGroupSessionSupported() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.CAPABILITY_IM_GROUP_SESSION));
		}
		return result;
	}

	/**
     * Is image sharing supported
     *
     * @return Boolean
     */
	public boolean isImageSharingSupported() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.CAPABILITY_IMAGE_SHARING));
		}
		return result;
	}

	/**
     * Is video sharing supported
     *
     * @return Boolean
     */
	public boolean isVideoSharingSupported() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.CAPABILITY_VIDEO_SHARING));
		}
		return result;
	}

	/**
     * Is presence discovery supported
     *
     * @return Boolean
     */
	public boolean isPresenceDiscoverySupported() {
		boolean result = false;
		if (instance != null) {
            if (getXdmServer().length() > 0) {
            	result = Boolean.parseBoolean(readParameter(RcsSettingsData.CAPABILITY_PRESENCE_DISCOVERY));
            }
		}
		return result;
	}

    /**
     * Is social presence supported
     *
     * @return Boolean
     */
	public boolean isSocialPresenceSupported() {
		boolean result = false;
		if (instance != null) {
            if (getXdmServer().length() > 0) {
            	result = Boolean.parseBoolean(readParameter(RcsSettingsData.CAPABILITY_SOCIAL_PRESENCE));
            }
		}
		return result;
	}

    /**
     * Is geolocation push supported
     *
     * @return Boolean
     */
	public boolean isGeoLocationPushSupported() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.CAPABILITY_GEOLOCATION_PUSH));
		}
		return result;
	}
	

    /**
     * Is file transfer thumbnail supported
     *
     * @return Boolean
     */
	public boolean isFileTransferThumbnailSupported() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.CAPABILITY_FILE_TRANSFER_THUMBNAIL));
		}
		return result;
	}
	
    /**
     * Is file transfer Store & Forward supported
     *
     * @return Boolean
     */
	public boolean isFileTransferStoreForwardSupported() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.CAPABILITY_FILE_TRANSFER_SF));
		}
		return result;
	}

	 /**
     * Is IP voice call supported
     *
     * @return Boolean
     */
	public boolean isIPVoiceCallSupported() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.CAPABILITY_IP_VOICE_CALL));
		}
		return result;
	}
	
	/**
     * Is IP video call supported
     *
     * @return Boolean
     */
	public boolean isIPVideoCallSupported() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.CAPABILITY_IP_VIDEO_CALL));
		}
		return result;
	}
	
	
    /**
     * Is group chat Store & Forward supported
     *
     * @return Boolean
     */
	public boolean isGroupChatStoreForwardSupported() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.CAPABILITY_GROUP_CHAT_SF));
		}
		return result;
	}

	/**
     * Get supported RCS extensions
     *
     * @return List of extensions (semicolon separated)
     */
	public String getSupportedRcsExtensions() {
		String result = null;
		if (instance != null) {
			return readParameter(RcsSettingsData.CAPABILITY_RCS_EXTENSIONS);
		}
		return result;
    }

	/**
     * Set supported RCS extensions
     *
     * @param extensions List of extensions (semicolon separated)
     */
	public void setSupportedRcsExtensions(String extensions) {
		if (instance != null) {
			writeParameter(RcsSettingsData.CAPABILITY_RCS_EXTENSIONS, extensions);
		}
    }

	/**
     * Is IM always-on thanks to the Store & Forward functionality
     *
     * @return Boolean
     */
	public boolean isImAlwaysOn() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.IM_CAPABILITY_ALWAYS_ON));
		}
		return result;
	}
	
	/**
     * Is File Transfer always-on thanks to the Store & Forward functionality
     *
     * @return Boolean
     */
	public boolean isFtAlwaysOn() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.FT_CAPABILITY_ALWAYS_ON));
		}
		return result;
	}

	/**
     * Is IM reports activated
     *
     * @return Boolean
     */
	public boolean isImReportsActivated() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.IM_USE_REPORTS));
		}
		return result;
	}

	/**
     * Get network access
     *
     * @return Network type
     */
	public int getNetworkAccess() {
		int result = RcsSettingsData.ANY_ACCESS;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.NETWORK_ACCESS));
			} catch(Exception e) {}
		}
		return result;
	}

    /**
     * Get SIP timer T1
     *
     * @return Timer in milliseconds
     */
	public int getSipTimerT1() {
		int result = 2000;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.SIP_TIMER_T1));
			} catch(Exception e) {}
		}
		return result;
	}

    /**
     * Get SIP timer T2
     *
     * @return Timer in milliseconds
     */
	public int getSipTimerT2() {
		int result = 16000;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.SIP_TIMER_T2));
			} catch(Exception e) {}
		}
		return result;
	}

    /**
     * Get SIP timer T4
     *
     * @return Timer in milliseconds
     */
	public int getSipTimerT4() {
		int result = 17000;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.SIP_TIMER_T4));
			} catch(Exception e) {}
		}
		return result;
	}

	/**
     * Is SIP keep-alive enabled
     *
     * @return Boolean
     */
	public boolean isSipKeepAliveEnabled() {
		boolean result = true;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.SIP_KEEP_ALIVE));
		}
		return result;
	}

    /**
     * Get SIP keep-alive period
     *
     * @return Period in seconds
     */
	public int getSipKeepAlivePeriod() {
		int result = 60;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.SIP_KEEP_ALIVE_PERIOD));
			} catch(Exception e) {}
		}
		return result;
    }

	/**
     * Get APN used to connect to RCS platform
     *
     * @return APN (null means any APN may be used to connect to RCS)
     */
	public String getNetworkApn() {
		String result = null;
		if (instance != null) {
			result = readParameter(RcsSettingsData.RCS_APN);
		}
		return result;
    }

	/**
     * Get operator authorized to connect to RCS platform
     *
     * @return SIM operator name (null means any SIM operator is authorized to connect to RCS)
     */
	public String getNetworkOperator() {
		String result = null;
		if (instance != null) {
			result = readParameter(RcsSettingsData.RCS_OPERATOR);
		}
		return result;
    }

	/**
     * Is GRUU supported
     *
     * @return Boolean
     */
	public boolean isGruuSupported() {
		boolean result = true;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.GRUU));
		}
		return result;
	}

    /**
     * Is IMEI used as device ID
     *
     * @return Boolean
     */
    public boolean isImeiUsedAsDeviceId() {
        boolean result = true;
        if (instance != null) {
            result = Boolean.parseBoolean(readParameter(RcsSettingsData.USE_IMEI_AS_DEVICE_ID));
        }
        return result;
    }

    /**
     * Is CPU Always_on activated
     *
     * @return Boolean
     */
    public boolean isCpuAlwaysOn() {
        boolean result = false;
        if (instance != null) {
            result = Boolean.parseBoolean(readParameter(RcsSettingsData.CPU_ALWAYS_ON));
        }
        return result;
    }

	/**
     * Get auto configuration mode
     *
     * @return Mode
     */
	public int getAutoConfigMode() {
		int result = RcsSettingsData.NO_AUTO_CONFIG;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.AUTO_CONFIG_MODE));
			} catch(Exception e) {}
		}
		return result;
	}

    /**
     * Is Terms and conditions via provisioning is accepted
     * 
     * @return Boolean
     */
    public boolean isProvisioningTermsAccepted() {
        boolean result = false;
        if (instance != null) {
            result = Boolean.parseBoolean(readParameter(RcsSettingsData.PROVISIONING_TERMS_ACCEPTED));
        }
        return result;
    }

    /**
     * Get provisioning version
     * 
     * @return Version
     */
    public String getProvisioningVersion() {
        String result = "0";
        if (instance != null) {
            result = readParameter(RcsSettingsData.PROVISIONING_VERSION);
        }
        return result;
    }

    /**
     * Set provisioning version
     * 
     * @param version Version
     */
    public void setProvisioningVersion(String version) {
        if (instance != null) {
            writeParameter(RcsSettingsData.PROVISIONING_VERSION, version);
        }
    }

    /**
     * Set Terms and conditions via provisioning accepted
     * 
     * @param state State
     */
    public void setProvisioningTermsAccepted(boolean state) {
        if (instance != null) {
            writeParameter(RcsSettingsData.PROVISIONING_TERMS_ACCEPTED,
                    Boolean.toString(state));
        }
    }

    /**
     * Get secondary provisioning address
     *
     * @return Address
     */
    public String getSecondaryProvisioningAddress() {
        String result = "";
        if (instance != null) {
            result = readParameter(RcsSettingsData.SECONDARY_PROVISIONING_ADDRESS);
        }
        return result;
    }

    /**
     * Set secondary provisioning address
     *
     * @param Address
     */
    public void setSecondaryProvisioningAddress(String value) {
        if (instance != null) {
            writeParameter(RcsSettingsData.SECONDARY_PROVISIONING_ADDRESS, value);
        }
    }

    /**
     * Is secondary provisioning address only used
     *
     * @return Boolean
     */
    public boolean isSecondaryProvisioningAddressOnly() {
        boolean result = false;
        if (instance != null) {
            result = Boolean.parseBoolean(readParameter(RcsSettingsData.SECONDARY_PROVISIONING_ADDRESS_ONLY));
        }
        return result;
    }

    /**
     * Set secondary provisioning address only used
     *
     * @param Boolean
     */
    public void setSecondaryProvisioningAddressOnly(boolean value) {
        if (instance != null) {
            writeParameter(RcsSettingsData.SECONDARY_PROVISIONING_ADDRESS_ONLY, Boolean.toString(value));
        }
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
         String mode = RcsSettings.getInstance().getImsAuhtenticationProcedureForMobile();
		 if (mode.equals(RcsSettingsData.DIGEST_AUTHENT)) {
	         if (TextUtils.isEmpty(getUserProfileImsUserName())) {
	        	 return false;
	         }
	         if (TextUtils.isEmpty(getUserProfileImsPassword())) {
	        	 return false;
	         }
	         if (TextUtils.isEmpty(this.getUserProfileImsPrivateId())) {
	        	 return false;
	         }			
		}
    	
        return true;
    }
    
	/**
     * Is group chat activated
     *
     * @return Boolean
     */
	public boolean isGroupChatActivated() {
		boolean result = false;
		if (instance != null) {
			String value = getImConferenceUri();
			if ((value != null) &&
					(value.length() > 0) &&
						!value.equals(RcsSettingsData.DEFAULT_GROUP_CHAT_URI)) {
				result = true;
			}
		}
		return result;
	}
    
	/**
	 * Get the root directory for photos
	 * 
	 *  @return Directory path
	 */
	public String getPhotoRootDirectory() {
        String result = Environment.getExternalStorageDirectory().toString();
        if (instance != null) {
            result = readParameter(RcsSettingsData.DIRECTORY_PATH_PHOTOS);
        }
        return result;
	}

	/**
	 * Set the root directory for photos
	 * 
	 *  @param path Directory path
	 */
	public void setPhotoRootDirectory(String path) {
        if (instance != null) {
            writeParameter(RcsSettingsData.DIRECTORY_PATH_PHOTOS, path);
        }
	}

	/**
	 * Get the root directory for videos
	 * 
	 *  @return Directory path
	 */
	public String getVideoRootDirectory() {
        String result = Environment.getExternalStorageDirectory().toString();
        if (instance != null) {
            result = readParameter(RcsSettingsData.DIRECTORY_PATH_VIDEOS);
        }
        return result;
	}
	
	/**
	 * Set the root directory for videos
	 * 
	 *  @param path Directory path
	 */
	public void setVideoRootDirectory(String path) {
        if (instance != null) {
            writeParameter(RcsSettingsData.DIRECTORY_PATH_VIDEOS, path);
        }
	}
	
	/**
	 * Get the root directory for files
	 * 
	 *  @return Directory path
	 */
	public String getFileRootDirectory() {	
        String result = Environment.getExternalStorageDirectory().toString();
        if (instance != null) {
            result = readParameter(RcsSettingsData.DIRECTORY_PATH_FILES);
        }
        return result;
	}
    
	/**
	 * Set the root directory for files
	 * 
	 *  @param path Directory path
	 */
	public void setFileRootDirectory(String path) {
        if (instance != null) {
            writeParameter(RcsSettingsData.DIRECTORY_PATH_FILES, path);
        }
	}
	
	/**
	 * Is secure MSRP media over Wi-Fi
	 * 
	 * @return Boolean
	 */
	public boolean isSecureMsrpOverWifi() {
        boolean result = false;
        if (instance != null) {
            result = Boolean.parseBoolean(readParameter(RcsSettingsData.SECURE_MSRP_OVER_WIFI));
        }
        return result;
	}

	/**
	 * Is secure RTP media over Wi-Fi
	 * 
	 * @return Boolean
	 */
	public boolean isSecureRtpOverWifi() {
        boolean result = false;
        if (instance != null) {
            result = Boolean.parseBoolean(readParameter(RcsSettingsData.SECURE_RTP_OVER_WIFI));
        }
        return result;
	}
	
    /**
     * Get max geolocation label length
     *
     * @return Number of char
     */
	public int getMaxGeolocLabelLength() {
		int result = 100;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.MAX_GEOLOC_LABEL_LENGTH));
			} catch(Exception e) {}
		}
		return result;
	}
	
    /**
     * Get geolocation expiration time
     *
     * @return Time in seconds
     */
	public int getGeolocExpirationTime() {
		int result = 1800;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.GEOLOC_EXPIRATION_TIME));
			} catch(Exception e) {}
		}
		return result;
	}

	public void setProvisioningToken(String token) {
		if (instance != null) {
            writeParameter(RcsSettingsData.PROVISIONING_TOKEN, token);
        }
	}

	public String getProvisioningToken() {
		String result = "0";
        if (instance != null) {
            result = readParameter(RcsSettingsData.PROVISIONING_TOKEN);
        }
        return result;
	}
	
    /**
     * Is SIP device an automata ?
     *
     * @return Boolean
     */
	public boolean isSipAutomata() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.CAPABILITY_SIP_AUTOMATA));
		}
		return result;
	}
	
	/**
     * Get max file-icon size
     *
     * @return Size in kilobytes
     */
	public int getMaxFileIconSize() {
		int result = 50;
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.MAX_FILE_ICON_SIZE));
			} catch(Exception e) {}
		}
		return result;
	}
	
	/**
	 * Get the GSMA release
	 * 
	 * @return the GSMA release
	 */
	public int getGsmaRelease() {
		int result = 1; // Blackbird
		if (instance != null) {
			try {
				result = Integer.parseInt(readParameter(RcsSettingsData.KEY_GSMA_RELEASE));
			} catch (Exception e) {
			}
		}
		return result;
	}
	
	/**
	 * Set the GSMA release
	 * 
	 * @param release Release
	 */
	public void setGsmaRelease(int release) {
		if (instance != null) {
			writeParameter(RcsSettingsData.KEY_GSMA_RELEASE, ""+release);
		}
	}
	
	/**
	 * Is Albatros GSMA release
	 * 
	 * @return Boolean
	 */
	public boolean isAlbatrosRelease() {
		return (RcsSettings.getInstance().getGsmaRelease() == RcsSettingsData.VALUE_GSMA_REL_ALBATROS);
	}	

	/**
	 * Is Blackbird GSMA release
	 * 
	 * @return Boolean
	 */
	public boolean isBlackbirdRelease() {
		return (RcsSettings.getInstance().getGsmaRelease() == RcsSettingsData.VALUE_GSMA_REL_BLACKBIRD);
	}		
	
	/**
     * Is IP voice call breakout supported in RCS-AA mode
     *
     * @return Boolean
     */
	public boolean isIPVoiceCallBreakoutAA() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.IPVOICECALL_BREAKOUT_AA));
		}
		return result;
	}
	
	/**
     * Is IP voice call breakout supported in RCS-CS mode
     *
     * @return Boolean
     */
	public boolean isIPVoiceCallBreakoutCS() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.IPVOICECALL_BREAKOUT_CS));
		}
		return result;
	}
	
	/**
     * Is IP Video Call upgrade without first tearing down the CS voice call authorized
     *
     * @return Boolean
     */
	public boolean isIPVideoCallUpgradeFromCS() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.IPVIDEOCALL_UPGRADE_FROM_CS));
		}
		return result;
	}
	
	
	/**
     * Is IP Video Call upgrade on capability error
     *
     * @return Boolean
     */
	public boolean isIPVideoCallUpgradeOnCapError() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.IPVIDEOCALL_UPGRADE_ON_CAPERROR));
		}
		return result;
	}
	
	/**
     * Is device in RCS-CS mode authorized to upgrade to video without first tearing down CS call?
     *
     * @return Boolean
     */
	public boolean isIPVideoCallAttemptEarly() {
		boolean result = false;
		if (instance != null) {
			result = Boolean.parseBoolean(readParameter(RcsSettingsData.IPVIDEOCALL_UPGRADE_ATTEMPT_EARLY));
		}
		return result;
	}
	
    /**
     * Is TCP fallback enabled according to RFC3261 chapter 18.1.1
     * 
     * @return Boolean
     */
    public boolean isTcpFallback() {
        boolean result = false;
        if (instance != null) {
            result = Boolean.parseBoolean(readParameter(RcsSettingsData.TCP_FALLBACK));
        }
        return result;
    }

    /**
     * Get vendor name of the client
     *
     * @return Vendor
     */
    public String getVendor() {
        String result = "OrangeLabs";
        if (instance != null) {
            result = readParameter(RcsSettingsData.VENDOR_NAME);
        }
        return result;
    }
}
