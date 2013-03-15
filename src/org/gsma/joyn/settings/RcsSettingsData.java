/*
 * Copyright 2013, France Telecom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsma.joyn.settings;

import java.lang.String;

/**
 * Class RcsSettingsData.
 */
public class RcsSettingsData {
    /**
     * Constant DEFAULT_GROUP_CHAT_URI.
     */
    public static final String DEFAULT_GROUP_CHAT_URI = "sip:foo@bar";

    /**
     * Constant TRUE.
     */
    public static final String TRUE = null;

    /**
     * Constant FALSE.
     */
    public static final String FALSE = null;

    /**
     * Constant GIBA_AUTHENT.
     */
    public static final String GIBA_AUTHENT = "GIBA";

    /**
     * Constant DIGEST_AUTHENT.
     */
    public static final String DIGEST_AUTHENT = "DIGEST";

    /**
     * Constant ANY_ACCESS.
     */
    public static final int ANY_ACCESS = -1;

    /**
     * Constant MOBILE_ACCESS.
     */
    public static final int MOBILE_ACCESS = 0;

    /**
     * Constant WIFI_ACCESS.
     */
    public static final int WIFI_ACCESS = 1;

    /**
     * Constant CERTIFICATE_FILE_TYPE.
     */
    public static final String CERTIFICATE_FILE_TYPE = ".crt";

    /**
     * Constant NO_AUTO_CONFIG.
     */
    public static final int NO_AUTO_CONFIG = 0;

    /**
     * Constant HTTPS_AUTO_CONFIG.
     */
    public static final int HTTPS_AUTO_CONFIG = 1;

    /**
     * Constant SERVICE_ACTIVATED.
     */
    public static final String SERVICE_ACTIVATED = "ServiceActivated";

    /**
     * Constant ROAMING_AUTHORIZED.
     */
    public static final String ROAMING_AUTHORIZED = "RoamingAuthorized";

    /**
     * Constant PRESENCE_INVITATION_RINGTONE.
     */
    public static final String PRESENCE_INVITATION_RINGTONE = "PresenceInvitationRingtone";

    /**
     * Constant PRESENCE_INVITATION_VIBRATE.
     */
    public static final String PRESENCE_INVITATION_VIBRATE = "PresenceInvitationVibrate";

    /**
     * Constant CSH_INVITATION_RINGTONE.
     */
    public static final String CSH_INVITATION_RINGTONE = "CShInvitationRingtone";

    /**
     * Constant CSH_INVITATION_VIBRATE.
     */
    public static final String CSH_INVITATION_VIBRATE = "CShInvitationVibrate";

    /**
     * Constant CSH_AVAILABLE_BEEP.
     */
    public static final String CSH_AVAILABLE_BEEP = "CShAvailableBeep";

    /**
     * Constant FILETRANSFER_INVITATION_RINGTONE.
     */
    public static final String FILETRANSFER_INVITATION_RINGTONE = "FileTransferInvitationRingtone";

    /**
     * Constant FILETRANSFER_INVITATION_VIBRATE.
     */
    public static final String FILETRANSFER_INVITATION_VIBRATE = "FileTransferInvitationVibrate";

    /**
     * Constant CHAT_INVITATION_RINGTONE.
     */
    public static final String CHAT_INVITATION_RINGTONE = "ChatInvitationRingtone";

    /**
     * Constant CHAT_INVITATION_VIBRATE.
     */
    public static final String CHAT_INVITATION_VIBRATE = "ChatInvitationVibrate";

    /**
     * Constant FREETEXT1.
     */
    public static final String FREETEXT1 = "Freetext1";

    /**
     * Constant FREETEXT2.
     */
    public static final String FREETEXT2 = "Freetext2";

    /**
     * Constant FREETEXT3.
     */
    public static final String FREETEXT3 = "Freetext3";

    /**
     * Constant FREETEXT4.
     */
    public static final String FREETEXT4 = "Freetext4";

    /**
     * Constant MIN_BATTERY_LEVEL.
     */
    public static final String MIN_BATTERY_LEVEL = "MinBatteryLevel";

