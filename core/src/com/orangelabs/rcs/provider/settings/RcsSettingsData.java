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

import javax2.sip.ListeningPoint;

import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Environment;
import android.util.SparseArray;

import com.gsma.services.rcs.RcsServiceConfiguration;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * RCS settings data constants
 *
 * @author jexa7410
 * @author yplo6403
 *
 */
public class RcsSettingsData {

	public static final Uri CONTENT_URI = Uri
			.parse("content://com.orangelabs.rcs.setting/setting");

	/**
	 * Key of the Rcs configuration parameter
	 */
	static final String KEY_KEY = RcsServiceConfiguration.Settings.KEY;

	/**
	 * Value of the Rcs configuration parameter
	 */
	static final String KEY_VALUE = RcsServiceConfiguration.Settings.VALUE;

	/**
	 * Default group chat conference URI
	 */
	public static final String DEFAULT_GROUP_CHAT_URI = "sip:foo@bar";

    /**
     * File type for certificate
     */
    public static final String CERTIFICATE_FILE_TYPE = ".crt";
    
	// ---------------------------------------------------------------------------
	// Enumerated
	// ---------------------------------------------------------------------------
	   
    /**
     * The authentication procedure enumerated type.
     */
	public enum AuthenticationProcedure {
		GIBA, DIGEST
	};
    
	// TODO replace by API definition CR031
	public enum MessagingMode {
		INTEGRATED(0), CONVERGED(1), SEAMLESS(2), NONE(3);

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

		public final int toInt() {
			return mValue;
		}

		public static MessagingMode valueOf(int value) {
			MessagingMode entry = mValueToEnum.get(value);
			if (entry != null) {
				return entry;
			}
			throw new IllegalArgumentException("No enum const class "
					+ MessagingMode.class.getName() + "." + value);

		}

	};
	
	// TODO replace by API definition CR031
	public enum DefaultMessagingMethod {
		AUTOMATIC(0), RCS(1), NON_RCS(2);
		
		private int mValue;

		private static SparseArray<DefaultMessagingMethod> mValueToEnum = new SparseArray<DefaultMessagingMethod>();
		static {
			for (DefaultMessagingMethod entry : DefaultMessagingMethod.values()) {
				mValueToEnum.put(entry.toInt(), entry);
			}
		}

		private DefaultMessagingMethod(int value) {
			mValue = value;
		}

		public final int toInt() {
			return mValue;
		}

		public static DefaultMessagingMethod valueOf(int value) {
			DefaultMessagingMethod entry = mValueToEnum.get(value);
			if (entry != null) {
				return entry;
			}
			throw new IllegalArgumentException("No enum const class "
					+ DefaultMessagingMethod.class.getName() + "." + value);

		}
		
	};
    
	// TODO replace by API definition CR031
	/**
	 * Enumerated for the Image Resize Option
	 */
	public enum ImageResizeOption {
		ALWAYS_PERFORM(0), ONLY_ABOVE_MAX_SIZE(1), ASK(2);
		
		private int mValue;

		private static SparseArray<ImageResizeOption> mValueToEnum = new SparseArray<ImageResizeOption>();
		static {
			for (ImageResizeOption entry : ImageResizeOption.values()) {
				mValueToEnum.put(entry.toInt(), entry);
			}
		}

		private ImageResizeOption(int value) {
			mValue = value;
		}

		public final int toInt() {
			return mValue;
		}

		public static ImageResizeOption valueOf(int value) {
			ImageResizeOption entry = mValueToEnum.get(value);
			if (entry != null) {
				return entry;
			}
			throw new IllegalArgumentException("No enum const class "
					+ ImageResizeOption.class.getName() + "." + value);

		}
		
	};
    
    public enum NetworkAccessType {
    	MOBILE(ConnectivityManager.TYPE_MOBILE), WIFI(ConnectivityManager.TYPE_WIFI), ANY(-1);
		
    	private int mValue;

		private static SparseArray<NetworkAccessType> mValueToEnum = new SparseArray<NetworkAccessType>();
		static {
			for (NetworkAccessType entry : NetworkAccessType.values()) {
				mValueToEnum.put(entry.toInt(), entry);
			}
		}

		private NetworkAccessType(int value) {
			mValue = value;
		}

		public final int toInt() {
			return mValue;
		}

		public static NetworkAccessType valueOf(int value) {
			NetworkAccessType entry = mValueToEnum.get(value);
			if (entry != null) {
				return entry;
			}
			throw new IllegalArgumentException("No enum const class "
					+ NetworkAccessType.class.getName() + "." + value);

		}
		
	};

	/**
	 * The configuration mode enumerated type.
	 */
	public enum ConfigurationMode {
		MANUAL(0), AUTO(1);
		
		private int mValue;

		private static SparseArray<ConfigurationMode> mValueToEnum = new SparseArray<ConfigurationMode>();
		static {
			for (ConfigurationMode entry : ConfigurationMode.values()) {
				mValueToEnum.put(entry.toInt(), entry);
			}
		}

		private ConfigurationMode(int value) {
			mValue = value;
		}

		public final int toInt() {
			return mValue;
		}

		public static ConfigurationMode valueOf(int value) {
			ConfigurationMode entry = mValueToEnum.get(value);
			if (entry != null) {
				return entry;
			}
			throw new IllegalArgumentException("No enum const class "
					+ ConfigurationMode.class.getName() + "." + value);

		}
	};
    
    /**
     * The File Transfer protocol enumerated type.
     */
	public enum FileTransferProtocol {
		MSRP, HTTP
	};
    
    /**
     * The GSMA release enumerated type.
     */
	public enum GsmaRelease {
		ALBATROS(0), BLACKBIRD(1), CRANE(2);
		
