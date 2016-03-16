/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.core.ims.service.presence;

import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.NetworkException;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.gsma.rcs.core.ims.service.SessionAuthenticationAgent;
import com.gsma.rcs.platform.registry.RegistryFactory;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.PeriodicRefresher;
import com.gsma.rcs.utils.logger.Logger;

import java.text.ParseException;
import java.util.Vector;

import javax2.sip.InvalidArgumentException;
import javax2.sip.header.ExpiresHeader;
import javax2.sip.header.SIPETagHeader;
import javax2.sip.message.Response;

/**
 * Publish manager for sending current user presence status
 * 
 * @author JM. Auffret
 */
public class PublishManager extends PeriodicRefresher {
    /**
     * Rate to convert from seconds to milliseconds
     */
    private static final long SECONDS_TO_MILLISECONDS_CONVERSION_RATE = 1000;

    /**
     * Last min expire period (in milliseconds)
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

    private ImsModule mImsModule;

    private long mExpirePeriod;

    private SipDialogPath mDialogPath;

    private String mEntityTag;

    private boolean mPublished = false;

    private SessionAuthenticationAgent mAuthenticationAgent;

    private static final Logger sLogger = Logger.getLogger(PublishManager.class.getName());

    private final RcsSettings mRcsSettings;

    /**
     * Constructor
     * 
     * @param parent IMS module
     * @param rcsSettings
     */
    public PublishManager(ImsModule parent, RcsSettings rcsSettings) {
        mImsModule = parent;
        mRcsSettings = rcsSettings;
    }

    public void initialize() {
        mAuthenticationAgent = new SessionAuthenticationAgent(mImsModule);
        long defaultExpirePeriod = mRcsSettings.getPublishExpirePeriod();
        long minExpireValue = RegistryFactory.getFactory().readLong(REGISTRY_MIN_EXPIRE_PERIOD, -1);
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
        if (sLogger.isActivated()) {
            sLogger.info("Terminate the publish manager");
        }

        // Do not unpublish for RCS, just stop timer
        if (mPublished) {
            // Stop timer
            stopTimer();
            mPublished = false;
        }

        if (sLogger.isActivated()) {
            sLogger.info("Publish manager is terminated");
        }
    }

    /**
     * Publish refresh processing
     * 
     * @throws PayloadException
     * @throws NetworkException
     */
    public void periodicProcessing() throws PayloadException, NetworkException {
        if (sLogger.isActivated()) {
            sLogger.info("Execute re-publish");
        }
        mDialogPath = createDialogPath();
        SipRequest publish = SipMessageFactory.createPublish(createDialogPath(), mExpirePeriod,
                mEntityTag, null);
        sendPublish(publish);
    }

    /**
     * Publish presence status
     * 
     * @param info Presence info
     * @return Boolean
     * @throws PayloadException
     * @throws NetworkException
     */
    public synchronized boolean publish(String info) throws PayloadException,
            NetworkException {
        mDialogPath = createDialogPath();
        mDialogPath.setLocalContent(info);
        SipRequest publish = SipMessageFactory.createPublish(mDialogPath, mExpirePeriod,
                mEntityTag, info);
        sendPublish(publish);
        return mPublished;
    }

    /**
     * Unpublish
     * 
     * @throws PayloadException
     * @throws NetworkException
     */
    public synchronized void unPublish() throws PayloadException, NetworkException {
        if (!mPublished) {
            return;
        }
        stopTimer();
        mDialogPath = createDialogPath();
        SipRequest publish = SipMessageFactory.createPublish(mDialogPath, 0, mEntityTag, null);
        sendPublish(publish);
        mPublished = false;
    }