    /**
     * Constant MAX_PHOTO_ICON_SIZE.
     */
    public static final String MAX_PHOTO_ICON_SIZE = "MaxPhotoIconSize";

    /**
     * Constant MAX_FREETXT_LENGTH.
     */
    public static final String MAX_FREETXT_LENGTH = "MaxFreetextLength";

    /**
     * Constant MAX_CHAT_PARTICIPANTS.
     */
    public static final String MAX_CHAT_PARTICIPANTS = "MaxChatParticipants";

    /**
     * Constant MAX_CHAT_MSG_LENGTH.
     */
    public static final String MAX_CHAT_MSG_LENGTH = "MaxChatMessageLength";

    /**
     * Constant CHAT_IDLE_DURATION.
     */
    public static final String CHAT_IDLE_DURATION = "ChatIdleDuration";

    /**
     * Constant MAX_FILE_TRANSFER_SIZE.
     */
    public static final String MAX_FILE_TRANSFER_SIZE = "MaxFileTransferSize";

    /**
     * Constant WARN_FILE_TRANSFER_SIZE.
     */
    public static final String WARN_FILE_TRANSFER_SIZE = "WarnFileTransferSize";

    /**
     * Constant MAX_IMAGE_SHARE_SIZE.
     */
    public static final String MAX_IMAGE_SHARE_SIZE = "MaxImageShareSize";

    /**
     * Constant MAX_VIDEO_SHARE_DURATION.
     */
    public static final String MAX_VIDEO_SHARE_DURATION = "MaxVideoShareDuration";

    /**
     * Constant MAX_CHAT_SESSIONS.
     */
    public static final String MAX_CHAT_SESSIONS = "MaxChatSessions";

    /**
     * Constant MAX_FILE_TRANSFER_SESSIONS.
     */
    public static final String MAX_FILE_TRANSFER_SESSIONS = "MaxFileTransferSessions";

    /**
     * Constant SMS_FALLBACK_SERVICE.
     */
    public static final String SMS_FALLBACK_SERVICE = "SmsFallbackService";

    /**
     * Constant AUTO_ACCEPT_FILE_TRANSFER.
     */
    public static final String AUTO_ACCEPT_FILE_TRANSFER = "AutoAcceptFileTransfer";

    /**
     * Constant AUTO_ACCEPT_CHAT.
     */
    public static final String AUTO_ACCEPT_CHAT = "AutoAcceptChat";

    /**
     * Constant AUTO_ACCEPT_GROUP_CHAT.
     */
    public static final String AUTO_ACCEPT_GROUP_CHAT = "AutoAcceptGroupChat";

    /**
     * Constant WARN_SF_SERVICE.
     */
    public static final String WARN_SF_SERVICE = "StoreForwardServiceWarning";

    /**
     * Constant IM_SESSION_START.
     */
    public static final String IM_SESSION_START = "ImSessionStart";

    /**
     * Constant MAX_CHAT_LOG_ENTRIES.
     */
    public static final String MAX_CHAT_LOG_ENTRIES = "MaxChatLogEntries";

    /**
     * Constant MAX_RICHCALL_LOG_ENTRIES.
     */
    public static final String MAX_RICHCALL_LOG_ENTRIES = "MaxRichcallLogEntries";

    /**
     * Constant USERPROFILE_IMS_USERNAME.
     */
    public static final String USERPROFILE_IMS_USERNAME = "ImsUsername";

    /**
     * Constant USERPROFILE_IMS_DISPLAY_NAME.
     */
    public static final String USERPROFILE_IMS_DISPLAY_NAME = "ImsDisplayName";

    /**
     * Constant USERPROFILE_IMS_HOME_DOMAIN.
     */
    public static final String USERPROFILE_IMS_HOME_DOMAIN = "ImsHomeDomain";

    /**
     * Constant USERPROFILE_IMS_PRIVATE_ID.
     */
    public static final String USERPROFILE_IMS_PRIVATE_ID = "ImsPrivateId";

    /**
     * Constant USERPROFILE_IMS_PASSWORD.
     */
    public static final String USERPROFILE_IMS_PASSWORD = "ImsPassword";

