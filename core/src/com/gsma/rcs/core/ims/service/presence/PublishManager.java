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

package com.gsma.rcs.core.ims.service.presence;

import java.util.Vector;

import javax2.sip.header.ExpiresHeader;
import javax2.sip.header.SIPETagHeader;

import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.gsma.rcs.core.ims.service.SessionAuthenticationAgent;
import com.gsma.rcs.platform.registry.RegistryFactory;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.PeriodicRefresher;
import com.gsma.rcs.utils.logger.Logger;

/**
 * Publish manager for sending current user presence status
 * 
 * @author JM. Auffret
 */
public class PublishManager extends PeriodicRefresher {
    /**
     * Last min expire period (in seconds)
     */
    private static final String REGISTRY_MIN_EXPIRE_PERIOD = "MinPublishExpirePeriod";

    /**
     * Last SIP Etag
     */
    private static final String REGISTRY_SIP_ETAG = "SipEntityTag";

    /**
     * SIP Etag expiration (in milliseconds)
     */
    private static final String REGISTRY_SIP_ETAG_EXPIRATION = "SipETagExpiration";

    /**
     * IMS module
     */
    private ImsModule mImsModule;

    /**
     * Expire period
     */
    private int mExpirePeriod;

    /**
     * Dialog path
     */
    private SipDialogPath mDialogPath;

    /**
     * Entity tag
     */
    private String mEntityTag;

    /**
     * Published flag
     */
    private boolean mPublished = false;

    /**
     * Authentication agent
     */
    private SessionAuthenticationAgent mAuthenticationAgent;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    private final RcsSettings mRcsSettings;

    /**
     * Constructor
     * 
     * @param parent IMS module
     * @param rcsSettings
     */
    public PublishManager(ImsModule parent, RcsSettings rcsSettings) {
        mImsModule = parent;
        mAuthenticationAgent = new SessionAuthenticationAgent(mImsModule);
        mRcsSettings = rcsSettings;

        int defaultExpirePeriod = rcsSettings.getPublishExpirePeriod();
        int minExpireValue = RegistryFactory.getFactory().readInteger(REGISTRY_MIN_EXPIRE_PERIOD,
                -1);
        if ((minExpireValue != -1) && (defaultExpirePeriod < minExpireValue)) {
            mExpirePeriod = minExpireValue;
        } else {
            mExpirePeriod = defaultExpirePeriod;
        }

        // Restore the last SIP-ETag from the registry
        readEntityTag();
    }

    /**
     * Is published
     * 
     * @return Return True if the terminal has published, else return False
     */
    public boolean isPublished() {
        return mPublished;
    }

    /**
     * Terminate manager
     */
    public void terminate() {
        if (logger.isActivated()) {
            logger.info("Terminate the publish manager");
        }

        // Do not unpublish for RCS, just stop timer
        if (mPublished) {
            // Stop timer
            stopTimer();
            mPublished = false;
        }

        if (logger.isActivated()) {
            logger.info("Publish manager is terminated");
        }
    }

