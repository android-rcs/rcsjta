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

package com.gsma.rcs.core.ims.service.terms;

import com.gsma.rcs.core.CoreException;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.SessionAuthenticationAgent;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.StringUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.util.Locale;

/**
 * Terms & conditions service via SIP
 * 
 * @author jexa7410
 */
public class TermsConditionsService extends ImsService {
    /**
     * Request MIME type
     */
    private final static String REQUEST_MIME_TYPE = "application/end-user-confirmation-request+xml";

    /**
     * Ack MIME type
     */
    private final static String ACK_MIME_TYPE = "application/end-user-confirmation-ack+xml";

    /**
     * User notification MIME type
     */
    private final static String USER_NOTIFICATION_MIME_TYPE = "application/end-user-notification-request+xml";

    /**
     * Response MIME type
     */
    private final static String RESPONSE_MIME_TYPE = "application/end-user-confirmation-response+xml";

    /**
     * Accept response
     */
    private final static String ACCEPT_RESPONSE = "accept";

    /**
     * Decline response
     */
    private final static String DECLINE_RESPONSE = "decline";

    private final RcsSettings mRcsSettings;

    private Logger mLogger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @param parent IMS module
     * @param rcsSettings RcsSettings
     * @throws CoreException
     */
    public TermsConditionsService(ImsModule parent, RcsSettings rcsSettings) throws CoreException {
        super(parent, true);
        mRcsSettings = rcsSettings;
    }

    /**
     * Start the IMS service
     */
    public synchronized void start() {
        if (isServiceStarted()) {
            // Already started
            return;
        }
        setServiceStarted(true);
    }

    /**
     * Stop the IMS service
     */
    public synchronized void stop() {
        if (!isServiceStarted()) {
            // Already stopped
            return;
        }
        setServiceStarted(false);
    }

    /**
     * Check the IMS service
     */
    public void check() {
    }

    /**
     * Receive a SIP message
     * 
     * @param message Received message
     */
    public void receiveMessage(SipRequest message) {
        boolean logActivated = mLogger.isActivated();
        if (logActivated) {
            mLogger.debug("Receive terms message");
        }

        // Send a 200 OK response
        try {
            if (logActivated) {
                mLogger.info("Send 200 OK");
            }
            SipResponse response = SipMessageFactory.createResponse(message,
                    IdGenerator.getIdentifier(), 200);
            getImsModule().getSipManager().sendSipResponse(response);
        } catch (Exception e) {
            if (logActivated) {
                mLogger.error("Can't send 200 OK response", e);
            }
            return;
        }

        // Parse received message
        try {
            String lang = Locale.getDefault().getLanguage();

            String remoteId = getRemoteIdentity(message);
            PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(remoteId);
            if (number == null) {
                if (logActivated) {
                    mLogger.error("Can't parse contact" + remoteId);
                }
                return;
            }
            ContactId contact = ContactUtil.createContactIdFromValidatedData(number);
            if (REQUEST_MIME_TYPE.equals(message.getContentType())) {
                // Parse terms request
                InputSource input = new InputSource(new ByteArrayInputStream(
                        message.getContentBytes()));
                TermsRequestParser parser = new TermsRequestParser(input, lang, mRcsSettings);

                // Notify listener
                getImsModule()
                        .getCore()
                        .getListener()
                        .handleUserConfirmationRequest(contact, parser.getId(), parser.getType(),
                                parser.getPin(), parser.getSubject(), parser.getText(),
                                parser.getButtonAccept(), parser.getButtonReject(),
                                parser.getTimeout());

            } else if (ACK_MIME_TYPE.equals(message.getContentType())) {
                // Parse terms ack
                InputSource input = new InputSource(new ByteArrayInputStream(
                        message.getContentBytes()));
                TermsAckParser parser = new TermsAckParser(input);

                // Notify listener
                getImsModule()
                        .getCore()
                        .getListener()
                        .handleUserConfirmationAck(contact, parser.getId(), parser.getStatus(),
                                parser.getSubject(), parser.getText());

            } else if (USER_NOTIFICATION_MIME_TYPE.equals(message.getContentType())) {
                // Parse terms notification
                InputSource input = new InputSource(new ByteArrayInputStream(
                        message.getContentBytes()));
                EndUserNotificationParser parser = new EndUserNotificationParser(input, lang);

                // Notify listener
                getImsModule()
                        .getCore()
                        .getListener()
                        .handleUserNotification(contact, parser.getId(), parser.getSubject(),
                                parser.getText(), parser.getButtonOk());
            } else {
                if (logActivated) {
                    mLogger.warn("Unknown terms request " + message.getContentType());
                }
            }
        } catch (Exception e) {
            if (logActivated) {
                mLogger.error("Can't parse terms request", e);
            }
        }
    }

    /**
     * Accept terms
     * 
     * @param id Request ID
     * @param pin Response value
     * @return Boolean result
     */
    public boolean acceptTerms(String id, String pin) {
        if (mLogger.isActivated()) {
            mLogger.debug("Send response for request " + id);
        }

        // Send SIP MESSAGE
        return sendSipMessage(mRcsSettings.getEndUserConfirmationRequestUri(), id, ACCEPT_RESPONSE,
                pin);
    }

