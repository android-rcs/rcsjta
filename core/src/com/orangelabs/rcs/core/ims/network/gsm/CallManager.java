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

package com.orangelabs.rcs.core.ims.network.gsm;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.utils.ContactUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Call manager. Note: for outgoing call the capability request is initiated only when we receive
 * the OPTIONS from the remote because the call state goes directly to CONNETED even if the remote
 * has not ringing. For the incoming call, the capability are requested when phone is ringing.
 * 
 * @author jexa7410
 */
public class CallManager {
    /**
     * Call state unknown
     */
    public final static int UNKNOWN = 0;

    /**
     * Call state ringing
     */
    public final static int RINGING = 1;

    /**
     * Call state connected
     */
    public final static int CONNECTED = 2;

    /**
     * Call state disconnected
     */
    public final static int DISCONNECTED = 3;

    /**
     * IMS module
     */
    private ImsModule imsModule;

    /**
     * Call state
     */
    private int callState = UNKNOWN;

    /**
     * Remote contact
     */
    private static ContactId mContact;

    /**
     * Multiparty call
     */
    private boolean multipartyCall = false;

    /**
     * Call hold
     */
    private boolean callHold = false;

    /**
     * Telephony manager
     */
    private TelephonyManager tm;

    /**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(CallManager.class.getSimpleName());

    /**
     * Constructor
     * 
     * @param core Core
     */
    public CallManager(ImsModule parent) throws CoreException {
        this.imsModule = parent;

        // Instantiate the telephony manager
        tm = (TelephonyManager) AndroidFactory.getApplicationContext().getSystemService(
                Context.TELEPHONY_SERVICE);
    }

    /**
     * Start call monitoring
     */
    public void startCallMonitoring() {
        if (logger.isActivated()) {
            logger.info("Start call monitoring");
        }

        // Monitor phone state
        tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    /**
     * Stop call monitoring
     */
    public void stopCallMonitoring() {
        if (logger.isActivated()) {
            logger.info("Stop call monitoring");
        }

        // Unmonitor phone state
        tm.listen(listener, PhoneStateListener.LISTEN_NONE);
    }

    /**
     * Phone state listener
     */
    private PhoneStateListener listener = new PhoneStateListener() {
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    if (callState == CallManager.CONNECTED) {
                        // Tentative of multiparty call
                        return;
                    }

                    if (logger.isActivated()) {
                        logger.debug("Call is RINGING: incoming number=" + incomingNumber);
                    }

                    // Set remote party
                    try {
                        mContact = ContactUtils.createContactId(incomingNumber);
                        // Phone is ringing: this state is only used for incoming call
                        callState = CallManager.RINGING;
                    } catch (RcsContactFormatException e) {
                        if (logger.isActivated()) {
                            logger.warn("Cannot parse ringning contact");
                        }
                    }

                    break;

                case TelephonyManager.CALL_STATE_IDLE:
                    if (logger.isActivated()) {
                        logger.debug("Call is IDLE: last number=" + mContact);
                    }

                    // No more call in progress
                    callState = CallManager.DISCONNECTED;
                    multipartyCall = false;
                    callHold = false;

                    // Abort pending richcall sessions
                    imsModule.getRichcallService().abortAllSessions();

                    if (mContact != null) {
                        // Disable content sharing capabilities
                        imsModule.getCapabilityService().resetContactCapabilitiesForContentSharing(
                                mContact);

                        // Request capabilities to the remote
                        imsModule.getCapabilityService().requestContactCapabilities(mContact);
                    }

                    // Reset remote party
                    mContact = null;
                    break;

                case TelephonyManager.CALL_STATE_OFFHOOK:
                    if (callState == CallManager.CONNECTED) {
                        // Request capabilities only if not a multiparty call or call hold
                        if (logger.isActivated()) {
                            logger.debug("Multiparty call established");
                        }
                        return;
                    }

                    if (logger.isActivated()) {
                        logger.debug("Call is CONNECTED: connected number=" + mContact);
                    }

                    // Both parties are connected
                    callState = CallManager.CONNECTED;

                    // Delay option request 2 seconds according to implementation guideline ID_4_20
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            // Request capabilities
                            requestCapabilities(mContact);
                        }
                    }, 2000);
                    break;

                default:
                    if (logger.isActivated()) {
                        logger.debug("Unknown call state " + state);
                    }
                    break;
            }
        }
    };

    /**
     * Set the remote phone number
     * 
     * @param number Phone number
     */
    public static void setRemoteParty(String number) {
        try {
            CallManager.mContact = ContactUtils.createContactId(number);
        } catch (RcsContactFormatException e) {
            if (logger.isActivated()) {
                logger.error("Cannot parse remote party " + number);
            }
        }
    }

    /**
     * Get the remote connected phone number
     * 
     * @return Phone number or null is disconnected
     */
    private ContactId getPhoneNumberOfConntectedRemote() {
        if (callState == CallManager.DISCONNECTED) {
            return null;
        } else {
            return mContact;
        }
    }

    /**
     * Returns the calling remote contact identifier
     * 
     * @return MSISDN
     */
    public ContactId getContact() {
        return mContact;
    }

    /**
     * Is call connected
     * 
     * @return Boolean
     */
    public boolean isCallConnected() {
        return (callState == CONNECTED);
    }

    /**
     * Is call connected with a given contact
     * 
     * @param contact Contact identifier
     * @return Boolean
     */

    public boolean isCallConnectedWith(ContactId contact) {
        if (this.multipartyCall || this.callHold) {
            return false;
        }

        return (isCallConnected() && contact != null && contact
                .equals(getPhoneNumberOfConntectedRemote()));
    }

    /**
     * Is a multiparty call
     * 
     * @return Boolean
     */
    public boolean isMultipartyCall() {
        return multipartyCall;
    }

    /**
     * Is call hold
     * 
     * @return Boolean
     */
    public boolean isCallHold() {
        return callHold;
    }

    /**
     * Request capabilities to a given contact
     * 
     * @param contact Contact identifier
     */
    private void requestCapabilities(ContactId contact) {
        if (imsModule.getCapabilityService().isServiceStarted()) {
            imsModule.getCapabilityService().requestContactCapabilities(contact);
        }
    }

    /**
     * Call leg has changed
     */
    private void callLegHasChanged() {
        if (multipartyCall | callHold) {
            // Abort pending richcall sessions if call hold or multiparty call
            imsModule.getRichcallService().abortAllSessions();
        }

        // Request new capabilities
        requestCapabilities(mContact);
    }

    /**
     * Set multiparty call
     * 
     * @param state State
     */
    public void setMultiPartyCall(boolean state) {
        if (logger.isActivated()) {
            logger.info("Set multiparty call to " + state);
        }
        this.multipartyCall = state;

        callLegHasChanged();
    }

    /**
     * Set call hold
     * 
     * @param state State
     */
    public void setCallHold(boolean state) {
        if (logger.isActivated()) {
            logger.info("Set call hold to " + state);
        }
        this.callHold = state;

        callLegHasChanged();
    }

    /**
     * Connection event
     * 
     * @param connected Connection state
     */
    public void connectionEvent(boolean connected) {
        if (mContact == null) {
            return;
        }

        if (connected) {
            if (logger.isActivated()) {
                logger.info("Connectivity changed: update content sharing capabilities");
            }

            // Update content sharing capabilities
            requestCapabilities(mContact);
        } else {
            if (logger.isActivated()) {
                logger.info("Connectivity changed: disable content sharing capabilities");
            }

            // Disable content sharing capabilities
            imsModule.getCapabilityService().resetContactCapabilitiesForContentSharing(mContact);
        }
    }
}