		private int mValue;

		private static SparseArray<GsmaRelease> mValueToEnum = new SparseArray<GsmaRelease>();
		static {
			for (GsmaRelease entry : GsmaRelease.values()) {
				mValueToEnum.put(entry.toInt(), entry);
			}
		}

		private GsmaRelease(int value) {
			mValue = value;
		}

		public final int toInt() {
			return mValue;
		}

		public static GsmaRelease valueOf(int value) {
			GsmaRelease entry = mValueToEnum.get(value);
			if (entry != null) {
				return entry;
			}
			throw new IllegalArgumentException("No enum const class "
					+ GsmaRelease.class.getName() + "." + value);

		}

	}

    // ---------------------------------------------------------------------------
	// UI settings
	// ---------------------------------------------------------------------------

	/**
	 * Activate or not the RCS service
	 */
	public static final String SERVICE_ACTIVATED = "ServiceActivated";
	/* package private */static final Boolean DEFAULT_SERVICE_ACTIVATED = false;

	/**
	 * Ringtone which is played when a social presence sharing invitation is received
	 */
	public static final String PRESENCE_INVITATION_RINGTONE = "PresenceInvitationRingtone";
	/* package private */static final String DEFAULT_PRESENCE_INVITATION_RINGTONE = "";

	/**
	 * Vibrate or not when a social presence sharing invitation is received
	 */
	public static final String PRESENCE_INVITATION_VIBRATE = "PresenceInvitationVibrate";
	/* package private */static final Boolean DEFAULT_PRESENCE_INVITATION_VIBRATE = true;

	/**
	 * Ringtone which is played when a content sharing invitation is received
	 */
	public static final String CSH_INVITATION_RINGTONE = "CShInvitationRingtone";
	/* package private */static final String DEFAULT_CSH_INVITATION_RINGTONE = "";

	/**
	 * Vibrate or not when a content sharing invitation is received
	 */
	public static final String CSH_INVITATION_VIBRATE = "CShInvitationVibrate";
	/* package private */static final Boolean DEFAULT_CSH_INVITATION_VIBRATE = true;
    
	/**
	 * Make a beep or not when content sharing is available during a call
	 */
	public static final String CSH_AVAILABLE_BEEP = "CShAvailableBeep";
	/* package private */static final Boolean DEFAULT_CSH_AVAILABLE_BEEP = true;

	/**
	 * Ringtone which is played when a file transfer invitation is received
	 */
	public static final String FILETRANSFER_INVITATION_RINGTONE = "FileTransferInvitationRingtone";
	/* package private */static final String DEFAULT_FT_INVITATION_RINGTONE = "";

	/**
	 * Vibrate or not when a file transfer invitation is received
	 */
	public static final String FILETRANSFER_INVITATION_VIBRATE = "FileTransferInvitationVibrate";
	/* package private */static final Boolean DEFAULT_FT_INVITATION_VIBRATE = true;

	/**
	 * Ringtone which is played when a chat invitation is received
	 */
	public static final String CHAT_INVITATION_RINGTONE = "ChatInvitationRingtone";
	/* package private */static final String DEFAULT_CHAT_INVITATION_RINGTONE = "";

    /**
     * Vibrate or not when a chat invitation is received
     */
	public static final String CHAT_INVITATION_VIBRATE = "ChatInvitationVibrate";
	/* package private */static final Boolean DEFAULT_CHAT_INVITATION_VIBRATE = true;

	/**
	 * Send or not the displayed notification
	 */
	public static final String CHAT_RESPOND_TO_DISPLAY_REPORTS = "ChatRespondToDisplayReports";
	/* package private */static final Boolean DEFAULT_CHAT_RESPOND_TO_DISPLAY_REPORTS = true;

	/**
	 * Predefined freetext
	 */
	/* package private */static final String FREETEXT1 = "Freetext1";

	/**
	 * Predefined freetext
	 */
	/* package private */static final String FREETEXT2 = "Freetext2";

	/**
	 * Predefined freetext
	 */
	/* package private */static final String FREETEXT3 = "Freetext3";

	/**
	 * Predefined freetext
	 */
	/* package private */static final String FREETEXT4 = "Freetext4";

	/**
	 * Battery level minimum
	 */
	/* package private */static final String MIN_BATTERY_LEVEL = "MinBatteryLevel";
	/* package private */static final Integer DEFAULT_MIN_BATTERY_LEVEL = 0;

	// ---------------------------------------------------------------------------
	// Service settings
	// ---------------------------------------------------------------------------

	/**
	 * Max file-icon size
	 */
	public static final String MAX_FILE_ICON_SIZE = "FileIconSize";
	/* package private */static final Integer DEFAULT_MAX_FILE_ICON_SIZE = 50;

	/**
	 * Max photo-icon size
	 */
	public static final String MAX_PHOTO_ICON_SIZE = "MaxPhotoIconSize";
	/* package private */static final Integer DEFAULT_MAX_PHOTO_ICON_SIZE = 100;

	/**
	 * Max length of the freetext
	 */
	public static final String MAX_FREETXT_LENGTH = "MaxFreetextLength";
	/* package private */static final Integer DEFAULT_MAX_FREETXT_LENGTH = 100;

	/**
	 * Max number of participants in a group chat
	 */
	public static final String MAX_CHAT_PARTICIPANTS = "MaxChatParticipants";
	/* package private */static final Integer DEFAULT_MAX_CHAT_PARTICIPANTS = 10;

	/**
	 * Max length of a chat message
	 */
	public static final String MAX_CHAT_MSG_LENGTH = "MaxChatMessageLength";
	/* package private */static final Integer DEFAULT_MAX_CHAT_MSG_LENGTH = 100;

