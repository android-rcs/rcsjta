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

package org.gsma.rcs.settings;

import android.content.Context;
import org.gsma.rcs.capability.Capabilities;

/**
 * Class RcsSettings.
 */
public class RcsSettings {
    /**
     * Creates a new instance of RcsSettings.
     *  
     * @param arg1 The arg1.
     */
    private RcsSettings(Context arg1) {

    }

    /**
     * Returns the sip listening port.
     *  
     * @return  The sip listening port.
     */
    public int getSipListeningPort() {
        return 0;
    }

    /**
     * Returns the default rtp port.
     *  
     * @return  The default rtp port.
     */
    public int getDefaultRtpPort() {
        return 0;
    }

    /**
     * Returns the default msrp port.
     *  
     * @return  The default msrp port.
     */
    public int getDefaultMsrpPort() {
        return 0;
    }

    /**
     * Returns the max richcall log entries per contact.
     *  
     * @return  The max richcall log entries per contact.
     */
    public int getMaxRichcallLogEntriesPerContact() {
        return 0;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isImageSharingSupported() {
        return false;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isVideoSharingSupported() {
        return false;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isImSessionSupported() {
        return false;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isFileTransferSupported() {
        return false;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isCsVideoSupported() {
        return false;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isPresenceDiscoverySupported() {
        return false;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isSocialPresenceSupported() {
        return false;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isImeiUsedAsDeviceId() {
        return false;
    }

    /**
     *  
     * @param arg1 The arg1.
     * @return  The string.
     */
    public String readParameter(String arg1) {
        return (java.lang.String) null;
    }

    /**
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     */
    public void writeParameter(String arg1, String arg2) {

    }

    /**
     *  
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     */
    public void insertParameter(String arg1, String arg2) {

    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isServiceActivated() {
        return false;
    }

    /**
     * Sets the service activation state.
     *  
     * @param arg1 The service activation state.
     */
    public void setServiceActivationState(boolean arg1) {

    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isRoamingAuthorized() {
        return false;
    }

    /**
     * Sets the roaming authorization state.
     *  
     * @param arg1 The roaming authorization state.
     */
    public void setRoamingAuthorizationState(boolean arg1) {

    }

    /**
     * Returns the presence invitation ringtone.
     *  
     * @return  The presence invitation ringtone.
     */
    public String getPresenceInvitationRingtone() {
        return (java.lang.String) null;
    }

    /**
     * Sets the presence invitation ringtone.
     *  
     * @param arg1 The presence invitation ringtone.
     */
    public void setPresenceInvitationRingtone(String arg1) {

    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isPhoneVibrateForPresenceInvitation() {
        return false;
    }

    /**
     * Sets the phone vibrate for presence invitation.
     *  
     * @param arg1 The phone vibrate for presence invitation.
     */
    public void setPhoneVibrateForPresenceInvitation(boolean arg1) {

    }

    /**
     * Returns the c sh invitation ringtone.
     *  
     * @return  The c sh invitation ringtone.
     */
    public String getCShInvitationRingtone() {
        return (java.lang.String) null;
    }

    /**
     * Sets the c sh invitation ringtone.
     *  
     * @param arg1 The c sh invitation ringtone.
     */
    public void setCShInvitationRingtone(String arg1) {

    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isPhoneVibrateForCShInvitation() {
        return false;
    }

    /**
     * Sets the phone vibrate for c sh invitation.
     *  
     * @param arg1 The phone vibrate for c sh invitation.
     */
    public void setPhoneVibrateForCShInvitation(boolean arg1) {

    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isPhoneBeepIfCShAvailable() {
        return false;
    }

    /**
     * Sets the phone beep if c sh available.
     *  
     * @param arg1 The phone beep if c sh available.
     */
    public void setPhoneBeepIfCShAvailable(boolean arg1) {

    }

    /**
     * Returns the file transfer invitation ringtone.
     *  
     * @return  The file transfer invitation ringtone.
     */
    public String getFileTransferInvitationRingtone() {
        return (java.lang.String) null;
    }

    /**
     * Sets the file transfer invitation ringtone.
     *  
     * @param arg1 The file transfer invitation ringtone.
     */
    public void setFileTransferInvitationRingtone(String arg1) {

    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isPhoneVibrateForFileTransferInvitation() {
        return false;
    }

    /**
     * Sets the phone vibrate for file transfer invitation.
     *  
     * @param arg1 The phone vibrate for file transfer invitation.
     */
    public void setPhoneVibrateForFileTransferInvitation(boolean arg1) {

    }

    /**
     * Returns the chat invitation ringtone.
     *  
     * @return  The chat invitation ringtone.
     */
    public String getChatInvitationRingtone() {
        return (java.lang.String) null;
    }

    /**
     * Sets the chat invitation ringtone.
     *  
     * @param arg1 The chat invitation ringtone.
     */
    public void setChatInvitationRingtone(String arg1) {

    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isPhoneVibrateForChatInvitation() {
        return false;
    }

    /**
     * Sets the phone vibrate for chat invitation.
     *  
     * @param arg1 The phone vibrate for chat invitation.
     */
    public void setPhoneVibrateForChatInvitation(boolean arg1) {

    }

    /**
     * Returns the predefined freetext1.
     *  
     * @return  The predefined freetext1.
     */
    public String getPredefinedFreetext1() {
        return (java.lang.String) null;
    }

    /**
     * Sets the predefined freetext1.
     *  
     * @param arg1 The predefined freetext1.
     */
    public void setPredefinedFreetext1(String arg1) {

    }

    /**
     * Returns the predefined freetext2.
     *  
     * @return  The predefined freetext2.
     */
    public String getPredefinedFreetext2() {
        return (java.lang.String) null;
    }

    /**
     * Sets the predefined freetext2.
     *  
     * @param arg1 The predefined freetext2.
     */
    public void setPredefinedFreetext2(String arg1) {

    }

    /**
     * Returns the predefined freetext3.
     *  
     * @return  The predefined freetext3.
     */
    public String getPredefinedFreetext3() {
        return (java.lang.String) null;
    }

    /**
     * Sets the predefined freetext3.
     *  
     * @param arg1 The predefined freetext3.
     */
    public void setPredefinedFreetext3(String arg1) {

    }

    /**
     * Returns the predefined freetext4.
     *  
     * @return  The predefined freetext4.
     */
    public String getPredefinedFreetext4() {
        return (java.lang.String) null;
    }

    /**
     * Sets the predefined freetext4.
     *  
     * @param arg1 The predefined freetext4.
     */
    public void setPredefinedFreetext4(String arg1) {

    }

    /**
     * Returns the min battery level.
     *  
     * @return  The min battery level.
     */
    public int getMinBatteryLevel() {
        return 0;
    }

    /**
     * Sets the min battery level.
     *  
     * @param arg1 The min battery level.
     */
    public void setMinBatteryLevel(int arg1) {

    }

    /**
     * Returns the user profile ims user name.
     *  
     * @return  The user profile ims user name.
     */
    public String getUserProfileImsUserName() {
        return (java.lang.String) null;
    }

    /**
     * Sets the user profile ims user name.
     *  
     * @param arg1 The user profile ims user name.
     */
    public void setUserProfileImsUserName(String arg1) {

    }

    /**
     * Returns the user profile ims display name.
     *  
     * @return  The user profile ims display name.
     */
    public String getUserProfileImsDisplayName() {
        return (java.lang.String) null;
    }

    /**
     * Sets the user profile ims display name.
     *  
     * @param arg1 The user profile ims display name.
     */
    public void setUserProfileImsDisplayName(String arg1) {

    }

    /**
     * Returns the user profile ims private id.
     *  
     * @return  The user profile ims private id.
     */
    public String getUserProfileImsPrivateId() {
        return (java.lang.String) null;
    }

    /**
     * Sets the user profile ims private id.
     *  
     * @param arg1 The user profile ims private id.
     */
    public void setUserProfileImsPrivateId(String arg1) {

    }

    /**
     * Returns the user profile ims password.
     *  
     * @return  The user profile ims password.
     */
    public String getUserProfileImsPassword() {
        return (java.lang.String) null;
    }

    /**
     * Sets the user profile ims password.
     *  
     * @param arg1 The user profile ims password.
     */
    public void setUserProfileImsPassword(String arg1) {

    }

    /**
     * Returns the user profile ims realm.
     *  
     * @return  The user profile ims realm.
     */
    public String getUserProfileImsRealm() {
        return (java.lang.String) null;
    }

    /**
     * Sets the user profile ims realm.
     *  
     * @param arg1 The user profile ims realm.
     */
    public void setUserProfileImsRealm(String arg1) {

    }

    /**
     * Returns the user profile ims domain.
     *  
     * @return  The user profile ims domain.
     */
    public String getUserProfileImsDomain() {
        return (java.lang.String) null;
    }

    /**
     * Sets the user profile ims domain.
     *  
     * @param arg1 The user profile ims domain.
     */
    public void setUserProfileImsDomain(String arg1) {

    }

    /**
     * Returns the ims proxy addr for mobile.
     *  
     * @return  The ims proxy addr for mobile.
     */
    public String getImsProxyAddrForMobile() {
        return (java.lang.String) null;
    }

    /**
     * Sets the ims proxy addr for mobile.
     *  
     * @param arg1 The ims proxy addr for mobile.
     */
    public void setImsProxyAddrForMobile(String arg1) {

    }

    /**
     * Returns the ims proxy port for mobile.
     *  
     * @return  The ims proxy port for mobile.
     */
    public int getImsProxyPortForMobile() {
        return 0;
    }

    /**
     * Sets the ims proxy port for mobile.
     *  
     * @param arg1 The ims proxy port for mobile.
     */
    public void setImsProxyPortForMobile(int arg1) {

    }

    /**
     * Returns the ims proxy addr for wifi.
     *  
     * @return  The ims proxy addr for wifi.
     */
    public String getImsProxyAddrForWifi() {
        return (java.lang.String) null;
    }

    /**
     * Sets the ims proxy addr for wifi.
     *  
     * @param arg1 The ims proxy addr for wifi.
     */
    public void setImsProxyAddrForWifi(String arg1) {

    }

    /**
     * Returns the ims proxy port for wifi.
     *  
     * @return  The ims proxy port for wifi.
     */
    public int getImsProxyPortForWifi() {
        return 0;
    }

    /**
     * Sets the ims proxy port for wifi.
     *  
     * @param arg1 The ims proxy port for wifi.
     */
    public void setImsProxyPortForWifi(int arg1) {

    }

    /**
     * Returns the xdm server.
     *  
     * @return  The xdm server.
     */
    public String getXdmServer() {
        return (java.lang.String) null;
    }

    /**
     * Sets the xdm server.
     *  
     * @param arg1 The xdm server.
     */
    public void setXdmServer(String arg1) {

    }

    /**
     * Returns the xdm login.
     *  
     * @return  The xdm login.
     */
    public String getXdmLogin() {
        return (java.lang.String) null;
    }

    /**
     * Sets the xdm login.
     *  
     * @param arg1 The xdm login.
     */
    public void setXdmLogin(String arg1) {

    }

    /**
     * Returns the xdm password.
     *  
     * @return  The xdm password.
     */
    public String getXdmPassword() {
        return (java.lang.String) null;
    }

    /**
     * Sets the xdm password.
     *  
     * @param arg1 The xdm password.
     */
    public void setXdmPassword(String arg1) {

    }

    /**
     * Returns the im conference uri.
     *  
     * @return  The im conference uri.
     */
    public String getImConferenceUri() {
        return (java.lang.String) null;
    }

    /**
     * Sets the im conference uri.
     *  
     * @param arg1 The im conference uri.
     */
    public void setImConferenceUri(String arg1) {

    }

    /**
     * Returns the end user confirmation request uri.
     *  
     * @return  The end user confirmation request uri.
     */
    public String getEndUserConfirmationRequestUri() {
        return (java.lang.String) null;
    }

    /**
     * Sets the end user confirmation request uri.
     *  
     * @param arg1 The end user confirmation request uri.
     */
    public void setEndUserConfirmationRequestUri(String arg1) {

    }

    /**
     * Returns the country code.
     *  
     * @return  The country code.
     */
    public String getCountryCode() {
        return (java.lang.String) null;
    }

    /**
     * Sets the country code.
     *  
     * @param arg1 The country code.
     */
    public void setCountryCode(String arg1) {

    }

    /**
     * Returns the country area code.
     *  
     * @return  The country area code.
     */
    public String getCountryAreaCode() {
        return (java.lang.String) null;
    }

    /**
     * Sets the country area code.
     *  
     * @param arg1 The country area code.
     */
    public void setCountryAreaCode(String arg1) {

    }

    /**
     * Returns the my capabilities.
     *  
     * @return  The my capabilities.
     */
    public Capabilities getMyCapabilities() {
        return (org.gsma.rcs.capability.Capabilities) null;
    }

    /**
     * Returns the supported rcs extensions.
     *  
     * @return  The supported rcs extensions.
     */
    public String getSupportedRcsExtensions() {
        return (java.lang.String) null;
    }

    /**
     * Returns the max photo icon size.
     *  
     * @return  The max photo icon size.
     */
    public int getMaxPhotoIconSize() {
        return 0;
    }

    /**
     * Returns the max freetext length.
     *  
     * @return  The max freetext length.
     */
    public int getMaxFreetextLength() {
        return 0;
    }

    /**
     * Returns the max chat participants.
     *  
     * @return  The max chat participants.
     */
    public int getMaxChatParticipants() {
        return 0;
    }

    /**
     * Returns the max chat message length.
     *  
     * @return  The max chat message length.
     */
    public int getMaxChatMessageLength() {
        return 0;
    }

    /**
     * Returns the chat idle duration.
     *  
     * @return  The chat idle duration.
     */
    public int getChatIdleDuration() {
        return 0;
    }

    /**
     * Returns the max file transfer size.
     *  
     * @return  The max file transfer size.
     */
    public int getMaxFileTransferSize() {
        return 0;
    }

    /**
     * Returns the warning max file transfer size.
     *  
     * @return  The warning max file transfer size.
     */
    public int getWarningMaxFileTransferSize() {
        return 0;
    }

    /**
     * Returns the max image sharing size.
     *  
     * @return  The max image sharing size.
     */
    public int getMaxImageSharingSize() {
        return 0;
    }

    /**
     * Returns the max video share duration.
     *  
     * @return  The max video share duration.
     */
    public int getMaxVideoShareDuration() {
        return 0;
    }

    /**
     * Returns the max chat sessions.
     *  
     * @return  The max chat sessions.
     */
    public int getMaxChatSessions() {
        return 0;
    }

    /**
     * Returns the max file transfer sessions.
     *  
     * @return  The max file transfer sessions.
     */
    public int getMaxFileTransferSessions() {
        return 0;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isSmsFallbackServiceActivated() {
        return false;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isChatAutoAccepted() {
        return false;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isGroupChatAutoAccepted() {
        return false;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isFileTransferAutoAccepted() {
        return false;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isStoreForwardWarningActivated() {
        return false;
    }

    /**
     * Returns the im session start mode.
     *  
     * @return  The im session start mode.
     */
    public int getImSessionStartMode() {
        return 0;
    }

    /**
     * Returns the max chat log entries per contact.
     *  
     * @return  The max chat log entries per contact.
     */
    public int getMaxChatLogEntriesPerContact() {
        return 0;
    }

    /**
     * Returns the ims service polling period.
     *  
     * @return  The ims service polling period.
     */
    public int getImsServicePollingPeriod() {
        return 0;
    }

    /**
     * Returns the sip default protocol for mobile.
     *  
     * @return  The sip default protocol for mobile.
     */
    public String getSipDefaultProtocolForMobile() {
        return (java.lang.String) null;
    }

    /**
     * Returns the sip default protocol for wifi.
     *  
     * @return  The sip default protocol for wifi.
     */
    public String getSipDefaultProtocolForWifi() {
        return (java.lang.String) null;
    }

    /**
     * Returns the tls certificate root.
     *  
     * @return  The tls certificate root.
     */
    public String getTlsCertificateRoot() {
        return (java.lang.String) null;
    }

    /**
     * Returns the tls certificate intermediate.
     *  
     * @return  The tls certificate intermediate.
     */
    public String getTlsCertificateIntermediate() {
        return (java.lang.String) null;
    }

    /**
     * Returns the sip transaction timeout.
     *  
     * @return  The sip transaction timeout.
     */
    public int getSipTransactionTimeout() {
        return 0;
    }

    /**
     * Returns the msrp transaction timeout.
     *  
     * @return  The msrp transaction timeout.
     */
    public int getMsrpTransactionTimeout() {
        return 0;
    }

    /**
     * Returns the register expire period.
     *  
     * @return  The register expire period.
     */
    public int getRegisterExpirePeriod() {
        return 0;
    }

    /**
     * Returns the register retry base time.
     *  
     * @return  The register retry base time.
     */
    public int getRegisterRetryBaseTime() {
        return 0;
    }

    /**
     * Returns the register retry max time.
     *  
     * @return  The register retry max time.
     */
    public int getRegisterRetryMaxTime() {
        return 0;
    }

    /**
     * Returns the publish expire period.
     *  
     * @return  The publish expire period.
     */
    public int getPublishExpirePeriod() {
        return 0;
    }

    /**
     * Returns the revoke timeout.
     *  
     * @return  The revoke timeout.
     */
    public int getRevokeTimeout() {
        return 0;
    }

    /**
     * Returns the ims auhtentication procedure for mobile.
     *  
     * @return  The ims auhtentication procedure for mobile.
     */
    public String getImsAuhtenticationProcedureForMobile() {
        return (java.lang.String) null;
    }

    /**
     * Returns the ims auhtentication procedure for wifi.
     *  
     * @return  The ims auhtentication procedure for wifi.
     */
    public String getImsAuhtenticationProcedureForWifi() {
        return (java.lang.String) null;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isTelUriFormatUsed() {
        return false;
    }

    /**
     * Returns the ringing period.
     *  
     * @return  The ringing period.
     */
    public int getRingingPeriod() {
        return 0;
    }

    /**
     * Returns the subscribe expire period.
     *  
     * @return  The subscribe expire period.
     */
    public int getSubscribeExpirePeriod() {
        return 0;
    }

    /**
     * Returns the is composing timeout.
     *  
     * @return  The is composing timeout.
     */
    public int getIsComposingTimeout() {
        return 0;
    }

    /**
     * Returns the session refresh expire period.
     *  
     * @return  The session refresh expire period.
     */
    public int getSessionRefreshExpirePeriod() {
        return 0;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isPermanentStateModeActivated() {
        return false;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isTraceActivated() {
        return false;
    }

    /**
     * Returns the trace level.
     *  
     * @return  The trace level.
     */
    public String getTraceLevel() {
        return (java.lang.String) null;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isSipTraceActivated() {
        return false;
    }

    /**
     * Returns the sip trace file.
     *  
     * @return  The sip trace file.
     */
    public String getSipTraceFile() {
        return (java.lang.String) null;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isMediaTraceActivated() {
        return false;
    }

    /**
     * Returns the capability refresh timeout.
     *  
     * @return  The capability refresh timeout.
     */
    public int getCapabilityRefreshTimeout() {
        return 0;
    }

    /**
     * Returns the capability expiry timeout.
     *  
     * @return  The capability expiry timeout.
     */
    public int getCapabilityExpiryTimeout() {
        return 0;
    }

    /**
     * Returns the capability polling period.
     *  
     * @return  The capability polling period.
     */
    public int getCapabilityPollingPeriod() {
        return 0;
    }

    /**
     * Sets the supported rcs extensions.
     *  
     * @param arg1 The supported rcs extensions.
     */
    public void setSupportedRcsExtensions(String arg1) {

    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isImAlwaysOn() {
        return false;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isImReportsActivated() {
        return false;
    }

    /**
     * Returns the network access.
     *  
     * @return  The network access.
     */
    public int getNetworkAccess() {
        return 0;
    }

    /**
     * Returns the sip timer t1.
     *  
     * @return  The sip timer t1.
     */
    public int getSipTimerT1() {
        return 0;
    }

    /**
     * Returns the sip timer t2.
     *  
     * @return  The sip timer t2.
     */
    public int getSipTimerT2() {
        return 0;
    }

    /**
     * Returns the sip timer t4.
     *  
     * @return  The sip timer t4.
     */
    public int getSipTimerT4() {
        return 0;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isSipKeepAliveEnabled() {
        return false;
    }

    /**
     * Returns the sip keep alive period.
     *  
     * @return  The sip keep alive period.
     */
    public int getSipKeepAlivePeriod() {
        return 0;
    }

    /**
     * Returns the network apn.
     *  
     * @return  The network apn.
     */
    public String getNetworkApn() {
        return (java.lang.String) null;
    }

    /**
     * Returns the network operator.
     *  
     * @return  The network operator.
     */
    public String getNetworkOperator() {
        return (java.lang.String) null;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isGruuSupported() {
        return false;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isCpuAlwaysOn() {
        return false;
    }

    /**
     * Returns the auto config mode.
     *  
     * @return  The auto config mode.
     */
    public int getAutoConfigMode() {
        return 0;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isProvisioningTermsAccepted() {
        return false;
    }

    /**
     * Returns the provisioning version.
     *  
     * @return  The provisioning version.
     */
    public String getProvisioningVersion() {
        return (java.lang.String) null;
    }

    /**
     * Sets the provisioning version.
     *  
     * @param arg1 The provisioning version.
     */
    public void setProvisioningVersion(String arg1) {

    }

    /**
     * Sets the provisioning terms accepted.
     *  
     * @param arg1 The provisioning terms accepted.
     */
    public void setProvisioningTermsAccepted(boolean arg1) {

    }

    /**
     * Returns the provisioning address.
     *  
     * @return  The provisioning address.
     */
    public String getProvisioningAddress() {
        return (java.lang.String) null;
    }

    public void resetUserProfile() {

    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isUserProfileConfigured() {
        return false;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isGroupChatActivated() {
        return false;
    }

    /**
     *  
     * @param arg1 The arg1.
     */
    public void backupAccountSettings(String arg1) {

    }

    /**
     *  
     * @param arg1 The arg1.
     */
    public void restoreAccountSettings(String arg1) {

    }

    /**
     * Returns the photo root directory.
     *  
     * @return  The photo root directory.
     */
    public String getPhotoRootDirectory() {
        return (java.lang.String) null;
    }

    /**
     * Sets the photo root directory.
     *  
     * @param arg1 The photo root directory.
     */
    public void setPhotoRootDirectory(String arg1) {

    }

    /**
     * Returns the video root directory.
     *  
     * @return  The video root directory.
     */
    public String getVideoRootDirectory() {
        return (java.lang.String) null;
    }

    /**
     * Sets the video root directory.
     *  
     * @param arg1 The video root directory.
     */
    public void setVideoRootDirectory(String arg1) {

    }

    /**
     * Returns the file root directory.
     *  
     * @return  The file root directory.
     */
    public String getFileRootDirectory() {
        return (java.lang.String) null;
    }

    /**
     * Sets the file root directory.
     *  
     * @param arg1 The file root directory.
     */
    public void setFileRootDirectory(String arg1) {

    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isSecureMsrpOverWifi() {
        return false;
    }

    /**
     *  
     * @return  The boolean.
     */
    public boolean isSecureRtpOverWifi() {
        return false;
    }

    /**
     * Returns the instance.
     *  
     * @return  The instance.
     */
    public static RcsSettings getInstance() {
        return (RcsSettings) null;
    }

    /**
     * Creates the instance.
     *  
     * @param arg1 The arg1.
     */
    public static synchronized void createInstance(Context arg1) {

    }

} // end RcsSettings