    /**
     * Send PUBLISH message
     * 
     * @param publish SIP PUBLISH
     * @throws NetworkException
     * @throws PayloadException
     */
    private void sendPublish(SipRequest publish) throws PayloadException, NetworkException {
        try {
            if (sLogger.isActivated()) {
                sLogger.info(new StringBuilder("Send PUBLISH, expire=")
                        .append(publish.getExpires()).append("ms").toString());
            }
            if (mPublished) {
                mAuthenticationAgent.setProxyAuthorizationHeader(publish);
            }
            SipTransactionContext ctx = mImsModule.getSipManager().sendSipMessageAndWait(publish);

            if (ctx.isSipResponse()) {
                final int statusCode = ctx.getStatusCode();
                switch (statusCode) {
                    case Response.OK:
                        if (publish.getExpires() != 0) {
                            handle200OK(ctx);
                        } else {
                            handle200OkUnpublish(ctx);
                        }
                        break;
                    case Response.PROXY_AUTHENTICATION_REQUIRED:
                        handle407Authentication(ctx);
                        break;
                    case Response.CONDITIONAL_REQUEST_FAILED:
                        handle412ConditionalRequestFailed(ctx);
                        break;
                    case Response.INTERVAL_TOO_BRIEF:
                        handle423IntervalTooBrief(ctx);
                        break;
                    default:
                        handleError(new PresenceError(PresenceError.PUBLISH_FAILED,
                                ctx.getStatusCode() + " " + ctx.getReasonPhrase()));
                        break;
                }
            } else {
                if (sLogger.isActivated()) {
                    sLogger.debug("No response received for PUBLISH");
                }
                handleError(new PresenceError(PresenceError.PUBLISH_FAILED));
            }
        } catch (InvalidArgumentException e) {
            throw new PayloadException("Publish has failed!", e);

        } catch (ParseException e) {
            throw new PayloadException("Publish has failed!", e);
        }
    }

    /**
     * Handle 200 0K response
     * 
     * @param ctx SIP transaction context
     */
    private void handle200OK(SipTransactionContext ctx) {
        // 200 OK response received
        if (sLogger.isActivated()) {
            sLogger.info("200 OK response received");
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
        startTimer(System.currentTimeMillis(), mExpirePeriod, 0.5);
    }

    /**
     * Handle 200 0K response of UNPUBLISH
     * 
     * @param ctx SIP transaction context
     */
    private void handle200OkUnpublish(SipTransactionContext ctx) {
        // 200 OK response received
        if (sLogger.isActivated()) {
            sLogger.info("200 OK response received");
        }

        SipResponse resp = ctx.getSipResponse();

        // Retrieve the entity tag in the response
        saveEntityTag((SIPETagHeader) resp.getHeader(SIPETagHeader.NAME));
    }

    /**
     * Handle 407 response
     * 
     * @param ctx SIP transaction context
     * @throws PayloadException
     * @throws NetworkException
     */
    private void handle407Authentication(SipTransactionContext ctx) throws PayloadException,
            NetworkException {
        try {
            // 407 response received
            if (sLogger.isActivated()) {
                sLogger.info("407 response received");
            }

            SipResponse resp = ctx.getSipResponse();

            // Set the Proxy-Authorization header
            mAuthenticationAgent.readProxyAuthenticateHeader(resp);

            // Increment the Cseq number of the dialog path
            mDialogPath.incrementCseq();

            // Create a second PUBLISH request with the right token
            if (sLogger.isActivated()) {
                sLogger.info("Send second PUBLISH");
            }
            SipRequest publish = SipMessageFactory.createPublish(mDialogPath, ctx.getTransaction()
                    .getRequest().getExpires().getExpires()
                    * SECONDS_TO_MILLISECONDS_CONVERSION_RATE, mEntityTag,
                    mDialogPath.getLocalContent());

            // Set the Authorization header
            mAuthenticationAgent.setProxyAuthorizationHeader(publish);

            // Send PUBLISH request
            sendPublish(publish);
        } catch (InvalidArgumentException e) {
            throw new PayloadException("Failed to handle 407 authentication response!", e);

        } catch (ParseException e) {
            throw new PayloadException("Failed to handle 407 authentication response!", e);
        }
    }

    /**
     * Handle 412 response
     * 
     * @param ctx SIP transaction context
     * @throws PayloadException
     * @throws NetworkException
     */
    private void handle412ConditionalRequestFailed(SipTransactionContext ctx)
            throws PayloadException, NetworkException {
        // 412 response received
        if (sLogger.isActivated()) {
            sLogger.info("412 conditional response received");
        }

        // Increment the Cseq number of the dialog path
        mDialogPath.incrementCseq();

        // Reset Sip-Etag
        saveEntityTag(null);

        // Create a PUBLISH request without ETag
        SipRequest publish = SipMessageFactory.createPublish(mDialogPath, mExpirePeriod,
                mEntityTag, mDialogPath.getLocalContent());

        // Send PUBLISH request
        sendPublish(publish);
    }

    /**
     * Handle 423 response
     * 
     * @param ctx SIP transaction context
     * @throws PayloadException
     * @throws NetworkException
     */
    private void handle423IntervalTooBrief(SipTransactionContext ctx) throws PayloadException,
            NetworkException {
        // 423 response received
        if (sLogger.isActivated()) {
            sLogger.info("423 interval too brief response received");
        }

        SipResponse resp = ctx.getSipResponse();

        // Increment the Cseq number of the dialog path
        mDialogPath.incrementCseq();

        // Extract the Min-Expire value
        long minExpire = SipUtils.getMinExpiresPeriod(resp);
        if (minExpire == -1) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't read the Min-Expires value");
            }
            handleError(new PresenceError(PresenceError.PUBLISH_FAILED,
                    "No Min-Expires value found"));
            return;
        }