    /**
     * Constant USERPROFILE_IMS_REALM.
     */
    public static final String USERPROFILE_IMS_REALM = "ImsRealm";

    /**
     * Constant IMS_PROXY_ADDR_MOBILE.
     */
    public static final String IMS_PROXY_ADDR_MOBILE = "ImsOutboundProxyAddrForMobile";

    /**
     * Constant IMS_PROXY_PORT_MOBILE.
     */
    public static final String IMS_PROXY_PORT_MOBILE = "ImsOutboundProxyPortForMobile";

    /**
     * Constant IMS_PROXY_ADDR_WIFI.
     */
    public static final String IMS_PROXY_ADDR_WIFI = "ImsOutboundProxyAddrForWifi";

    /**
     * Constant IMS_PROXY_PORT_WIFI.
     */
    public static final String IMS_PROXY_PORT_WIFI = "ImsOutboundProxyPortForWifi";

    /**
     * Constant XDM_SERVER.
     */
    public static final String XDM_SERVER = "XdmServerAddr";

    /**
     * Constant XDM_LOGIN.
     */
    public static final String XDM_LOGIN = "XdmServerLogin";

    /**
     * Constant XDM_PASSWORD.
     */
    public static final String XDM_PASSWORD = "XdmServerPassword";

    /**
     * Constant IM_CONF_URI.
     */
    public static final String IM_CONF_URI = "ImConferenceUri";

    /**
     * Constant ENDUSER_CONFIRMATION_URI.
     */
    public static final String ENDUSER_CONFIRMATION_URI = "EndUserConfReqUri";

    /**
     * Constant COUNTRY_CODE.
     */
    public static final String COUNTRY_CODE = "CountryCode";

    /**
     * Constant COUNTRY_AREA_CODE.
     */
    public static final String COUNTRY_AREA_CODE = "CountryAreaCode";

    /**
     * Constant IMS_SERVICE_POLLING_PERIOD.
     */
    public static final String IMS_SERVICE_POLLING_PERIOD = "ImsServicePollingPeriod";

    /**
     * Constant SIP_DEFAULT_PORT.
     */
    public static final String SIP_DEFAULT_PORT = "SipListeningPort";

    /**
     * Constant SIP_DEFAULT_PROTOCOL_FOR_MOBILE.
     */
    public static final String SIP_DEFAULT_PROTOCOL_FOR_MOBILE = "SipDefaultProtocolForMobile";

    /**
     * Constant SIP_DEFAULT_PROTOCOL_FOR_WIFI.
     */
    public static final String SIP_DEFAULT_PROTOCOL_FOR_WIFI = "SipDefaultProtocolForWifi";

    /**
     * Constant TLS_CERTIFICATE_ROOT.
     */
    public static final String TLS_CERTIFICATE_ROOT = "TlsCertificateRoot";

    /**
     * Constant TLS_CERTIFICATE_INTERMEDIATE.
     */
    public static final String TLS_CERTIFICATE_INTERMEDIATE = "TlsCertificateIntermediate";

    /**
     * Constant SIP_TRANSACTION_TIMEOUT.
     */
    public static final String SIP_TRANSACTION_TIMEOUT = "SipTransactionTimeout";

    /**
     * Constant MSRP_DEFAULT_PORT.
     */
    public static final String MSRP_DEFAULT_PORT = "DefaultMsrpPort";

    /**
     * Constant RTP_DEFAULT_PORT.
     */
    public static final String RTP_DEFAULT_PORT = "DefaultRtpPort";

    /**
     * Constant MSRP_TRANSACTION_TIMEOUT.
     */
    public static final String MSRP_TRANSACTION_TIMEOUT = "MsrpTransactionTimeout";

    /**
     * Constant REGISTER_EXPIRE_PERIOD.
     */
    public static final String REGISTER_EXPIRE_PERIOD = "RegisterExpirePeriod";

    /**
     * Constant REGISTER_RETRY_BASE_TIME.
     */
    public static final String REGISTER_RETRY_BASE_TIME = "RegisterRetryBaseTime";