	/**
	 * Max length of a group chat message
	 */
	public static final String MAX_GROUPCHAT_MSG_LENGTH = "MaxGroupChatMessageLength";
	/* package private */static final Integer DEFAULT_MAX_GC_MSG_LENGTH = 100;

	/**
	 * Idle duration of a chat session
	 */
	public static final String CHAT_IDLE_DURATION = "ChatIdleDuration";
	/* package private */static final Integer DEFAULT_CHAT_IDLE_DURATION = 300;

	/**
	 * Max size of a file transfer
	 */
	public static final String MAX_FILE_TRANSFER_SIZE = "MaxFileTransferSize";
	/* package private */static final Integer DEFAULT_MAX_FT_SIZE = 3072;

	/**
	 * Warning threshold for file transfer size
	 */
	public static final String WARN_FILE_TRANSFER_SIZE = "WarnFileTransferSize";
	/* package private */static final Integer DEFAULT_WARN_FT_SIZE = 2048;

	/**
	 * Max size of an image share
	 */
	public static final String MAX_IMAGE_SHARE_SIZE = "MaxImageShareSize";
	/* package private */static final Integer DEFAULT_MAX_ISH_SIZE = 3072;

	/**
	 * Max duration of a video share
	 */
	public static final String MAX_VIDEO_SHARE_DURATION = "MaxVideoShareDuration";
	/* package private */static final Integer DEFAULT_MAX_VSH_DURATION = 54000;

	/**
	 * Max number of simultaneous chat sessions
	 */
	public static final String MAX_CHAT_SESSIONS = "MaxChatSessions";
	/* package private */static final Integer DEFAULT_MAX_CHAT_SESSIONS = 20;

	/**
	 * Max number of simultaneous file transfer sessions
	 */
	public static final String MAX_FILE_TRANSFER_SESSIONS = "MaxFileTransferSessions";
	/* package private */static final Integer DEFAULT_MAX_FT_SESSIONS = 10;

	/**
	 * Max number of simultaneous IP call sessions
	 */
	public static final String MAX_IP_CALL_SESSIONS = "MaxIpCallSessions";
	/* package private */static final Integer DEFAULT_MAX_IP_CALL_SESSIONS = 5;

	/**
	 * Activate or not SMS fallback service
	 */
	public static final String SMS_FALLBACK_SERVICE = "SmsFallbackService";
	/* package private */static final Boolean DEFAULT_SMS_FALLBACK_SERVICE = true;

	/**
	 * Auto accept file transfer invitation
	 */
	public static final String AUTO_ACCEPT_FILE_TRANSFER = "AutoAcceptFileTransfer";
	/* package private */static final Boolean DEFAULT_AUTO_ACCEPT_FT = false;
	
	/**
	 * Auto accept chat invitation
	 */
	public static final String AUTO_ACCEPT_CHAT = "AutoAcceptChat";
	/* package private */static final Boolean DEFAULT_AUTO_ACCEPT_CHAT = false;

	/**
	 * Auto accept group chat invitation
	 */
	public static final String AUTO_ACCEPT_GROUP_CHAT = "AutoAcceptGroupChat";
	/* package private */static final Boolean DEFAULT_AUTO_ACCEPT_GC = false;

	/**
	 * Display a warning if Store & Forward service is activated
	 */
	public static final String WARN_SF_SERVICE = "StoreForwardServiceWarning";
	/* package private */static final Boolean DEFAULT_WARN_SF_SERVICE = false;
	
	/**
	 * Define when the chat receiver sends the 200 OK back to the sender
	 */
	public static final String IM_SESSION_START = "ImSessionStart";
	/* package private */static final Integer DEFAULT_IM_SESSION_START = 1;

	/**
	 * Max entries for chat log
	 */
	public static final String MAX_CHAT_LOG_ENTRIES = "MaxChatLogEntries";
	/* package private */static final Integer DEFAULT_MAX_CHAT_LOG_ENTRIES = 500;

	/**
	 * Max entries for richcall log
	 */
	public static final String MAX_RICHCALL_LOG_ENTRIES = "MaxRichcallLogEntries";
	/* package private */static final Integer DEFAULT_MAX_RICHCALL_LOG_ENTRIES = 200;
	
	/**
	 * Max entries for IP call log
	 */
	public static final String MAX_IPCALL_LOG_ENTRIES = "MaxIpcallLogEntries";
	/* package private */static final Integer DEFAULT_MAX_IPCALL_LOG_ENTRIES = 200;

	/**
	 * Max length of a geolocation label
	 */
	public static final String MAX_GEOLOC_LABEL_LENGTH = "MaxGeolocLabelLength";
	/* package private */static final Integer DEFAULT_MAX_GEOLOC_LABEL_LENGTH = 100;

	/**
	 * Geolocation expiration time
	 */
	public static final String GEOLOC_EXPIRATION_TIME = "GeolocExpirationTime";
	/* package private */static final Integer DEFAULT_GEOLOC_EXPIRATION_TIME = 3600;

	/**
	 * Minimum storage capacity
	 */
	public static final String MIN_STORAGE_CAPACITY = "MinStorageCapacity";
	/* package private */static final Integer DEFAULT_MIN_STORAGE_CAPACITY = 10240;
    
	/**
	 * Convergent messaging UX option
	 */
	public static final String KEY_MESSAGING_MODE = RcsServiceConfiguration.Settings.MESSAGING_MODE;
	/* package private */static final Integer DEFAULT_KEY_MESSAGING_MODE = MessagingMode.NONE.toInt();
	
	/**
	 * Default messaging method
	 */
	public static final String KEY_DEFAULT_MESSAGING_METHOD = RcsServiceConfiguration.Settings.DEFAULT_MESSAGING_METHOD;
	/* package private */static final Integer DEFAULT_KEY_DEFAULT_MESSAGING_METHOD = DefaultMessagingMethod.AUTOMATIC.toInt();

	
    // ---------------------------------------------------------------------------
	// User profile settings
	// ---------------------------------------------------------------------------