        // Save the min expire value in the terminal registry
        RegistryFactory.getFactory().writeLong(REGISTRY_MIN_EXPIRE_PERIOD, minExpire);

        // Set the default expire value
        mExpirePeriod = minExpire;

        // Create a new PUBLISH request with the right expire period
        SipRequest publish = SipMessageFactory.createPublish(mDialogPath, mExpirePeriod,
                mEntityTag, mDialogPath.getLocalContent());

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
        if (sLogger.isActivated()) {
            sLogger.info("Publish has failed: " + error.getErrorCode() + ", reason="
                    + error.getMessage());
        }
        mPublished = false;

        // Publish has failed, stop the periodic publish
        stopTimer();

        // Error
        if (sLogger.isActivated()) {
            sLogger.info("Publish has failed");
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
                mExpirePeriod = expires * SECONDS_TO_MILLISECONDS_CONVERSION_RATE;
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
            long etagExpiration = System.currentTimeMillis() + mExpirePeriod;
            RegistryFactory.getFactory().writeLong(REGISTRY_SIP_ETAG_EXPIRATION, etagExpiration);
            if (sLogger.isActivated()) {
                sLogger.debug("New entity tag: " + mEntityTag + ", expire at=" + etagExpiration);
            }
        } else {
            RegistryFactory.getFactory().removeParameter(REGISTRY_SIP_ETAG);
            RegistryFactory.getFactory().removeParameter(REGISTRY_SIP_ETAG_EXPIRATION);
            if (sLogger.isActivated()) {
                sLogger.debug("Entity tag has been reset");
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
        if (sLogger.isActivated()) {
            sLogger.debug("New entity tag: " + mEntityTag + ", expire at=" + etagExpiration);
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
        String target = ImsModule.getImsUserProfile().getPublicUri();

        // Set local party
        String localParty = target;

        // Set remote party
        String remoteParty = target;

        // Set the route path
        Vector<String> route = mImsModule.getSipManager().getSipStack().getServiceRoutePath();

        // Create a dialog path
        SipDialogPath dialog = new SipDialogPath(mImsModule.getSipManager().getSipStack(), callId,
                1, target, localParty, remoteParty, route, mRcsSettings);
        return dialog;
    }
}