    /**
     * Constant REGISTER_RETRY_MAX_TIME.
     */
    public static final String REGISTER_RETRY_MAX_TIME = "RegisterRetryMaxTime";

    /**
     * Constant PUBLISH_EXPIRE_PERIOD.
     */
    public static final String PUBLISH_EXPIRE_PERIOD = "PublishExpirePeriod";

    /**
     * Constant REVOKE_TIMEOUT.
     */
    public static final String REVOKE_TIMEOUT = "RevokeTimeout";

    /**
     * Constant IMS_AUTHENT_PROCEDURE_MOBILE.
     */
    public static final String IMS_AUTHENT_PROCEDURE_MOBILE = "ImsAuhtenticationProcedureForMobile";

    /**
     * Constant IMS_AUTHENT_PROCEDURE_WIFI.
     */
    public static final String IMS_AUTHENT_PROCEDURE_WIFI = "ImsAuhtenticationProcedureForWifi";

    /**
     * Constant TEL_URI_FORMAT.
     */
    public static final String TEL_URI_FORMAT = "TelUriFormat";

    /**
     * Constant RINGING_SESSION_PERIOD.
     */
    public static final String RINGING_SESSION_PERIOD = "RingingPeriod";

    /**
     * Constant SUBSCRIBE_EXPIRE_PERIOD.
     */
    public static final String SUBSCRIBE_EXPIRE_PERIOD = "SubscribeExpirePeriod";

    /**
     * Constant IS_COMPOSING_TIMEOUT.
     */
    public static final String IS_COMPOSING_TIMEOUT = "IsComposingTimeout";

    /**
     * Constant SESSION_REFRESH_EXPIRE_PERIOD.
     */
    public static final String SESSION_REFRESH_EXPIRE_PERIOD = "SessionRefreshExpirePeriod";

    /**
     * Constant PERMANENT_STATE_MODE.
     */
    public static final String PERMANENT_STATE_MODE = "PermanentState";

    /**
     * Constant TRACE_ACTIVATED.
     */
    public static final String TRACE_ACTIVATED = "TraceActivated";

    /**
     * Constant TRACE_LEVEL.
     */
    public static final String TRACE_LEVEL = "TraceLevel";

    /**
     * Constant SIP_TRACE_ACTIVATED.
     */
    public static final String SIP_TRACE_ACTIVATED = "SipTraceActivated";

    /**
     * Constant SIP_TRACE_FILE.
     */
    public static final String SIP_TRACE_FILE = "SipTraceFile";

    /**
     * Constant MEDIA_TRACE_ACTIVATED.
     */
    public static final String MEDIA_TRACE_ACTIVATED = "MediaTraceActivated";

    /**
     * Constant CAPABILITY_REFRESH_TIMEOUT.
     */
    public static final String CAPABILITY_REFRESH_TIMEOUT = "CapabilityRefreshTimeout";

    /**
     * Constant CAPABILITY_EXPIRY_TIMEOUT.
     */
    public static final String CAPABILITY_EXPIRY_TIMEOUT = "CapabilityExpiryTimeout";

    /**
     * Constant CAPABILITY_POLLING_PERIOD.
     */
    public static final String CAPABILITY_POLLING_PERIOD = "CapabilityPollingPeriod";

    /**
     * Constant CAPABILITY_CS_VIDEO.
     */
    public static final String CAPABILITY_CS_VIDEO = "CapabilityCsVideo";

    /**
     * Constant CAPABILITY_IMAGE_SHARING.
     */
    public static final String CAPABILITY_IMAGE_SHARING = "CapabilityImageShare";

    /**
     * Constant CAPABILITY_VIDEO_SHARING.
     */
    public static final String CAPABILITY_VIDEO_SHARING = "CapabilityVideoShare";

    /**
     * Constant CAPABILITY_IM_SESSION.
     */
    public static final String CAPABILITY_IM_SESSION = "CapabilityImSession";

    /**
     * Constant CAPABILITY_FILE_TRANSFER.
     */
    public static final String CAPABILITY_FILE_TRANSFER = "CapabilityFileTransfer";