	/**
	 * IMS username or username part of the IMPU (for HTTP Digest only)
	 */
	public static final String USERPROFILE_IMS_USERNAME = RcsServiceConfiguration.Settings.MY_CONTACT_ID;
	/* package private */static final String DEFAULT_USERPROFILE_IMS_USERNAME = "";

	/**
	 * IMS display name
	 */
	public static final String USERPROFILE_IMS_DISPLAY_NAME = RcsServiceConfiguration.Settings.MY_DISPLAY_NAME;
	/* package private */static final String DEFAULT_USERPROFILE_IMS_DISPLAY_NAME = "";

	/**
	 * IMS home domain
	 */
	public static final String USERPROFILE_IMS_HOME_DOMAIN = "ImsHomeDomain";
	/* package private */static final String DEFAULT_USERPROFILE_IMS_HOME_DOMAIN = "";

	/**
	 * IMS private URI or IMPI (for HTTP Digest only)
	 */
	public static final String USERPROFILE_IMS_PRIVATE_ID = "ImsPrivateId";
	/* package private */static final String DEFAULT_USERPROFILE_IMS_PRIVATE_ID = "";

	/**
	 * IMS password (for HTTP Digest only)
	 */
	public static final String USERPROFILE_IMS_PASSWORD = "ImsPassword";
	/* package private */static final String DEFAULT_USERPROFILE_IMS_PASSWORD = "";

	/**
	 * IMS realm (for HTTP Digest only)
	 */
	public static final String USERPROFILE_IMS_REALM = "ImsRealm";
	/* package private */static final String DEFAULT_USERPROFILE_IMS_REALM = "";

	/**
	 * P-CSCF or outbound proxy address for mobile access
	 */
	public static final String IMS_PROXY_ADDR_MOBILE = "ImsOutboundProxyAddrForMobile";
	/* package private */static final String DEFAULT_IMS_PROXY_ADDR_MOBILE = "";

	/**
	 * P-CSCF or outbound proxy port for mobile access
	 */
	public static final String IMS_PROXY_PORT_MOBILE = "ImsOutboundProxyPortForMobile";
	/* package private */static final Integer DEFAULT_IMS_PROXY_PORT_MOBILE = 5060;

	/**
	 * P-CSCF or outbound proxy address for Wi-Fi access
	 */
	public static final String IMS_PROXY_ADDR_WIFI = "ImsOutboundProxyAddrForWifi";
	/* package private */static final String DEFAULT_IMS_PROXY_ADDR_WIFI = "";

	/**
	 * P-CSCF or outbound proxy port for Wi-Fi access
	 */
	public static final String IMS_PROXY_PORT_WIFI = "ImsOutboundProxyPortForWifi";
	/* package private */static final Integer DEFAULT_IMS_PROXY_PORT_WIFI = 5060;

	/**
	 * XDM server address & port
	 */
	public static final String XDM_SERVER = "XdmServerAddr";
	/* package private */static final String DEFAULT_XDM_SERVER = "";

	/**
	 * XDM server login (for HTTP Digest only)
	 */
	public static final String XDM_LOGIN = "XdmServerLogin";
	/* package private */static final String DEFAULT_XDM_LOGIN = "";

	/**
	 * XDM server password (for HTTP Digest only)
	 */
	public static final String XDM_PASSWORD = "XdmServerPassword";
	/* package private */static final String DEFAULT_XDM_PASSWORD = "";
	
	/**
	 * File transfer HTTP server address & port
	 */
	public static final String FT_HTTP_SERVER = "FtHttpServerAddr";
	/* package private */static final String DEFAULT_FT_HTTP_SERVER = "";

	/**
	 * File transfer HTTP server login
	 */
	public static final String FT_HTTP_LOGIN = "FtHttpServerLogin";
	/* package private */static final String DEFAULT_FT_HTTP_LOGIN = "";

	/**
	 * File transfer HTTP server password
	 */
	public static final String FT_HTTP_PASSWORD = "FtHttpServerPassword";
	/* package private */static final String DEFAULT_FT_HTTP_PASSWORD = "";

	/**
	 * File transfer default protocol
	 */
	public static final String FT_PROTOCOL = "FtProtocol";
	/* package private */static final String DEFAULT_FT_PROTOCOL = FileTransferProtocol.MSRP.name();

	/**
	 * IM conference URI for group chat session
	 */
	public static final String IM_CONF_URI = "ImConferenceUri";
	/* package private */static final String DEFAULT_IM_CONF_URI = RcsSettingsData.DEFAULT_GROUP_CHAT_URI;

	/**
	 * End user confirmation request URI for terms and conditions
	 */
	public static final String ENDUSER_CONFIRMATION_URI = "EndUserConfReqUri";
	/* package private */static final String DEFAULT_ENDUSER_CONFIRMATION_URI = "";

	/**
	 * Country code
	 */
	public static final String COUNTRY_CODE = RcsServiceConfiguration.Settings.MY_COUNTRY_CODE;
	/* package private */static final String DEFAULT_COUNTRY_CODE = "+33";

	/**
	 * Country area code
	 */
	public static final String COUNTRY_AREA_CODE = RcsServiceConfiguration.Settings.MY_COUNTRY_AREA_CODE;
	/* package private */static final String DEFAULT_COUNTRY_AREA_CODE = "0";

	/**
	 * Msisdn
	 */
	public static final String MSISDN = "MSISDN";
	/* package private */static final String DEFAULT_MSISDN = "";