    /**
     * Reject terms
     * 
     * @param id Request ID
     * @param pin Response value
     * @return Boolean result
     */
    public boolean rejectTerms(String id, String pin) {
        if (mLogger.isActivated()) {
            mLogger.debug("Send response for request " + id);
        }

        // Send SIP MESSAGE
        return sendSipMessage(mRcsSettings.getEndUserConfirmationRequestUri(), id,
                DECLINE_RESPONSE, pin);
    }

    /**
     * Send SIP MESSAGE
     * 
     * @param remote Remote server
     * @param id Request ID
     * @param value Response value
     * @param pin Response value
     * @return Boolean result
     */
    private boolean sendSipMessage(String remote, String id, String value, String pin) {
        if (StringUtils.isEmpty(remote)) {
            if (mLogger.isActivated()) {
                mLogger.error("Remote URI not set");
            }
            return false;
        }

        if (StringUtils.isEmpty(id)) {
            if (mLogger.isActivated()) {
                mLogger.error("Request ID not set");
            }
            return false;
        }

        boolean result = false;
        try {
            if (mLogger.isActivated()) {
                mLogger.debug("Send SIP response");
            }

            // Build response
            String response = "<?xml version=\"1.0\" standalone=\"yes\"?>"
                    + "<EndUserConfirmationResponse id=\"" + id + "\" value=\"" + value + "\"";
            if (pin != null) {
                response += " pin=\"";
            }
            response += "/>";

            // Create authentication agent
            SessionAuthenticationAgent authenticationAgent = new SessionAuthenticationAgent(
                    getImsModule());

            // Create a dialog path
            SipDialogPath dialogPath = new SipDialogPath(getImsModule().getSipManager()
                    .getSipStack(), getImsModule().getSipManager().getSipStack().generateCallId(),
                    1, remote, ImsModule.IMS_USER_PROFILE.getPublicUri(), remote, getImsModule()
                            .getSipManager().getSipStack().getServiceRoutePath(), mRcsSettings);

            // Create MESSAGE request
            if (mLogger.isActivated()) {
                mLogger.info("Send first MESSAGE");
            }
            SipRequest msg = SipMessageFactory.createMessage(dialogPath, RESPONSE_MIME_TYPE,
                    response);

            // Send MESSAGE request
            SipTransactionContext ctx = getImsModule().getSipManager().sendSipMessageAndWait(msg);

            // Analyze received message
            if (ctx.getStatusCode() == 407) {
                // 407 response received
                if (mLogger.isActivated()) {
                    mLogger.info("407 response received");
                }

                // Set the Proxy-Authorization header
                authenticationAgent.readProxyAuthenticateHeader(ctx.getSipResponse());

                // Increment the Cseq number of the dialog path
                dialogPath.incrementCseq();

                // Create a second MESSAGE request with the right token
                if (mLogger.isActivated()) {
                    mLogger.info("Send second MESSAGE");
                }
                msg = SipMessageFactory.createMessage(dialogPath, RESPONSE_MIME_TYPE, response);

                // Set the Authorization header
                authenticationAgent.setProxyAuthorizationHeader(msg);

                // Send MESSAGE request
                ctx = getImsModule().getSipManager().sendSipMessageAndWait(msg);

                // Analyze received message
                if ((ctx.getStatusCode() == 200) || (ctx.getStatusCode() == 202)) {
                    // 200 OK response
                    if (mLogger.isActivated()) {
                        mLogger.info("20x OK response received");
                    }
                    result = true;
                } else {
                    // Error
                    if (mLogger.isActivated()) {
                        mLogger.info("Delivery report has failed: " + ctx.getStatusCode()
                                + " response received");
                    }
                }
            } else if ((ctx.getStatusCode() == 200) || (ctx.getStatusCode() == 202)) {
                // 200 OK received
                if (mLogger.isActivated()) {
                    mLogger.info("20x OK response received");
                }
                result = true;
            } else {
                // Error responses
                if (mLogger.isActivated()) {
                    mLogger.info("Delivery report has failed: " + ctx.getStatusCode()
                            + " response received");
                }
            }
        } catch (Exception e) {
            if (mLogger.isActivated()) {
                mLogger.error("Can't send MESSAGE request", e);
            }
        }
        return result;
    }

    /**
     * Get remote identity of the incoming request
     * 
     * @param request Request
     * @return ID
     */
    private String getRemoteIdentity(SipRequest request) {
        // Use the Asserted-Identity header
        return SipUtils.getAssertedIdentity(request);
    }

    /**
     * Is a terms & conditions request
     * 
     * @param request Request
     * @return Boolean
     */
    public static boolean isTermsRequest(SipRequest request) {
        String contentType = request.getContentType();
        return (contentType != null && contentType.startsWith("application/end-user"));
    }
}
