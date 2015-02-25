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

package com.gsma.rcs.core.ims.protocol.sip;

import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.service.SessionAuthenticationAgent;
import com.gsma.rcs.core.ims.service.SessionTimerManager;
import com.gsma.rcs.platform.registry.RegistryFactory;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.IdGenerator;

import java.util.Vector;

import javax2.sip.Dialog;

/**
 * SIP dialog path. A dialog path corresponds to a SIP session, for example from the INVITE to the
 * BYE.
 * 
 * @author JM. Auffret
 */
public class SipDialogPath {
    /**
     * Last min session expire period key
     */
    private static final String REGISTRY_MIN_SESSION_EXPIRE_PERIOD = "MinSessionExpirePeriod";

    /**
     * SIP stack interface
     */
    private SipInterface mStack;

    /**
     * Call-Id
     */
    private String mCallId;

    /**
     * CSeq number
     */
    private long mCseq = 1;

    /**
     * Local tag
     */
    private String mLocalTag = IdGenerator.getIdentifier();

    /**
     * Remote tag
     */
    private String mRemoteTag;

    /**
     * Target
     */
    private String mTarget;

    /**
     * Local party
     */
    private String mLocalParty;

    /**
     * Remote party
     */
    private String mRemoteParty;

    /**
     * Initial INVITE request
     */
    private SipRequest mInvite;

    /**
     * Local content
     */
    private String mLocalContent;

    /**
     * Remote content
     */
    private String mRemoteContent;

    /**
     * Remote sip instance
     */
    private String mRemoteSipInstance;

    /**
     * Route path
     */
    private Vector<String> mRoute;

    /**
     * Authentication agent
     */
    private SessionAuthenticationAgent mAuthenticationAgent;

    /**
     * Session expire time
     */
    private int mSessionExpireTime;

    /**
     * Flag that indicates if the signalisation is established or not
     */
    private boolean mSigEstablished = false;

    /**
     * Flag that indicates if the session (sig + media) is established or not
     */
    private boolean mSessionEstablished = false;

    /**
     * Flag that indicates if the session has been cancelled by the end-user
     */
    private boolean mSessionCancelled = false;

    /**
     * Flag that indicates if the session has been terminated by the server
     */
    private boolean mSessionTerminated = false;

    /**
     * Session termination reason code
     */
    private int mSessionTerminationReasonCode = -1;

    /**
     * Session termination reason phrase
     */
    private String mSessionTerminationReasonPhrase;

    /**
     * Constructor
     * 
     * @param stack SIP stack interface
     * @param callId Call-Id
     * @param cseq CSeq
     * @param target Target
     * @param localParty Local party
     * @param remoteParty Remote party
     * @param route Route path
     * @param rcsSettings
     */
    public SipDialogPath(SipInterface stack, String callId, long cseq, String target,
            String localParty, String remoteParty, Vector<String> route, RcsSettings rcsSettings) {
        mStack = stack;
        mCallId = callId;
        mCseq = cseq;
        mTarget = SipUtils.extractUriFromAddress(target);
        mLocalParty = localParty;
        mRemoteParty = remoteParty;
        mRoute = route;

        int defaultExpireTime = rcsSettings.getSessionRefreshExpirePeriod();
        int minExpireValue = RegistryFactory.getFactory().readInteger(
                REGISTRY_MIN_SESSION_EXPIRE_PERIOD, -1);
        if ((defaultExpireTime > SessionTimerManager.MIN_EXPIRE_PERIOD) && (minExpireValue != -1)
                && (defaultExpireTime < minExpireValue)) {
            mSessionExpireTime = minExpireValue;
        } else {
            mSessionExpireTime = defaultExpireTime;
        }
    }