	// ---------------------------------------------------------------------------
	// Stack settings
	// ---------------------------------------------------------------------------

	/**
	 * Polling period used before each IMS service check (e.g. test subscription state for presence service)
	 */
	public static final String IMS_SERVICE_POLLING_PERIOD = "ImsServicePollingPeriod";
	/* package private */static final Integer DEFAULT_IMS_SERVICE_POLLING_PERIOD = 300;

	/**
	 * Default SIP port
	 */
	public static final String SIP_DEFAULT_PORT = "SipListeningPort";
	/* package private */static final Integer DEFAULT_SIP_DEFAULT_PORT = 5062;

	/**
	 * Default SIP protocol for mobile
	 */
	public static final String SIP_DEFAULT_PROTOCOL_FOR_MOBILE = "SipDefaultProtocolForMobile";
	/* package private */static final String DEFAULT_SIP_DEFAULT_PROTOCOL_FOR_MOBILE = ListeningPoint.UDP;

	/**
	 * Default SIP protocol for Wi-Fi
	 */
	public static final String SIP_DEFAULT_PROTOCOL_FOR_WIFI = "SipDefaultProtocolForWifi";
	/* package private */static final String DEFAULT_SIP_DEFAULT_PROTOCOL_FOR_WIFI = ListeningPoint.TCP;

	/**
	 * TLS Certificate root
	 */
	public static final String TLS_CERTIFICATE_ROOT = "TlsCertificateRoot";
	/* package private */static final String DEFAULT_TLS_CERTIFICATE_ROOT = "";

	/**
	 * TLS Certificate intermediate
	 */
	public static final String TLS_CERTIFICATE_INTERMEDIATE = "TlsCertificateIntermediate";
	/* package private */static final String DEFAULT_TLS_CERTIFICATE_INTERMEDIATE = "";

	/**
	 * SIP transaction timeout used to wait a SIP response
	 */
	public static final String SIP_TRANSACTION_TIMEOUT = "SipTransactionTimeout";
	/* package private */static final Integer DEFAULT_SIP_TRANSACTION_TIMEOUT = 120;

	/**
	 * Default TCP port for MSRP session
	 */
	public static final String MSRP_DEFAULT_PORT = "DefaultMsrpPort";
	/* package private */static final Integer DEFAULT_MSRP_DEFAULT_PORT = 20000;

	/**
	 * Default UDP port for RTP session
	 */
	public static final String RTP_DEFAULT_PORT = "DefaultRtpPort";
	/* package private */static final Integer DEFAULT_RTP_DEFAULT_PORT = 10000;

	/**
	 * MSRP transaction timeout used to wait MSRP response
	 */
	public static final String MSRP_TRANSACTION_TIMEOUT = "MsrpTransactionTimeout";
	/* package private */static final Integer DEFAULT_MSRP_TRANSACTION_TIMEOUT = 5;

	/**
	 * Registration expire period
	 */
	public static final String REGISTER_EXPIRE_PERIOD = "RegisterExpirePeriod";
	/* package private */static final Integer DEFAULT_REGISTER_EXPIRE_PERIOD = 600000;

	/**
	 * Registration retry base time
	 */
	public static final String REGISTER_RETRY_BASE_TIME = "RegisterRetryBaseTime";
	/* package private */static final Integer DEFAULT_REGISTER_RETRY_BASE_TIME = 30;

	/**
	 * Registration retry max time
	 */
	public static final String REGISTER_RETRY_MAX_TIME = "RegisterRetryMaxTime";
	/* package private */static final Integer DEFAULT_REGISTER_RETRY_MAX_TIME = 1800;

	/**
	 * Publish expire period
	 */
	public static final String PUBLISH_EXPIRE_PERIOD = "PublishExpirePeriod";
	/* package private */static final Integer DEFAULT_PUBLISH_EXPIRE_PERIOD = 3600;

	/**
	 * Revoke timeout
	 */
	public static final String REVOKE_TIMEOUT = "RevokeTimeout";
	/* package private */static final Integer DEFAULT_REVOKE_TIMEOUT = 300;

	/**
	 * IMS authentication procedure for mobile access
	 */
	public static final String IMS_AUTHENT_PROCEDURE_MOBILE = "ImsAuthenticationProcedureForMobile";
	/* package private */static final String DEFAULT_IMS_AUTHENT_PROCEDURE_MOBILE = AuthenticationProcedure.DIGEST.name();
	
	/**
	 * IMS authentication procedure for Wi-Fi access
	 */
	public static final String IMS_AUTHENT_PROCEDURE_WIFI = "ImsAuthenticationProcedureForWifi";
	/* package private */static final String DEFAULT_IMS_AUTHENT_PROCEDURE_WIFI = AuthenticationProcedure.DIGEST.name();

	/**
	 * Activate or not Tel-URI format
	 */
	public static final String TEL_URI_FORMAT = "TelUriFormat";
	/* package private */static final Boolean DEFAULT_TEL_URI_FORMAT = true;

	/**
	 * Ringing session period. At the end of the period the session is cancelled
	 */
	public static final String RINGING_SESSION_PERIOD = "RingingPeriod";
	/* package private */static final Integer DEFAULT_RINGING_SESSION_PERIOD = 60;

	/**
	 * Subscribe expiration timeout
	 */
	public static final String SUBSCRIBE_EXPIRE_PERIOD = "SubscribeExpirePeriod";
	/* package private */static final Integer DEFAULT_SUBSCRIBE_EXPIRE_PERIOD = 3600;

	/**
	 * "Is-composing" timeout for chat service
	 */
	public static final String IS_COMPOSING_TIMEOUT = "IsComposingTimeout";
	/* package private */static final Integer DEFAULT_IS_COMPOSING_TIMEOUT = 5;

