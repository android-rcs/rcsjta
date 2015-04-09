/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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

package com.gsma.rcs.core.ims.network.gsm;

import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Call manager. Note: for outgoing call the capability request is initiated only when we receive
 * the OPTIONS from the remote because the call state goes directly to CONNETED even if the remote
 * has not ringing. For the incoming call, the capability are requested when phone is ringing.
 * 
 * @author jexa7410
 */
public class CallManager {

    private enum State {
        /**
         * Call state unknown
         */
        UNKNOWN,
        /**
         * Call state ringing
         */
        RINGING,
        /**
         * Call state connected
         */
        CONNECTED,
        /**
         * Call state disconnected
         */
        DISCONNECTED;
    }

    private final ImsModule mImsModule;

    private State callState = State.UNKNOWN;

    /**
     * Remote contact
     */
    private static ContactId sContact;

    private boolean mMultipartyCall = false;

    private boolean mCallHold = false;

    private final TelephonyManager mPhonyManager;

    /**
     * Receiver for outgoing calls
     */
    private BroadcastReceiver mOutgoingCallReceiver;

    /**
     * The context this manager is part of
     */
    private Context mContext;

    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(CallManager.class.getSimpleName());

    /**
     * Constructor
     * 
     * @param parent The ImsModule this manager is part of
     * @param context The Context this manager is part of
     */
    public CallManager(ImsModule parent, Context context) {
        mImsModule = parent;
        mContext = context;

        // Instantiate the telephony manager
        mPhonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        mOutgoingCallReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                PhoneNumber number = ContactUtil.getValidPhoneNumberFromAndroid(intent
                        .getStringExtra(Intent.EXTRA_PHONE_NUMBER));
                if (number != null) {
                    setRemoteParty(ContactUtil.createContactIdFromValidatedData(number));
                }
            }
        };
    }

    /**
     * Start call monitoring
     */
    public void startCallMonitoring() {
        if (sLogger.isActivated()) {
            sLogger.info("Start call monitoring");
        }
        /* Monitor phone state */
        mPhonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE);

        mContext.registerReceiver(mOutgoingCallReceiver, new IntentFilter(
                Intent.ACTION_NEW_OUTGOING_CALL));
    }

    /**
     * Stop call monitoring
     */
    public void stopCallMonitoring() {
        if (sLogger.isActivated()) {
            sLogger.info("Stop call monitoring");
        }

        mContext.unregisterReceiver(mOutgoingCallReceiver);

        // Unmonitor phone state
        mPhonyManager.listen(listener, PhoneStateListener.LISTEN_NONE);
    }

    /**
     * Phone state listener
     */
    private PhoneStateListener listener = new PhoneStateListener() {
        public void onCallStateChanged(int state, String incomingNumber) {
            boolean logActivated = sLogger.isActivated();
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    if (State.CONNECTED == callState) {
                        // Tentative of multiparty call
                        return;
                    }
                    PhoneNumber number = ContactUtil.getValidPhoneNumberFromAndroid(incomingNumber);
                    if (number == null) {
                        if (logActivated) {
                            sLogger.info(new StringBuilder("Invalid phone number '")
                                    .append(incomingNumber).append("' is RINGING").toString());
                        }
                        return;
                    }
                    // Set remote party
                    if (logActivated) {
                        sLogger.debug("Call is RINGING: incoming number=".concat(incomingNumber));
                    }
                    sContact = ContactUtil.createContactIdFromValidatedData(number);
                    // Phone is ringing: this state is only used for incoming call
                    callState = State.RINGING;
                    break;

                case TelephonyManager.CALL_STATE_IDLE:
                    if (logActivated) {
                        sLogger.debug("Call is IDLE: last number=" + sContact);
                    }

                    // No more call in progress
                    callState = State.DISCONNECTED;
                    mMultipartyCall = false;
                    mCallHold = false;

                    /* Terminate richcall sessions */
                    mImsModule.getRichcallService().terminateAllSessions();

                    if (sContact == null) {
                        return;
                    }
                    // Disable content sharing capabilities
                    mImsModule.getCapabilityService().resetContactCapabilitiesForContentSharing(
                            sContact);

                    // Request capabilities to the remote
                    mImsModule.getCapabilityService().requestContactCapabilities(sContact);

                    // Reset remote party
                    sContact = null;
                    break;

                case TelephonyManager.CALL_STATE_OFFHOOK:
                    if (State.CONNECTED == callState) {
                        // Request capabilities only if not a multiparty call or call hold
                        if (logActivated) {
                            sLogger.debug("Multiparty call established");
                        }
                        return;
                    }

                    if (logActivated) {
                        sLogger.debug("Call is CONNECTED: connected number=" + sContact);
                    }

                    // Both parties are connected
                    callState = State.CONNECTED;

                    // Delay option request 2 seconds according to implementation guideline ID_4_20
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            // Request capabilities
                            requestCapabilities(sContact);
                        }
                    }, 2000);
                    break;

                default:
                    if (logActivated) {
                        sLogger.debug("Unknown call state ".concat(Integer.toString(state)));
                    }
                    break;
            }
        }
    };

    /**
     * Set the remote contact
     * 
     * @param contact
     */
    public static void setRemoteParty(ContactId contact) {
        sContact = contact;
    }

    /**
     * Get the remote connected phone number
     * 
     * @return Phone number or null is disconnected
     */
    private ContactId getPhoneNumberOfConntectedRemote() {
        if (State.DISCONNECTED == callState) {
            return null;
        }
        return sContact;
    }

    /**
     * Returns the calling remote contact identifier
     * 
     * @return MSISDN
     */
    public ContactId getContact() {
        return sContact;
    }

    /**
     * Is call connected
     * 
     * @return Boolean
     */
    public boolean isCallConnected() {
        return (State.CONNECTED == callState);
    }

    /**
     * Is call connected with a given contact
     * 
     * @param contact Contact identifier
     * @return Boolean
     */

    public boolean isCallConnectedWith(ContactId contact) {
        if (mMultipartyCall || mCallHold) {
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
        return mMultipartyCall;
    }

    /**
     * Is call hold
     * 
     * @return Boolean
     */
    public boolean isCallHold() {
        return mCallHold;
    }

    /**
     * Request capabilities to a given contact
     * 
     * @param contact Contact identifier
     */
    private void requestCapabilities(ContactId contact) {
        if (mImsModule.getCapabilityService().isServiceStarted()) {
            mImsModule.getCapabilityService().requestContactCapabilities(contact);
        }
    }

    /**
     * Call leg has changed
     */
    private void callLegHasChanged() {
        if (mMultipartyCall | mCallHold) {
            /* Terminate richcall sessions if call hold or multiparty call */
            mImsModule.getRichcallService().terminateAllSessions();
        }

        // Request new capabilities
        requestCapabilities(sContact);
    }

    /**
     * Set multiparty call
     * 
     * @param state State
     */
    public void setMultiPartyCall(boolean state) {
        if (sLogger.isActivated()) {
            sLogger.info("Set multiparty call to " + state);
        }
        mMultipartyCall = state;

        callLegHasChanged();
    }

    /**
     * Set call hold
     * 
     * @param state State
     */
    public void setCallHold(boolean state) {
        if (sLogger.isActivated()) {
            sLogger.info("Set call hold to " + state);
        }
        mCallHold = state;

        callLegHasChanged();
    }

    /**
     * Connection event
     * 
     * @param connected Connection state
     */
    public void connectionEvent(boolean connected) {
        if (sContact == null) {
            return;
        }

        if (connected) {
            if (sLogger.isActivated()) {
                sLogger.info("Connectivity changed: update content sharing capabilities");
            }

            // Update content sharing capabilities
            requestCapabilities(sContact);
        } else {
            if (sLogger.isActivated()) {
                sLogger.info("Connectivity changed: disable content sharing capabilities");
            }

            // Disable content sharing capabilities
            mImsModule.getCapabilityService().resetContactCapabilitiesForContentSharing(sContact);
        }
    }
}