    /**
     * Constant CAPABILITY_PRESENCE_DISCOVERY.
     */
    public static final String CAPABILITY_PRESENCE_DISCOVERY = "CapabilityPresenceDiscovery";

    /**
     * Constant CAPABILITY_SOCIAL_PRESENCE.
     */
    public static final String CAPABILITY_SOCIAL_PRESENCE = "CapabilitySocialPresence";

    /**
     * Constant CAPABILITY_RCS_EXTENSIONS.
     */
    public static final String CAPABILITY_RCS_EXTENSIONS = "CapabilityRcsExtensions";

    /**
     * Constant IM_CAPABILITY_ALWAYS_ON.
     */
    public static final String IM_CAPABILITY_ALWAYS_ON = "ImAlwaysOn";

    /**
     * Constant IM_USE_REPORTS.
     */
    public static final String IM_USE_REPORTS = "ImUseReports";

    /**
     * Constant NETWORK_ACCESS.
     */
    public static final String NETWORK_ACCESS = "NetworkAccess";

    /**
     * Constant SIP_TIMER_T1.
     */
    public static final String SIP_TIMER_T1 = "SipTimerT1";

    /**
     * Constant SIP_TIMER_T2.
     */
    public static final String SIP_TIMER_T2 = "SipTimerT2";

    /**
     * Constant SIP_TIMER_T4.
     */
    public static final String SIP_TIMER_T4 = "SipTimerT4";

    /**
     * Constant SIP_KEEP_ALIVE.
     */
    public static final String SIP_KEEP_ALIVE = "SipKeepAlive";

    /**
     * Constant SIP_KEEP_ALIVE_PERIOD.
     */
    public static final String SIP_KEEP_ALIVE_PERIOD = "SipKeepAlivePeriod";

    /**
     * Constant RCS_APN.
     */
    public static final String RCS_APN = "RcsApn";

    /**
     * Constant RCS_OPERATOR.
     */
    public static final String RCS_OPERATOR = "RcsOperator";

    /**
     * Constant GRUU.
     */
    public static final String GRUU = "GRUU";

    /**
     * Constant USE_IMEI_AS_DEVICE_ID.
     */
    public static final String USE_IMEI_AS_DEVICE_ID = "ImeiDeviceId";

    /**
     * Constant CPU_ALWAYS_ON.
     */
    public static final String CPU_ALWAYS_ON = "CpuAlwaysOn";

    /**
     * Constant AUTO_CONFIG_MODE.
     */
    public static final String AUTO_CONFIG_MODE = "Autoconfig";

    /**
     * Constant PROVISIONING_TERMS_ACCEPTED.
     */
    public static final String PROVISIONING_TERMS_ACCEPTED = "ProvisioningTermsAccepted";

    /**
     * Constant PROVISIONING_VERSION.
     */
    public static final String PROVISIONING_VERSION = "ProvisioningVersion";

    /**
     * Constant PROVISIONING_ADDRESS.
     */
    public static final String PROVISIONING_ADDRESS = "ProvisioningAddress";

    /**
     * Constant DIRECTORY_PATH_PHOTOS.
     */
    public static final String DIRECTORY_PATH_PHOTOS = "DirectoryPathPhotos";

    /**
     * Constant DIRECTORY_PATH_VIDEOS.
     */
    public static final String DIRECTORY_PATH_VIDEOS = "DirectoryPathVideos";

    /**
     * Constant DIRECTORY_PATH_FILES.
     */
    public static final String DIRECTORY_PATH_FILES = "DirectoryPathFiles";

    /**
     * Constant SECURE_MSRP_OVER_WIFI.
     */
    public static final String SECURE_MSRP_OVER_WIFI = "SecureMsrpOverWifi";

    /**
     * Constant SECURE_RTP_OVER_WIFI.
     */
    public static final String SECURE_RTP_OVER_WIFI = "SecureRtpOverWifi";

    /**
     * Creates a new instance of RcsSettingsData.
     */
    public RcsSettingsData() {

    }

} // end RcsSettingsData