	/**
	 * SIP session refresh expire period
	 */
	public static final String SESSION_REFRESH_EXPIRE_PERIOD = "SessionRefreshExpirePeriod";
	/* package private */static final Integer DEFAULT_SESSION_REFRESH_EXPIRE_PERIOD = 0;

	/**
	 * Activate or not permanent state mode
	 */
	public static final String PERMANENT_STATE_MODE = "PermanentState";
	/* package private */static final Boolean DEFAULT_PERMANENT_STATE_MODE = true;

	/**
	 * Activate or not the traces
	 */
	public static final String TRACE_ACTIVATED = "TraceActivated";
	/* package private */static final Boolean DEFAULT_TRACE_ACTIVATED = true;

	/**
	 * Logger trace level
	 */
	public static final String TRACE_LEVEL = "TraceLevel";
	/* package private */static final Integer DEFAULT_TRACE_LEVEL = Logger.DEBUG_LEVEL;

	/**
	 * Activate or not the SIP trace
	 */
	public static final String SIP_TRACE_ACTIVATED = "SipTraceActivated";
	/* package private */static final Boolean DEFAULT_SIP_TRACE_ACTIVATED = false;

	/**
	 * SIP trace file
	 */
	public static final String SIP_TRACE_FILE = "SipTraceFile";
	/* package private */static final String DEFAULT_SIP_TRACE_FILE = Environment.getExternalStorageDirectory() + "/sip.txt";

	/**
	 * Activate or not the media trace
	 */
	public static final String MEDIA_TRACE_ACTIVATED = "MediaTraceActivated";
	/* package private */static final Boolean DEFAULT_MEDIA_TRACE_ACTIVATED = false;

	/**
	 * Capability refresh timeout used to avoid too many requests in a short time
	 */
	public static final String CAPABILITY_REFRESH_TIMEOUT = "CapabilityRefreshTimeout";
	/* package private */static final Integer DEFAULT_CAPABILITY_REFRESH_TIMEOUT = 1;

	/**
	 * Capability refresh timeout used to decide when to refresh contact capabilities
	 */
	public static final String CAPABILITY_EXPIRY_TIMEOUT = "CapabilityExpiryTimeout";
	/* package private */static final Integer DEFAULT_CAPABILITY_EXPIRY_TIMEOUT = 86400;

	/**
	 * Polling period used to decide when to refresh contacts capabilities
	 */
	public static final String CAPABILITY_POLLING_PERIOD = "CapabilityPollingPeriod";
	/* package private */static final Integer DEFAULT_CAPABILITY_POLLING_PERIOD = 3600;

	/**
	 * CS video capability
	 */
	public static final String CAPABILITY_CS_VIDEO = "CapabilityCsVideo";
	/* package private */static final Boolean DEFAULT_CAPABILITY_CS_VIDEO = false;

	/**
	 * Image sharing capability
	 */
	public static final String CAPABILITY_IMAGE_SHARING = "CapabilityImageShare";
	/* package private */static final Boolean DEFAULT_CAPABILITY_ISH = true;

	/**
	 * Video sharing capability
	 */
	public static final String CAPABILITY_VIDEO_SHARING = "CapabilityVideoShare";
	/* package private */static final Boolean DEFAULT_CAPABILITY_VSH = true;

	/**
	 * IP voice call capability
	 */
	public static final String CAPABILITY_IP_VOICE_CALL = "CapabilityIPVoiceCall";
	/* package private */static final Boolean DEFAULT_CAPABILITY_IP_VOICE_CALL = true;

	/**
	 * IP video call capability
	 */
	public static final String CAPABILITY_IP_VIDEO_CALL = "CapabilityIPVideoCall";
	/* package private */static final Boolean DEFAULT_CAPABILITY_IP_VIDEO_CALL = true;

	/**
	 * Instant Messaging session capability
	 */
	public static final String CAPABILITY_IM_SESSION = "CapabilityImSession";
	/* package private */static final Boolean DEFAULT_CAPABILITY_IM_SESSION = true;

	/**
	 * Group Instant Messaging session capability
	 */
	public static final String CAPABILITY_IM_GROUP_SESSION = "CapabilityImGroupSession";
	/* package private */static final Boolean DEFAULT_CAPABILITY_IM_GROUP_SESSION = true;

	/**
	 * File transfer capability
	 */
	public static final String CAPABILITY_FILE_TRANSFER = "CapabilityFileTransfer";
	/* package private */static final Boolean DEFAULT_CAPABILITY_FT = true;

	/**
	 * File transfer via HTTP capability
	 */
	public static final String CAPABILITY_FILE_TRANSFER_HTTP = "CapabilityFileTransferHttp";
	/* package private */static final Boolean DEFAULT_CAPABILITY_FT_HTTP = true;

	/**
	 * Presence discovery capability
	 */
	public static final String CAPABILITY_PRESENCE_DISCOVERY = "CapabilityPresenceDiscovery";
	/* package private */static final Boolean DEFAULT_CAPABILITY_PRESENCE_DISCOVERY = false;

	/**
	 * Social presence capability
	 */
	public static final String CAPABILITY_SOCIAL_PRESENCE = "CapabilitySocialPresence";
	/* package private */static final Boolean DEFAULT_CAPABILITY_SOCIAL_PRESENCE = false;

	/**
	 * Geolocation push capability
	 */
	public static final String CAPABILITY_GEOLOCATION_PUSH = "CapabilityGeoLocationPush";
	/* package private */static final Boolean DEFAULT_CAPABILITY_GEOLOCATION_PUSH = true;

	/**
	 * File transfer thumbnail capability
	 */
	public static final String CAPABILITY_FILE_TRANSFER_THUMBNAIL = "CapabilityFileTransferThumbnail";
	/* package private */static final Boolean DEFAULT_CAPABILITY_FT_THUMBNAIL = false;