    /**
     * Constructor<br>
     * Perform a deep copy of the dialogPath
     * 
     * @param dialogPath
     */
    public SipDialogPath(SipDialogPath dialogPath) {
        mStack = dialogPath.getSipStack();
        mCallId = dialogPath.getCallId();
        mCseq = dialogPath.getCseq();
        mLocalTag = dialogPath.getLocalTag();
        mRemoteTag = dialogPath.getRemoteTag();
        mTarget = dialogPath.getTarget();
        mLocalParty = dialogPath.getLocalParty();
        mRemoteParty = dialogPath.getRemoteParty();
        mInvite = dialogPath.getInvite();
        mLocalContent = dialogPath.getLocalContent();
        mRemoteContent = dialogPath.getRemoteContent();
        mRemoteSipInstance = dialogPath.getRemoteSipInstance();
        mRoute = dialogPath.getRoute();
        mAuthenticationAgent = dialogPath.getAuthenticationAgent();
        mSessionExpireTime = dialogPath.getSessionExpireTime();
        mSigEstablished = dialogPath.isSigEstablished();
        mSessionEstablished = dialogPath.isSessionEstablished();
        mSessionCancelled = dialogPath.isSessionCancelled();
        mSessionTerminated = dialogPath.isSessionTerminated();
        mSessionTerminationReasonCode = dialogPath.getSessionTerminationReasonCode();
        mSessionTerminationReasonPhrase = dialogPath.getSessionTerminationReasonPhrase();
    }

    /**
     * Get the current SIP stack interface
     * 
     * @return SIP stack interface
     */
    public SipInterface getSipStack() {
        return mStack;
    }

    /**
     * Get the target of the dialog path
     * 
     * @return String
     */
    public String getTarget() {
        return mTarget;
    }

    /**
     * Set the target of the dialog path
     * 
     * @param tg Target address
     */
    public void setTarget(String tg) {
        mTarget = tg;
    }

    /**
     * Get the local party of the dialog path
     * 
     * @return String
     */
    public String getLocalParty() {
        return mLocalParty;
    }

    /**
     * Get the remote party of the dialog path
     * 
     * @return String
     */
    public String getRemoteParty() {
        return mRemoteParty;
    }

    /**
     * Get the local tag of the dialog path
     * 
     * @return String
     */
    public String getLocalTag() {
        return mLocalTag;
    }

    /**
     * Get the remote tag of the dialog path
     * 
     * @return String
     */
    public String getRemoteTag() {
        return mRemoteTag;
    }

    /**
     * Set the remote tag of the dialog path
     * 
     * @param tag Remote tag
     */
    public void setRemoteTag(String tag) {
        mRemoteTag = tag;
    }

    /**
     * Get the call-id of the dialog path
     * 
     * @return String
     */
    public String getCallId() {
        return mCallId;
    }

    /**
     * Set the call-id of the dialog path
     * 
     * @param callId
     */
    public void setCallId(String callId) {
        mCallId = callId;
    }

    /**
     * Return the Cseq number of the dialog path
     * 
     * @return Cseq number
     */
    public long getCseq() {
        return mCseq;
    }

    /**
     * Increment the Cseq number of the dialog path
     */
    public void incrementCseq() {
        mCseq++;

        // Increment internal stack CSeq if terminating side (NIST stack issue?)
        Dialog dlg = getStackDialog();
        if ((dlg != null) && dlg.isServer()) {
            dlg.incrementLocalSequenceNumber();
        }
    }

    /**
     * Get the initial INVITE request of the dialog path
     * 
     * @return String
     */
    public SipRequest getInvite() {
        return mInvite;
    }

    /**
     * Set the initial INVITE request of the dialog path
     * 
     * @param invite INVITE request
     */
    public void setInvite(SipRequest invite) {
        mInvite = invite;
    }

    /**
     * Returns the local content
     * 
     * @return String
     */
    public String getLocalContent() {
        return mLocalContent;
    }

    /**
     * Returns the remote content
     * 
     * @return String
     */
    public String getRemoteContent() {
        return mRemoteContent;
    }

    /**
     * Sets the local content
     * 
     * @param local Local content
     */
    public void setLocalContent(String local) {
        mLocalContent = local;
    }

    /**
     * Returns the remote SIP instance ID
     * 
     * @return String
     */
    public String getRemoteSipInstance() {
        return mRemoteSipInstance;
    }