    /**
     * Publish refresh processing
     */
    public void periodicProcessing() {
        // Make a publish
        if (logger.isActivated()) {
            logger.info("Execute re-publish");
        }

        try {
            // Create a new dialog path for each publish
            mDialogPath = createDialogPath();

            // Create PUBLISH request with no SDP and expire period
            SipRequest publish = SipMessageFactory.createPublish(createDialogPath(), mExpirePeriod,
                    mEntityTag, null);

            // Send PUBLISH request
            sendPublish(publish);
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Publish has failed", e);
            }
            handleError(new PresenceError(PresenceError.UNEXPECTED_EXCEPTION, e.getMessage()));
        }
    }

    /**
     * Publish presence status
     * 
     * @param info Presence info
     * @return Boolean
     */
    public synchronized boolean publish(String info) {
        try {
            // Create a new dialog path for each publish
            mDialogPath = createDialogPath();

            // Set the local SDP part in the dialog path
            mDialogPath.setLocalContent(info);

            // Create PUBLISH request
            SipRequest publish = SipMessageFactory.createPublish(mDialogPath, mExpirePeriod,
                    mEntityTag, info);

            // Send PUBLISH request
            sendPublish(publish);
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Publish has failed", e);
            }
            handleError(new PresenceError(PresenceError.UNEXPECTED_EXCEPTION, e.getMessage()));
        }
        return mPublished;
    }

    /**
     * Unpublish
     */
    public synchronized void unPublish() {
        if (!mPublished) {
            // Already unpublished
            return;
        }

        try {
            // Stop periodic publish
            stopTimer();

            // Create a new dialog path for each publish
            mDialogPath = createDialogPath();

            // Create PUBLISH request with no SDP and expire period
            SipRequest publish = SipMessageFactory.createPublish(mDialogPath, 0, mEntityTag, null);

            // Send PUBLISH request
            sendPublish(publish);

            // Force publish flag to false
            mPublished = false;
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Publish has failed", e);
            }
            handleError(new PresenceError(PresenceError.UNEXPECTED_EXCEPTION, e.getMessage()));
        }
    }

    /**
     * Send PUBLISH message
     * 
     * @param publish SIP PUBLISH
     * @throws Exception
     */
    private void sendPublish(SipRequest publish) throws Exception {
        if (logger.isActivated()) {
            logger.info("Send PUBLISH, expire=" + publish.getExpires());
        }

        if (mPublished) {
            // Set the Authorization header
            mAuthenticationAgent.setProxyAuthorizationHeader(publish);
        }

        // Send PUBLISH request
        SipTransactionContext ctx = mImsModule.getSipManager().sendSipMessageAndWait(publish);

        // Analyze the received response
        if (ctx.isSipResponse()) {
            // A response has been received
            if (ctx.getStatusCode() == 200) {
                // 200 OK
                if (publish.getExpires() != 0) {
                    handle200OK(ctx);
                } else {
                    handle200OkUnpublish(ctx);
                }
            } else if (ctx.getStatusCode() == 407) {
                // 407 Proxy Authentication Required
                handle407Authentication(ctx);
            } else if (ctx.getStatusCode() == 412) {
                // 412 Error
                handle412ConditionalRequestFailed(ctx);
            } else if (ctx.getStatusCode() == 423) {
                // 423 Interval Too Brief
                handle423IntervalTooBrief(ctx);
            } else {
                // Other error response
                handleError(new PresenceError(PresenceError.PUBLISH_FAILED, ctx.getStatusCode()
                        + " " + ctx.getReasonPhrase()));
            }
        } else {
            if (logger.isActivated()) {
                logger.debug("No response received for PUBLISH");
            }

            // No response received: timeout
            handleError(new PresenceError(PresenceError.PUBLISH_FAILED));
        }
    }

    /**
     * Handle 200 0K response
     * 
     * @param ctx SIP transaction context
     */
    private void handle200OK(SipTransactionContext ctx) {
        // 200 OK response received
        if (logger.isActivated()) {
            logger.info("200 OK response received");
        }
        mPublished = true;

        SipResponse resp = ctx.getSipResponse();

        // Set the Proxy-Authorization header
        mAuthenticationAgent.readProxyAuthenticateHeader(resp);

        // Retrieve the expire value in the response
        retrieveExpirePeriod(resp);

        // Retrieve the entity tag in the response
        saveEntityTag((SIPETagHeader) resp.getHeader(SIPETagHeader.NAME));

        // Start the periodic publish
        startTimer(mExpirePeriod, 0.5);
    }

    /**
     * Handle 200 0K response of UNPUBLISH
     * 
     * @param ctx SIP transaction context
     */
    private void handle200OkUnpublish(SipTransactionContext ctx) {
        // 200 OK response received
        if (logger.isActivated()) {
            logger.info("200 OK response received");
        }

        SipResponse resp = ctx.getSipResponse();

        // Retrieve the entity tag in the response
        saveEntityTag((SIPETagHeader) resp.getHeader(SIPETagHeader.NAME));
    }

    /**
     * Handle 407 response
     * 
     * @param ctx SIP transaction context
     * @throws Exception
     */
    private void handle407Authentication(SipTransactionContext ctx) throws Exception {
        // 407 response received
        if (logger.isActivated()) {
            logger.info("407 response received");
        }

        SipResponse resp = ctx.getSipResponse();

        // Set the Proxy-Authorization header
        mAuthenticationAgent.readProxyAuthenticateHeader(resp);

        // Increment the Cseq number of the dialog path
        mDialogPath.incrementCseq();

        // Create a second PUBLISH request with the right token
        if (logger.isActivated()) {
            logger.info("Send second PUBLISH");
        }
        SipRequest publish = SipMessageFactory.createPublish(mDialogPath, ctx.getTransaction()
                .getRequest().getExpires().getExpires(), mEntityTag, mDialogPath.getLocalContent());

        // Set the Authorization header
        mAuthenticationAgent.setProxyAuthorizationHeader(publish);

        // Send PUBLISH request
        sendPublish(publish);
    }

    /**
     * Handle 412 response
     * 
     * @param ctx SIP transaction context
     */
    private void handle412ConditionalRequestFailed(SipTransactionContext ctx) throws Exception {
        // 412 response received
        if (logger.isActivated()) {
            logger.info("412 conditional response received");
        }

        // Increment the Cseq number of the dialog path
        mDialogPath.incrementCseq();

        // Reset Sip-Etag
        saveEntityTag(null);

        // Create a PUBLISH request without ETag
        SipRequest publish = SipMessageFactory.createPublish(mDialogPath, mExpirePeriod,
                mEntityTag,
                mDialogPath.getLocalContent());

        // Send PUBLISH request
        sendPublish(publish);
    }

    /**
     * Handle 423 response
     * 
     * @param ctx SIP transaction context
     * @throws Exception
     */
    private void handle423IntervalTooBrief(SipTransactionContext ctx) throws Exception {
        // 423 response received
        if (logger.isActivated()) {
            logger.info("423 interval too brief response received");
        }

        SipResponse resp = ctx.getSipResponse();

        // Increment the Cseq number of the dialog path
        mDialogPath.incrementCseq();

        // Extract the Min-Expire value
        int minExpire = SipUtils.getMinExpiresPeriod(resp);
        if (minExpire == -1) {
            if (logger.isActivated()) {
                logger.error("Can't read the Min-Expires value");
            }
            handleError(new PresenceError(PresenceError.PUBLISH_FAILED,
                    "No Min-Expires value found"));
            return;
        }

        // Save the min expire value in the terminal registry
        RegistryFactory.getFactory().writeInteger(REGISTRY_MIN_EXPIRE_PERIOD, minExpire);

        // Set the default expire value
        mExpirePeriod = minExpire;

        // Create a new PUBLISH request with the right expire period
        SipRequest publish = SipMessageFactory.createPublish(mDialogPath, mExpirePeriod,
                mEntityTag,
                mDialogPath.getLocalContent());

        // Send a PUBLISH request
        sendPublish(publish);
    }

    /**
     * Handle error response
     * 
     * @param error Error
     */
    private void handleError(PresenceError error) {
        // Error
        if (logger.isActivated()) {
            logger.info("Publish has failed: " + error.getErrorCode() + ", reason="
                    + error.getMessage());
        }
        mPublished = false;

        // Publish has failed, stop the periodic publish
        stopTimer();

        // Error
        if (logger.isActivated()) {
            logger.info("Publish has failed");
        }
    }

    /**
     * Retrieve the expire period
     * 
     * @param response SIP response
     */
    private void retrieveExpirePeriod(SipResponse response) {
        // Extract expire value from Expires header
        ExpiresHeader expiresHeader = (ExpiresHeader) response.getHeader(ExpiresHeader.NAME);
        if (expiresHeader != null) {
            int expires = expiresHeader.getExpires();
            if (expires != -1) {
                mExpirePeriod = expires;
            }
        }
    }

    /**
     * Save the SIP entity tag
     * 
     * @param etagHeader Header tag
     */
    private void saveEntityTag(SIPETagHeader etagHeader) {
        if (etagHeader == null) {
            mEntityTag = null;
        } else {
            mEntityTag = etagHeader.getETag();
        }
        if (mEntityTag != null) {
            RegistryFactory.getFactory().writeString(REGISTRY_SIP_ETAG, mEntityTag);
            long etagExpiration = System.currentTimeMillis() + (mExpirePeriod * 1000);
            RegistryFactory.getFactory().writeLong(REGISTRY_SIP_ETAG_EXPIRATION, etagExpiration);
            if (logger.isActivated()) {
                logger.debug("New entity tag: " + mEntityTag + ", expire at=" + etagExpiration);
            }
        } else {
            RegistryFactory.getFactory().removeParameter(REGISTRY_SIP_ETAG);
            RegistryFactory.getFactory().removeParameter(REGISTRY_SIP_ETAG_EXPIRATION);
            if (logger.isActivated()) {
                logger.debug("Entity tag has been reset");
            }
        }
    }

    /**
     * Read the SIP entity tag
     */
    private void readEntityTag() {
        mEntityTag = RegistryFactory.getFactory().readString(REGISTRY_SIP_ETAG, null);
        long etagExpiration = RegistryFactory.getFactory().readLong(REGISTRY_SIP_ETAG_EXPIRATION,
                -1);
        if (logger.isActivated()) {
            logger.debug("New entity tag: " + mEntityTag + ", expire at=" + etagExpiration);
        }
    }

    /**
     * Create a new dialog path
     * 
     * @return Dialog path
     */
    private SipDialogPath createDialogPath() {
        // Set Call-Id
        String callId = mImsModule.getSipManager().getSipStack().generateCallId();

        // Set target
        String target = ImsModule.IMS_USER_PROFILE.getPublicUri();

        // Set local party
        String localParty = ImsModule.IMS_USER_PROFILE.getPublicUri();

        // Set remote party
        String remoteParty = ImsModule.IMS_USER_PROFILE.getPublicUri();

        // Set the route path
        Vector<String> route = mImsModule.getSipManager().getSipStack().getServiceRoutePath();

        // Create a dialog path
        SipDialogPath dialog = new SipDialogPath(mImsModule.getSipManager().getSipStack(), callId,
                1, target, localParty, remoteParty, route, mRcsSettings);
        return dialog;
    }
}