	/**
	 * File transfer Store & Forward
	 */
	public static final String CAPABILITY_FILE_TRANSFER_SF = "CapabilityFileTransferSF";
	/* package private */static final Boolean DEFAULT_CAPABILITY_FT_SF = false;

	/**
	 * Group chat Store & Forward
	 */
	public static final String CAPABILITY_GROUP_CHAT_SF = "CapabilityGroupChatSF";
	/* package private */static final Boolean DEFAULT_CAPABILITY_GC_SF = false;

	/**
	 * RCS extensions capability
	 */
	public static final String CAPABILITY_RCS_EXTENSIONS = "CapabilityRcsExtensions";
	/* package private */static final String DEFAULT_CAPABILITY_RCS_EXTENSIONS = "";
	
	/**
	 * Instant messaging is always on (Store & Forward server)
	 */
	public static final String IM_CAPABILITY_ALWAYS_ON = "ImAlwaysOn";
	/* package private */static final Boolean DEFAULT_IM_CAPABILITY_ALWAYS_ON = true;

	/**
	 * SIP Automata capability (@see RFC3840)
	 */
	public static final String CAPABILITY_SIP_AUTOMATA = "CapabilitySipAutomata";
	/* package private */static final Boolean DEFAULT_CAPABILITY_SIP_AUTOMATA = false;

	/**
	 * File transfer always on (Store & Forward server)
	 */
	public static final String FT_CAPABILITY_ALWAYS_ON = "FtAlwaysOn";
	/* package private */static final Boolean DEFAULT_FT_CAPABILITY_ALWAYS_ON = false;

	/**
	 * Instant messaging use report
	 */
	public static final String IM_USE_REPORTS = "ImUseReports";
	/* package private */static final Boolean DEFAULT_IM_USE_REPORTS = true;

	/**
	 * Network access authorized
	 */
	public static final String NETWORK_ACCESS = "NetworkAccess";
	/* package private */static final Integer DEFAULT_NETWORK_ACCESS = NetworkAccessType.ANY.toInt();

	/**
	 * SIP stack timer T1
	 */
	public static final String SIP_TIMER_T1 = "SipTimerT1";
	/* package private */static final Integer DEFAULT_SIP_TIMER_T1 = 2000;

	/**
	 * SIP stack timer T2
	 */
	public static final String SIP_TIMER_T2 = "SipTimerT2";
	/* package private */static final Integer DEFAULT_SIP_TIMER_T2 = 16000;

	/**
	 * SIP stack timer T4
	 */
	public static final String SIP_TIMER_T4 = "SipTimerT4";
	/* package private */static final Integer DEFAULT_SIP_TIMER_T4 = 17000;

	/**
	 * Enable SIP keep alive
	 */
	public static final String SIP_KEEP_ALIVE = "SipKeepAlive";
	/* package private */static final Boolean DEFAULT_SIP_KEEP_ALIVE = true;

	/**
	 * SIP keep alive period
	 */
	public static final String SIP_KEEP_ALIVE_PERIOD = "SipKeepAlivePeriod";
	/* package private */static final Integer DEFAULT_SIP_KEEP_ALIVE_PERIOD = 60;

	/**
	 * RCS APN
	 */
	public static final String RCS_APN = "RcsApn";
	/* package private */static final String DEFAULT_RCS_APN = "";

	/**
	 * RCS operator
	 */
	public static final String RCS_OPERATOR = "RcsOperator";
	/* package private */static final String DEFAULT_RCS_OPERATOR = "";

	/**
	 * GRUU support
	 */
	public static final String GRUU = "GRUU";
	/* package private */static final Boolean DEFAULT_GRUU = true;

	/**
	 * IMEI used as device ID
	 */
	public static final String USE_IMEI_AS_DEVICE_ID = "ImeiDeviceId";
	/* package private */static final Boolean DEFAULT_USE_IMEI_AS_DEVICE_ID = true;

	/**
	 * CPU always_on support
	 */
	public static final String CPU_ALWAYS_ON = "CpuAlwaysOn";
	/* package private */static final Boolean DEFAULT_CPU_ALWAYS_ON = false;

	/**
	 * Configuration mode
	 */
	public static final String CONFIG_MODE = "ConfigMode";
	/* package private */static final Integer DEFAULT_CONFIG_MODE = ConfigurationMode.AUTO.toInt();

	/**
	 * Provisioning terms accepted
	 */
	public static final String PROVISIONING_TERMS_ACCEPTED = "ProvisioningTermsAccepted";
	/* package private */static final Boolean DEFAULT_PROVISIONING_TERMS_ACCEPTED = false;

	/**
	 * Provisioning version
	 */
	public static final String PROVISIONING_VERSION = "ProvisioningVersion";
	/* package private */static final String DEFAULT_PROVISIONING_VERSION = "0";
    
	/**
	 * Provisioning version
	 */
	public static final String PROVISIONING_TOKEN = "ProvisioningToken";
	/* package private */static final String DEFAULT_PROVISIONING_TOKEN = "";

	/**
	 * Secondary provisioning address
	 */
	public static final String SECONDARY_PROVISIONING_ADDRESS = "SecondaryProvisioningAddress";
	/* package private */static final String DEFAULT_SECONDARY_PROV_ADDR = "";

	/**
	 * Use only the secondary provisioning address
	 */
	public static final String SECONDARY_PROVISIONING_ADDRESS_ONLY = "SecondaryProvisioningAddressOnly";
	/* package private */static final Boolean DEFAULT_SECONDARY_PROV_ADDR_ONLY = false;

