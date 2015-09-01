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

import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.sip.SipManager;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.protocol.sip.SipInterface;
import com.gsma.rcs.core.ims.protocol.sip.SipNetworkException;
import com.gsma.rcs.core.ims.protocol.sip.SipPayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.SessionAuthenticationAgent;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.ContactUtil.PhoneNumber;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;
import android.text.TextUtils;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;

import javax2.sip.InvalidArgumentException;
import javax2.sip.message.Response;

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

    private static final Logger sLogger = Logger.getLogger(TermsConditionsService.class.getName());

    /**
     * Constructor
     * 
     * @param parent IMS module
     * @param rcsSettings RcsSettings
     */
    public TermsConditionsService(ImsModule parent, RcsSettings rcsSettings) {
        super(parent, true);
        mRcsSettings = rcsSettings;
    }

    /**
     * Start the IMS service
     */
    public synchronized void start() {
        if (isServiceStarted()) {
            return;
        }
        setServiceStarted(true);
    }

    /**
     * Stop the IMS service
     */
    public synchronized void stop() {
        if (!isServiceStarted()) {
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
     * @throws SipPayloadException
     * @throws SipNetworkException
     */
    public void receiveMessage(SipRequest message) throws SipPayloadException, SipNetworkException {
        boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.debug("Receive terms message");
        }
        try {
            if (logActivated) {
                sLogger.info("Send 200 OK");
            }
            SipResponse response = SipMessageFactory.createResponse(message,
                    IdGenerator.getIdentifier(), Response.OK);
            final ImsModule imsModule = getImsModule();
            imsModule.getSipManager().sendSipResponse(response);

            String lang = Locale.getDefault().getLanguage();

            String remoteId = getRemoteIdentity(message);
            PhoneNumber number = ContactUtil.getValidPhoneNumberFromUri(remoteId);
            if (number == null) {
                if (logActivated) {
                    sLogger.error("Can't parse contact : ".concat(remoteId));
                }
                return;
            }
            ContactId contact = ContactUtil.createContactIdFromValidatedData(number);
            final String contentType = message.getContentType();
            if (REQUEST_MIME_TYPE.equals(contentType)) {
                InputSource input = new InputSource(new ByteArrayInputStream(
                        message.getContentBytes()));
                TermsRequestParser parser = new TermsRequestParser(input, lang, mRcsSettings);
                imsModule
                        .getCore()
                        .getListener()
                        .handleUserConfirmationRequest(contact, parser.getId(), parser.getType(),
                                parser.getPin(), parser.getSubject(), parser.getText(),
                                parser.getButtonAccept(), parser.getButtonReject(),
                                parser.getTimeout());

            } else if (ACK_MIME_TYPE.equals(contentType)) {
                InputSource input = new InputSource(new ByteArrayInputStream(
                        message.getContentBytes()));
                TermsAckParser parser = new TermsAckParser(input);
                imsModule
                        .getCore()
                        .getListener()
                        .handleUserConfirmationAck(contact, parser.getId(), parser.getStatus(),
                                parser.getSubject(), parser.getText());

            } else if (USER_NOTIFICATION_MIME_TYPE.equals(contentType)) {
                InputSource input = new InputSource(new ByteArrayInputStream(
                        message.getContentBytes()));
                EndUserNotificationParser parser = new EndUserNotificationParser(input, lang);
                imsModule
                        .getCore()
                        .getListener()
                        .handleUserNotification(contact, parser.getId(), parser.getSubject(),
                                parser.getText(), parser.getButtonOk());
            } else {
                if (logActivated) {
                    sLogger.warn("Unknown terms request ".concat(contentType));
                }
            }
        } catch (ParserConfigurationException e) {
            throw new SipPayloadException("Can't parse terms request!", e);

        } catch (SAXException e) {
            throw new SipPayloadException("Can't parse terms request!", e);

        } catch (IOException e) {
            throw new SipNetworkException("Can't parse terms request!", e);
        }
    }

    /**
     * Accept terms
     * 
     * @param requestId Request ID
     * @param pin Response value
     * @throws SipNetworkException
     * @throws SipPayloadException
     */
    public void acceptTerms(String requestId, String pin) throws SipPayloadException,
            SipNetworkException {
        if (sLogger.isActivated()) {
            sLogger.debug("Send response for request ".concat(requestId));
        }
        if (TextUtils.isEmpty(requestId)) {
            throw new SipPayloadException("requestId should never be null or empty!");
        }
        sendSipMessage(mRcsSettings.getEndUserConfirmationRequestUri(), requestId, ACCEPT_RESPONSE,
                pin);
    }

    /**
     * Reject terms
     * 
     * @param requestId Request ID
     * @param pin Response value
     * @throws SipNetworkException
     * @throws SipPayloadException
     */
    public void rejectTerms(String requestId, String pin) throws SipPayloadException,
            SipNetworkException {
        if (sLogger.isActivated()) {
            sLogger.debug("Send response for request ".concat(requestId));
        }
        if (TextUtils.isEmpty(requestId)) {
            throw new SipPayloadException("requestId should never be null or empty!");
        }
        sendSipMessage(mRcsSettings.getEndUserConfirmationRequestUri(), requestId,
                DECLINE_RESPONSE, pin);
    }

    /**
     * Send SIP MESSAGE
     * 
     * @param eucr
     * @param requestId
     * @param responseValue
     * @param pin
     * @throws SipPayloadException
     * @throws SipNetworkException
     */
    private void sendSipMessage(Uri eucr, String requestId, String responseValue, String pin)
            throws SipPayloadException, SipNetworkException {
        if (sLogger.isActivated()) {
            sLogger.debug("Send SIP response");
        }
        StringBuilder response = new StringBuilder(
                "<?xml version=\"1.0\" standalone=\"yes\"?><EndUserConfirmationResponse id=\"")
                .append(requestId).append("\" value=\"").append(responseValue).append("\"");

        if (pin != null) {
            response.append(" pin=\"").append(pin).append("\"");
        }
        response.append("/>");

        final SipManager sipManager = getImsModule().getSipManager();
        final SipInterface sipStack = sipManager.getSipStack();
        final String remoteServer = eucr.getPath();
        SipDialogPath dialogPath = new SipDialogPath(sipStack, sipStack.generateCallId(), 1,
                remoteServer, ImsModule.getImsUserProfile().getPublicUri(), remoteServer,
                sipStack.getServiceRoutePath(), mRcsSettings);

        if (sLogger.isActivated()) {
            sLogger.info("Send first MESSAGE");
        }
        SipRequest msg = SipMessageFactory.createMessage(dialogPath, RESPONSE_MIME_TYPE,
                response.toString());
        SipTransactionContext ctx = sipManager.sendSipMessageAndWait(msg);
        final int statusCode = ctx.getStatusCode();
        switch (statusCode) {
            case Response.PROXY_AUTHENTICATION_REQUIRED:
                handle407Authentication(dialogPath, ctx.getSipResponse(), response.toString());
                break;
            case Response.OK:
                /* Intentional fall-through */
            case Response.ACCEPTED:
                if (sLogger.isActivated()) {
                    sLogger.info("20x OK response received");
                }
                break;
            default:
                throw new IllegalArgumentException(new StringBuilder("Invalid response :  ")
                        .append(statusCode).append(" received!").toString());
        }
    }

    /**
     * Get remote identity of the incoming request
     * 
     * @param request Request
     * @return ID
     */
    private String getRemoteIdentity(SipRequest request) {
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

    /**
     * Handle proxy authentication response
     * 
     * @param dialogPath
     * @param sipResponse
     * @param response
     * @throws SipPayloadException
     * @throws SipNetworkException
     */
    private void handle407Authentication(SipDialogPath dialogPath, SipResponse sipResponse,
            String response) throws SipPayloadException, SipNetworkException {
        try {
            if (sLogger.isActivated()) {
                sLogger.info("407 response received");
            }
            final ImsModule imsModule = getImsModule();
            SessionAuthenticationAgent authenticationAgent = new SessionAuthenticationAgent(
                    imsModule);
            authenticationAgent.readProxyAuthenticateHeader(sipResponse);
            dialogPath.incrementCseq();
            if (sLogger.isActivated()) {
                sLogger.info("Send second MESSAGE");
            }
            SipRequest msg = SipMessageFactory.createMessage(dialogPath, RESPONSE_MIME_TYPE,
                    response);
            authenticationAgent.setProxyAuthorizationHeader(msg);
            SipTransactionContext ctx = imsModule.getSipManager().sendSipMessageAndWait(msg);
            final int statusCode = ctx.getStatusCode();
            switch (statusCode) {
                case Response.OK:
                    /* Intentional fall-through */
                case Response.ACCEPTED:
                    if (sLogger.isActivated()) {
                        sLogger.info("20x OK response received");
                    }
                    break;
                default:
                    throw new IllegalArgumentException(new StringBuilder("Invalid response :  ")
                            .append(statusCode).append(" received!").toString());
            }
        } catch (InvalidArgumentException e) {
            throw new SipPayloadException("Failed to handle 407 authentication response!", e);

        } catch (ParseException e) {
            throw new SipPayloadException("Failed to handle 407 authentication response!", e);
        }
    }
}