    /**
     * Sets the remote SIP instance ID
     * 
     * @param instanceId SIP instance ID
     */
    public void setRemoteSipInstance(String instanceId) {
        mRemoteSipInstance = instanceId;
    }

    /**
     * Sets the remote content
     * 
     * @param remote Remote content
     */
    public void setRemoteContent(String remote) {
        mRemoteContent = remote;
    }

    /**
     * Returns the route path
     * 
     * @return Vector of string
     */
    public Vector<String> getRoute() {
        return mRoute;
    }

    /**
     * Set the route path
     * 
     * @param route New route path
     */
    public void setRoute(Vector<String> route) {
        mRoute = route;
    }

    /**
     * Is session cancelled
     * 
     * @return Boolean
     */
    public boolean isSessionCancelled() {
        return mSessionCancelled;
    }

    /**
     * The session has been cancelled
     */
    public synchronized void sessionCancelled() {
        mSessionCancelled = true;
    }

    /**
     * Is session established
     * 
     * @return Boolean
     */
    public boolean isSessionEstablished() {
        return mSessionEstablished;
    }

    /**
     * Session is established
     */
    public synchronized void sessionEstablished() {
        mSessionEstablished = true;
    }

    /**
     * Is session terminated
     * 
     * @return Boolean
     */
    public boolean isSessionTerminated() {
        return mSessionTerminated;
    }

    /**
     * Session is terminated
     */
    public synchronized void sessionTerminated() {
        mSessionTerminated = true;
        mSessionTerminationReasonCode = -1;
        mSessionTerminationReasonPhrase = null;
    }

    /**
     * Session is terminated with a specific reason code
     * 
     * @param code Reason code
     * @param phrase Reason phrase
     */
    public synchronized void sessionTerminated(int code, String phrase) {
        mSessionTerminated = true;
        mSessionTerminationReasonCode = code;
        mSessionTerminationReasonPhrase = phrase;
    }

    /**
     * Get session termination reason code
     * 
     * @return Reason code
     */
    public int getSessionTerminationReasonCode() {
        return mSessionTerminationReasonCode;
    }

    /**
     * Get session termination reason phrase
     * 
     * @return Reason phrase
     */
    public String getSessionTerminationReasonPhrase() {
        return mSessionTerminationReasonPhrase;
    }

    /**
     * Is signalisation established with success
     * 
     * @return Boolean
     */
    public boolean isSigEstablished() {
        return mSigEstablished;
    }

    /**
     * Signalisation is established with success
     */
    public synchronized void sigEstablished() {
        mSigEstablished = true;
    }

    /**
     * Set the session authentication agent
     * 
     * @param agent Authentication agent
     */
    public void setAuthenticationAgent(SessionAuthenticationAgent agent) {
        mAuthenticationAgent = agent;
    }

    /**
     * Returns the session authentication agent
     * 
     * @return Authentication agent
     */
    public SessionAuthenticationAgent getAuthenticationAgent() {
        return mAuthenticationAgent;
    }

    /**
     * Returns the session expire value
     * 
     * @return Session expire time in seconds
     */
    public int getSessionExpireTime() {
        return mSessionExpireTime;
    }

    /**
     * Set the session expire value
     * 
     * @param sessionExpireTime Session expire time in seconds
     */
    public void setSessionExpireTime(int sessionExpireTime) {
        mSessionExpireTime = sessionExpireTime;
    }

    /**
     * Set the min session expire value
     * 
     * @param sessionExpireTime Session expire time in seconds
     */
    public void setMinSessionExpireTime(int sessionExpireTime) {
        RegistryFactory.getFactory().writeInteger(REGISTRY_MIN_SESSION_EXPIRE_PERIOD,
                sessionExpireTime);
    }

    /**
     * Get stack dialog
     * 
     * @return Dialog or null
     */
    public Dialog getStackDialog() {
        if (mInvite != null) {
            return mInvite.getStackTransaction().getDialog();
        } else {
            return null;
        }
    }
}