	/**
	 * Directory path for photos
	 */
	public static final String DIRECTORY_PATH_PHOTOS = "DirectoryPathPhotos";
	/* package private */static final String DEFAULT_DIRECTORY_PATH_PHOTOS = Environment.getExternalStorageDirectory()
			+ "/rcs/photos/";

	/**
	 * Directory path for videos
	 */
	public static final String DIRECTORY_PATH_VIDEOS = "DirectoryPathVideos";
	/* package private */static final String DEFAULT_DIRECTORY_PATH_VIDEOS = Environment.getExternalStorageDirectory()
			+ "/rcs/videos/";

	/**
	 * Directory path for files
	 */
	public static final String DIRECTORY_PATH_FILES = "DirectoryPathFiles";
	/* package private */static final String DEFAULT_DIRECTORY_PATH_FILES = Environment.getExternalStorageDirectory()
			+ "/rcs/files/";

	/**
	 * Secure MSRP over Wi-Fi
	 */
	public static final String SECURE_MSRP_OVER_WIFI = "SecureMsrpOverWifi";
	/* package private */static final Boolean DEFAULT_SECURE_MSRP_OVER_WIFI = false;

	/**
	 * Secured RTP over Wi-Fi
	 */
	public static final String SECURE_RTP_OVER_WIFI = "SecureRtpOverWifi";
	/* package private */static final Boolean DEFAULT_SECURE_RTP_OVER_WIFI = false;

	/**
	 * Key and associated values for GSMA release of the device as provisioned by the network
	 */
	public static final String KEY_GSMA_RELEASE = "GsmaRelease";
	/* package private */static final Integer DEFAULT_KEY_GSMA_RELEASE = GsmaRelease.BLACKBIRD.toInt();
    
	/**
	 * IP voice call breakout capabilities in RCS-AA mode
	 */
	public static final String IPVOICECALL_BREAKOUT_AA = "IPCallBreakOutAA";
	/* package private */static final Boolean DEFAULT_IPVOICECALL_BREAKOUT_AA = false;

	/**
	 * IP voice call breakout capabilities in RCS-CS mode
	 */
	public static final String IPVOICECALL_BREAKOUT_CS = "IPCallBreakOutCS";
	/* package private */static final Boolean DEFAULT_IPVOICECALL_BREAKOUT_CS = false;

	/**
	 * CS call upgrade to IP Video Call in RCS-CS mode
	 */
	public static final String IPVIDEOCALL_UPGRADE_FROM_CS = "rcsIPVideoCallUpgradeFromCS";
	/* package private */static final Boolean DEFAULT_IPVIDEOCALL_UPGRADE_FROM_CS = false;

	/**
	 * CS call upgrade to IP Video Call in case of capability error
	 */
	public static final String IPVIDEOCALL_UPGRADE_ON_CAPERROR = "rcsIPVideoCallUpgradeOnCapError";
	/* package private */static final Boolean DEFAULT_IPVIDEOCALL_UPGRADE_ON_CAPERROR = false;

	/**
	 * Leaf node that tells an RCS-CS device whether it can initiate an RCS IP Video Call upgrade without first tearing down the CS
	 * voice call
	 */
	public static final String IPVIDEOCALL_UPGRADE_ATTEMPT_EARLY = "rcsIPVideoCallUpgradeAttemptEarly";
	/* package private */static final Boolean DEFAULT_IPVIDEOCALL_UPGRADE_ATTEMPT_EARLY = false;

	/**
	 * TCP fallback option
	 */
	public static final String TCP_FALLBACK = "TcpFallback";
	/* package private */static final Boolean DEFAULT_TCP_FALLBACK = false;

	/**
	 * Vendor name of the Client.
	 */
	public static final String VENDOR_NAME = "VendorName";
	/* package private */static final String DEFAULT_VENDOR_NAME = "OrangeLabs";

	/**
	 * Control RCS extensions
	 */
	public static final String CONTROL_EXTENSIONS = "ControlRcsExtensions";
	/* package private */static final Boolean DEFAULT_CONTROL_EXTENSIONS = false;

	/**
	 * Allow RCS extensions
	 */
	public static final String ALLOW_EXTENSIONS = "AllowRcsExtensions";
	/* package private */static final Boolean DEFAULT_ALLOW_EXTENSIONS = true;

	/**
	 * Max length for extensions using real time messaging (MSRP)
	 */
	public static final String MAX_MSRP_SIZE_EXTENSIONS = "ExtensionsMaxMsrpSize";
	/* package private */static final Integer DEFAULT_MAX_MSRP_SIZE_EXTENSIONS = 0;

	/**
	 * Validity of the RCS configuration.
	 */
	public static final String CONFIGURATION_VALID = RcsServiceConfiguration.Settings.CONFIGURATION_VALIDITY;
	/* package private */static final Boolean DEFAULT_CONFIGURATION_VALID = false;

	/**
	 * Auto accept file transfer invitation in roaming
	 */
	public static final String AUTO_ACCEPT_FT_IN_ROAMING = "AutoAcceptFtInRoaming";
	/* package private */static final Boolean DEFAULT_AUTO_ACCEPT_FT_IN_ROAMING = false;

	/**
	 * Auto accept file transfer enabled
	 */
	public static final String AUTO_ACCEPT_FT_CHANGEABLE = "AutoAcceptFtChangeable";
	/* package private */static final Boolean DEFAULT_AUTO_ACCEPT_FT_CHANGEABLE = false;
	
	/**
	 * Image resize option
	 */
	public static final String KEY_IMAGE_RESIZE_OPTION = "ImageResizeOption";
	/* package private */static final Integer DEFAULT_KEY_IMAGE_RESIZE_OPTION = ImageResizeOption.ONLY_ABOVE_MAX_SIZE.toInt();
	
}
