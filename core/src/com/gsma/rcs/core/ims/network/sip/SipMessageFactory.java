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

package com.gsma.rcs.core.ims.network.sip;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.service.SessionTimerManager;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.services.rcs.contact.ContactId;

import android.net.Uri;

import gov2.nist.core.NameValue;
import gov2.nist.javax2.sip.Utils;
import gov2.nist.javax2.sip.header.Subject;

import java.text.ParseException;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax2.sip.ClientTransaction;
import javax2.sip.InvalidArgumentException;
import javax2.sip.SipException;
import javax2.sip.address.Address;
import javax2.sip.address.URI;
import javax2.sip.header.AcceptHeader;
import javax2.sip.header.CSeqHeader;
import javax2.sip.header.CallIdHeader;
import javax2.sip.header.ContactHeader;
import javax2.sip.header.ContentDispositionHeader;
import javax2.sip.header.ContentTypeHeader;
import javax2.sip.header.EventHeader;
import javax2.sip.header.ExpiresHeader;
import javax2.sip.header.FromHeader;
import javax2.sip.header.Header;
import javax2.sip.header.ReasonHeader;
import javax2.sip.header.ReferToHeader;
import javax2.sip.header.RequireHeader;
import javax2.sip.header.RouteHeader;
import javax2.sip.header.SIPIfMatchHeader;
import javax2.sip.header.SupportedHeader;
import javax2.sip.header.ToHeader;
import javax2.sip.header.UserAgentHeader;
import javax2.sip.header.ViaHeader;
import javax2.sip.header.WarningHeader;
import javax2.sip.message.Request;
import javax2.sip.message.Response;

/**
 * SIP message factory
 * 
 * @author Jean-Marc AUFFRET
 */
public class SipMessageFactory {
    /**
     * Rate to convert from seconds to milliseconds
     */
    private static final long SECONDS_TO_MILLISECONDS_CONVERSION_RATE = 1000;

    /**
     * Create a SIP REGISTER request
     * 
     * @param dialog SIP dialog path
     * @param featureTags Feature tags
     * @param expirePeriod Expiration period in milliseconds
     * @param instanceId UA SIP instance ID
     * @return SIP request
     * @throws PayloadException
     */
    public static SipRequest createRegister(SipDialogPath dialog, String[] featureTags,
            long expirePeriod, String instanceId) throws PayloadException {
        try {
            // Set request line header
            URI requestURI = SipUtils.ADDR_FACTORY.createURI(dialog.getTarget());

            // Set Call-Id header
            CallIdHeader callIdHeader = SipUtils.HEADER_FACTORY.createCallIdHeader(dialog
                    .getCallId());

            // Set the CSeq header
            CSeqHeader cseqHeader = SipUtils.HEADER_FACTORY.createCSeqHeader(dialog.getCseq(),
                    Request.REGISTER);

            // Set the From header
            Address fromAddress = SipUtils.ADDR_FACTORY.createAddress(dialog.getLocalParty());
            FromHeader fromHeader = SipUtils.HEADER_FACTORY.createFromHeader(fromAddress,
                    IdGenerator.getIdentifier());

            // Set the To header
            Address toAddress = SipUtils.ADDR_FACTORY.createAddress(dialog.getRemoteParty());
            ToHeader toHeader = SipUtils.HEADER_FACTORY.createToHeader(toAddress, null);

            // Insert "keep" flag to Via header (RFC6223 "Indication of Support for Keep-Alive")
            List<ViaHeader> viaHeaders = dialog.getSipStack().getViaHeaders();
            if (viaHeaders != null && !viaHeaders.isEmpty()) {
                ViaHeader viaHeader = viaHeaders.get(0);
                viaHeader.setParameter(new NameValue("keep", null, true));
            }

            // Create the request
            Request register = SipUtils.MSG_FACTORY.createRequest(requestURI, Request.REGISTER,
                    callIdHeader, cseqHeader, fromHeader, toHeader, viaHeaders,
                    SipUtils.buildMaxForwardsHeader());

            // Set Contact header
            ContactHeader contact = dialog.getSipStack().getLocalContact();
            if (instanceId != null) {
                contact.setParameter(SipUtils.SIP_INSTANCE_PARAM, instanceId);
            }
            register.addHeader(contact);

            // Set Supported header
            String supported;
            if (instanceId != null) {
                supported = "path, gruu";
            } else {
                supported = "path";
            }
            SupportedHeader supportedHeader = SipUtils.HEADER_FACTORY
                    .createSupportedHeader(supported);
            register.addHeader(supportedHeader);

            if (featureTags.length != 0) {
                // Set feature tags
                SipUtils.setContactFeatureTags(register, featureTags);
            }

            // Set Allow header
            SipUtils.buildAllowHeader(register);

            // Set the Expires header
            ExpiresHeader expHeader = SipUtils.HEADER_FACTORY
                    .createExpiresHeader((int) (expirePeriod / SECONDS_TO_MILLISECONDS_CONVERSION_RATE));
            register.addHeader(expHeader);

            // Set User-Agent header
            register.addHeader(SipUtils.buildUserAgentHeader());

            // Set "rport" (RFC3581)
            ViaHeader viaHeader = (ViaHeader) register.getHeader(ViaHeader.NAME);
            viaHeader.setRPort();

            return new SipRequest(register);

        } catch (ParseException e) {
            throw new PayloadException(new StringBuilder(
                    "Can't create SIP message for instanceId : ").append(instanceId).toString(), e);

        } catch (InvalidArgumentException e) {
            throw new PayloadException(new StringBuilder(
                    "Can't create SIP message for instanceId : ").append(instanceId).toString(), e);
        }
    }

    /**
     * Create a SIP SUBSCRIBE request
     * 
     * @param dialog SIP dialog path
     * @param expirePeriod Expiration period in milliseconds
     * @return SIP request
     * @throws PayloadException
     */
    public static SipRequest createSubscribe(SipDialogPath dialog, long expirePeriod)
            throws PayloadException {
        try {
            // Set request line header
            URI requestURI = SipUtils.ADDR_FACTORY.createURI(dialog.getTarget());

            // Set Call-Id header
            CallIdHeader callIdHeader = SipUtils.HEADER_FACTORY.createCallIdHeader(dialog
                    .getCallId());

            // Set the CSeq header
            CSeqHeader cseqHeader = SipUtils.HEADER_FACTORY.createCSeqHeader(dialog.getCseq(),
                    Request.SUBSCRIBE);

            // Set the From header
            Address fromAddress = SipUtils.ADDR_FACTORY.createAddress(dialog.getLocalParty());
            FromHeader fromHeader = SipUtils.HEADER_FACTORY.createFromHeader(fromAddress,
                    dialog.getLocalTag());

            // Set the To header
            Address toAddress = SipUtils.ADDR_FACTORY.createAddress(dialog.getRemoteParty());
            ToHeader toHeader = SipUtils.HEADER_FACTORY.createToHeader(toAddress,
                    dialog.getRemoteTag());

            // Create the request
            Request subscribe = SipUtils.MSG_FACTORY.createRequest(requestURI, Request.SUBSCRIBE,
                    callIdHeader, cseqHeader, fromHeader, toHeader, dialog.getSipStack()
                            .getViaHeaders(), SipUtils.buildMaxForwardsHeader());

            // Set the Route header
            Vector<String> route = dialog.getRoute();
            for (int i = 0; i < route.size(); i++) {
                Header routeHeader = SipUtils.HEADER_FACTORY.createHeader(RouteHeader.NAME,
                        route.elementAt(i));
                subscribe.addHeader(routeHeader);
            }

            // Set the Expires header
            ExpiresHeader expHeader = SipUtils.HEADER_FACTORY
                    .createExpiresHeader((int) (expirePeriod / SECONDS_TO_MILLISECONDS_CONVERSION_RATE));
            subscribe.addHeader(expHeader);

            // Set User-Agent header
            subscribe.addHeader(SipUtils.buildUserAgentHeader());

            // Set Contact header
            subscribe.addHeader(dialog.getSipStack().getContact());

            // Set Allow header
            SipUtils.buildAllowHeader(subscribe);

            // Set "rport" (RFC3581)
            ViaHeader viaHeader = (ViaHeader) subscribe.getHeader(ViaHeader.NAME);
            viaHeader.setRPort();

            return new SipRequest(subscribe);

        } catch (ParseException e) {
            throw new PayloadException("Can't create SIP message!", e);

        } catch (InvalidArgumentException e) {
            throw new PayloadException("Can't create SIP message!", e);
        }
    }

    /**
     * Create a SIP MESSAGE request with a feature tag
     * 
     * @param dialog SIP dialog path
     * @param contentType Content type
     * @param content Content
     * @return SIP request
     * @throws PayloadException
     */
    public static SipRequest createMessage(SipDialogPath dialog, String contentType, String content)
            throws PayloadException {
        return createMessage(dialog, null, contentType, content.getBytes(UTF8));
    }

    /**
     * Create a SIP MESSAGE request with a feature tag
     * 
     * @param dialog SIP dialog path
     * @param featureTag Feature tag
     * @param contentType Content type
     * @param content Content
     * @return SIP request
     * @throws PayloadException
     */
    public static SipRequest createMessage(SipDialogPath dialog, String featureTag,
            String contentType, byte[] content) throws PayloadException {
        try {
            // Set request line header
            URI requestURI = SipUtils.ADDR_FACTORY.createURI(dialog.getTarget());

            // Set Call-Id header
            CallIdHeader callIdHeader = SipUtils.HEADER_FACTORY.createCallIdHeader(dialog
                    .getCallId());

            // Set the CSeq header
            CSeqHeader cseqHeader = SipUtils.HEADER_FACTORY.createCSeqHeader(dialog.getCseq(),
                    Request.MESSAGE);

            // Set the From header
            Address fromAddress = SipUtils.ADDR_FACTORY.createAddress(dialog.getLocalParty());
            FromHeader fromHeader = SipUtils.HEADER_FACTORY.createFromHeader(fromAddress,
                    dialog.getLocalTag());

            // Set the To header
            Address toAddress = SipUtils.ADDR_FACTORY.createAddress(dialog.getRemoteParty());
            ToHeader toHeader = SipUtils.HEADER_FACTORY.createToHeader(toAddress,
                    dialog.getRemoteTag());

            // Create the request
            Request message = SipUtils.MSG_FACTORY.createRequest(requestURI, Request.MESSAGE,
                    callIdHeader, cseqHeader, fromHeader, toHeader, dialog.getSipStack()
                            .getViaHeaders(), SipUtils.buildMaxForwardsHeader());

            // Set the Route header
            Vector<String> route = dialog.getRoute();
            for (int i = 0; i < route.size(); i++) {
                Header routeHeader = SipUtils.HEADER_FACTORY.createHeader(RouteHeader.NAME,
                        route.elementAt(i));
                message.addHeader(routeHeader);
            }

            setPPreferedIdentityHeader(message);

            // Set Contact header
            message.addHeader(dialog.getSipStack().getContact());

            // Set User-Agent header
            message.addHeader(SipUtils.buildUserAgentHeader());

            // Set feature tags
            if (featureTag != null) {
                SipUtils.setFeatureTags(message, new String[] {
                    featureTag
                });
            }

            // Set the message content
            String[] type = contentType.split("/");
            message.setContent(content,
                    SipUtils.HEADER_FACTORY.createContentTypeHeader(type[0], type[1]));

            // Set the message content length
            message.setContentLength(SipUtils.HEADER_FACTORY
                    .createContentLengthHeader(content.length));

            // Set "rport" (RFC3581)
            ViaHeader viaHeader = (ViaHeader) message.getHeader(ViaHeader.NAME);
            viaHeader.setRPort();

            String remoteInstanceId = dialog.getRemoteSipInstance();
            if (remoteInstanceId != null) {
                // Add remote SIP instance ID
                SipUtils.setRemoteInstanceID(message, remoteInstanceId);
            }

            return new SipRequest(message);

        } catch (ParseException e) {
            throw new PayloadException(new StringBuilder(
                    "Can't create SIP message for featureTag : ").append(featureTag)
                    .append(" with contentType : ").append(contentType).toString(), e);

        } catch (InvalidArgumentException e) {
            throw new PayloadException(new StringBuilder(
                    "Can't create SIP message for featureTag : ").append(featureTag)
                    .append(" with contentType : ").append(contentType).toString(), e);
        }
    }

    /**
     * Create a SIP PUBLISH request
     * 
     * @param dialog SIP dialog path
     * @param expirePeriod Expiration period in milliseconds
     * @param entityTag Entity tag
     * @param sdp SDP part
     * @return SIP request
     * @throws PayloadException
     */
    public static SipRequest createPublish(SipDialogPath dialog, long expirePeriod,
            String entityTag, String sdp) throws PayloadException {
        try {
            // Set request line header
            URI requestURI = SipUtils.ADDR_FACTORY.createURI(dialog.getTarget());

            // Set Call-Id header
            CallIdHeader callIdHeader = SipUtils.HEADER_FACTORY.createCallIdHeader(dialog
                    .getCallId());

            // Set the CSeq header
            CSeqHeader cseqHeader = SipUtils.HEADER_FACTORY.createCSeqHeader(dialog.getCseq(),
                    Request.PUBLISH);

            // Set the From header
            Address fromAddress = SipUtils.ADDR_FACTORY.createAddress(dialog.getLocalParty());
            FromHeader fromHeader = SipUtils.HEADER_FACTORY.createFromHeader(fromAddress,
                    dialog.getLocalTag());

            // Set the To header
            Address toAddress = SipUtils.ADDR_FACTORY.createAddress(dialog.getRemoteParty());
            ToHeader toHeader = SipUtils.HEADER_FACTORY.createToHeader(toAddress,
                    dialog.getRemoteTag());

            // Create the request
            Request publish = SipUtils.MSG_FACTORY.createRequest(requestURI, Request.PUBLISH,
                    callIdHeader, cseqHeader, fromHeader, toHeader, dialog.getSipStack()
                            .getViaHeaders(), SipUtils.buildMaxForwardsHeader());

            // Set the Route header
            Vector<String> route = dialog.getRoute();
            for (int i = 0; i < route.size(); i++) {
                Header routeHeader = SipUtils.HEADER_FACTORY.createHeader(RouteHeader.NAME,
                        route.elementAt(i));
                publish.addHeader(routeHeader);
            }

            // Set the Expires header
            ExpiresHeader expHeader = SipUtils.HEADER_FACTORY
                    .createExpiresHeader((int) (expirePeriod / SECONDS_TO_MILLISECONDS_CONVERSION_RATE));
            publish.addHeader(expHeader);

            // Set the SIP-If-Match header
            if (entityTag != null) {
                Header sipIfMatchHeader = SipUtils.HEADER_FACTORY.createHeader(
                        SIPIfMatchHeader.NAME, entityTag);
                publish.addHeader(sipIfMatchHeader);
            }

            // Set User-Agent header
            publish.addHeader(SipUtils.buildUserAgentHeader());

            // Set the Event header
            publish.addHeader(SipUtils.HEADER_FACTORY.createHeader(EventHeader.NAME, "presence"));

            // Set the message content
            if (sdp != null) {
                publish.setContent(sdp,
                        SipUtils.HEADER_FACTORY.createContentTypeHeader("application", "pidf+xml"));
            }

            // Set the message content length
            int length = sdp == null ? 0 : sdp.getBytes(UTF8).length;
            publish.setContentLength(SipUtils.HEADER_FACTORY.createContentLengthHeader(length));

            // Set "rport" (RFC3581)
            ViaHeader viaHeader = (ViaHeader) publish.getHeader(ViaHeader.NAME);
            viaHeader.setRPort();

            return new SipRequest(publish);

        } catch (ParseException e) {
            throw new PayloadException("Can't create SIP message", e);

        } catch (InvalidArgumentException e) {
            throw new PayloadException("Can't create SIP message", e);
        }
    }

    /**
     * Create a SIP INVITE request
     * 
     * @param dialog SIP dialog path
     * @param featureTags Feature tags
     * @param sdp SDP part
     * @return SIP request
     * @throws PayloadException
     */
    public static SipRequest createInvite(SipDialogPath dialog, String[] featureTags, String sdp)
            throws PayloadException {
        return createInvite(dialog, featureTags, featureTags, sdp);
    }

    /**
     * Create a SIP INVITE request
     * 
     * @param dialog SIP dialog path
     * @param featureTags Feature tags
     * @param acceptTags Feature tags
     * @param sdp SDP part
     * @return SIP request
     * @throws PayloadException
     */
    public static SipRequest createInvite(SipDialogPath dialog, String[] featureTags,
            String[] acceptTags, String sdp) throws PayloadException {
        try {
            // Create the content type
            ContentTypeHeader contentType = SipUtils.HEADER_FACTORY.createContentTypeHeader(
                    "application", "sdp");

            // Create the request
            return createInvite(dialog, featureTags, acceptTags, sdp, contentType);

        } catch (ParseException e) {
            throw new PayloadException(new StringBuilder("Can't create SIP message with SDP : ")
                    .append(sdp).toString(), e);
        }
    }

    /**
     * Create a SIP INVITE request
     * 
     * @param dialog SIP dialog path
     * @param featureTags Feature tags
     * @param multipart Multipart
     * @param boundary Boundary tag
     * @return SIP request
     * @throws PayloadException
     */
    public static SipRequest createMultipartInvite(SipDialogPath dialog, String[] featureTags,
            String multipart, String boundary) throws PayloadException {
        return createMultipartInvite(dialog, featureTags, featureTags, multipart, boundary);
    }

    /**
     * Create a SIP INVITE request
     * 
     * @param dialog SIP dialog path
     * @param featureTags Feature tags
     * @param acceptTags Feature tags
     * @param multipart Multipart
     * @param boundary Boundary tag
     * @return SIP request
     * @throws PayloadException
     */
    public static SipRequest createMultipartInvite(SipDialogPath dialog, String[] featureTags,
            String[] acceptTags, String multipart, String boundary) throws PayloadException {
        try {
            // Create the content type
            ContentTypeHeader contentType = SipUtils.HEADER_FACTORY.createContentTypeHeader(
                    "multipart", "mixed");
            contentType.setParameter("boundary", boundary);

            // Create the request
            return createInvite(dialog, featureTags, acceptTags, multipart, contentType);

        } catch (ParseException e) {
            throw new PayloadException(new StringBuilder(
                    "Can't create SIP message with multipart : ").append(multipart).toString(), e);
        }
    }

    /**
     * Create a SIP INVITE request
     * 
     * @param dialog SIP dialog path
     * @param featureTags Feature tags
     * @param acceptTags Feature tags
     * @param content Content
     * @param contentType Content type
     * @return SIP request
     * @throws PayloadException
     */
    public static SipRequest createInvite(SipDialogPath dialog, String[] featureTags,
            String[] acceptTags, String content, ContentTypeHeader contentType)
            throws PayloadException {
        try {
            // Set request line header
            URI requestURI = SipUtils.ADDR_FACTORY.createURI(dialog.getTarget());

            // Set Call-Id header
            CallIdHeader callIdHeader = SipUtils.HEADER_FACTORY.createCallIdHeader(dialog
                    .getCallId());

            // Set the CSeq header
            CSeqHeader cseqHeader = SipUtils.HEADER_FACTORY.createCSeqHeader(dialog.getCseq(),
                    Request.INVITE);

            // Set the From header
            Address fromAddress = SipUtils.ADDR_FACTORY.createAddress(dialog.getLocalParty());
            FromHeader fromHeader = SipUtils.HEADER_FACTORY.createFromHeader(fromAddress,
                    dialog.getLocalTag());

            // Set the To header
            Address toAddress = SipUtils.ADDR_FACTORY.createAddress(dialog.getRemoteParty());
            ToHeader toHeader = SipUtils.HEADER_FACTORY.createToHeader(toAddress, null);

            // Insert "keep" flag to Via header (RFC6223 "Indication of Support for Keep-Alive")
            List<ViaHeader> viaHeaders = dialog.getSipStack().getViaHeaders();
            if (viaHeaders != null && !viaHeaders.isEmpty()) {
                ViaHeader viaHeader = viaHeaders.get(0);
                viaHeader.setParameter(new NameValue("keep", null, true));
            }

            // Create the request
            Request invite = SipUtils.MSG_FACTORY.createRequest(requestURI, Request.INVITE,
                    callIdHeader, cseqHeader, fromHeader, toHeader, viaHeaders,
                    SipUtils.buildMaxForwardsHeader());

            // Set Contact header
            invite.addHeader(dialog.getSipStack().getContact());

            // Set feature tags
            SipUtils.setFeatureTags(invite, featureTags, acceptTags);

            // Set Allow header
            SipUtils.buildAllowHeader(invite);

            // Set the Route header
            Vector<String> route = dialog.getRoute();
            for (int i = 0; i < route.size(); i++) {
                Header routeHeader = SipUtils.HEADER_FACTORY.createHeader(RouteHeader.NAME,
                        route.elementAt(i));
                invite.addHeader(routeHeader);
            }

            setPPreferedIdentityHeader(invite);

            // Set User-Agent header
            invite.addHeader(SipUtils.buildUserAgentHeader());

            // Add session timer management
            if (dialog.getSessionExpireTime() >= SessionTimerManager.MIN_EXPIRE_PERIOD) {
                // Set the Supported header
                Header supportedHeader = SipUtils.HEADER_FACTORY.createHeader(SupportedHeader.NAME,
                        "timer");
                invite.addHeader(supportedHeader);

                // Set Session-Timer headers
                Header sessionExpiresHeader = SipUtils.HEADER_FACTORY.createHeader(
                        SipUtils.HEADER_SESSION_EXPIRES, "" + dialog.getSessionExpireTime());
                invite.addHeader(sessionExpiresHeader);
            }

            // Set the message content
            invite.setContent(content, contentType);

            // Set the content length
            invite.setContentLength(SipUtils.HEADER_FACTORY.createContentLengthHeader(content
                    .getBytes(UTF8).length));

            // Set "rport" (RFC3581)
            ViaHeader viaHeader = (ViaHeader) invite.getHeader(ViaHeader.NAME);
            viaHeader.setRPort();

            return new SipRequest(invite);

        } catch (ParseException e) {
            throw new PayloadException(
                    new StringBuilder("Can't create SIP message with content : ").append(content)
                            .toString(), e);

        } catch (InvalidArgumentException e) {
            throw new PayloadException(
                    new StringBuilder("Can't create SIP message with content : ").append(content)
                            .toString(), e);
        }
    }

    /**
     * Create a 200 OK response for INVITE request
     * 
     * @param dialog SIP dialog path
     * @param featureTags Feature tags
     * @param sdp SDP part
     * @return SIP response
     * @throws PayloadException
     */
    public static SipResponse create200OkInviteResponse(SipDialogPath dialog, String[] featureTags,
            String sdp) throws PayloadException {
        return create200OkInviteResponse(dialog, featureTags, featureTags, sdp);
    }

    /**
     * Create a 200 OK response for INVITE request
     * 
     * @param dialog SIP dialog path
     * @param featureTags Feature tags
     * @param acceptContactTags Feature tags
     * @param sdp SDP part
     * @return SIP response
     * @throws PayloadException
     */
    public static SipResponse create200OkInviteResponse(SipDialogPath dialog, String[] featureTags,
            String[] acceptContactTags, String sdp) throws PayloadException {
        try {
            // Create the response
            Response response = SipUtils.MSG_FACTORY.createResponse(200, dialog.getInvite()
                    .getStackMessage());

            // Set the local tag
            ToHeader to = (ToHeader) response.getHeader(ToHeader.NAME);
            to.setTag(dialog.getLocalTag());

            // Set Contact header
            response.addHeader(dialog.getSipStack().getContact());

            // Set feature tags
            SipUtils.setFeatureTags(response, featureTags, acceptContactTags);

            // Set Allow header
            SipUtils.buildAllowHeader(response);

            // Set the Server header
            response.addHeader(SipUtils.buildServerHeader());

            // Add session timer management
            if (dialog.getSessionExpireTime() >= SessionTimerManager.MIN_EXPIRE_PERIOD) {
                // Set the Require header
                Header requireHeader = SipUtils.HEADER_FACTORY.createHeader(RequireHeader.NAME,
                        "timer");
                response.addHeader(requireHeader);

                // Set Session-Timer header
                Header sessionExpiresHeader = SipUtils.HEADER_FACTORY.createHeader(
                        SipUtils.HEADER_SESSION_EXPIRES,
                        new StringBuilder(String.valueOf(dialog.getSessionExpireTime()
                                / SECONDS_TO_MILLISECONDS_CONVERSION_RATE)).append(";refresher=")
                                .append(dialog.getInvite().getSessionTimerRefresher()).toString());
                response.addHeader(sessionExpiresHeader);
            }

            // Set the message content
            response.setContent(sdp,
                    SipUtils.HEADER_FACTORY.createContentTypeHeader("application", "sdp"));

            // Set the message content length
            response.setContentLength(SipUtils.HEADER_FACTORY.createContentLengthHeader(sdp
                    .getBytes(UTF8).length));

            SipResponse resp = new SipResponse(response);
            resp.setStackTransaction(dialog.getInvite().getStackTransaction());
            return resp;

        } catch (ParseException e) {
            throw new PayloadException(new StringBuilder("Can't create SIP response with SDP : ")
                    .append(sdp).toString(), e);

        } catch (InvalidArgumentException e) {
            throw new PayloadException(new StringBuilder("Can't create SIP response with SDP : ")
                    .append(sdp).toString(), e);
        }
    }

    /**
     * Create a SIP ACK request
     * 
     * @param dialog SIP dialog path
     * @return SIP request
     * @throws PayloadException
     */
    public static SipRequest createAck(SipDialogPath dialog) throws PayloadException {
        try {
            Request ack = null;

            // Set request line header
            URI requestURI = SipUtils.ADDR_FACTORY.createURI(dialog.getTarget());

            // Set Call-Id header
            CallIdHeader callIdHeader = SipUtils.HEADER_FACTORY.createCallIdHeader(dialog
                    .getCallId());

            // Set the CSeq header
            CSeqHeader cseqHeader = SipUtils.HEADER_FACTORY.createCSeqHeader(dialog.getCseq(),
                    Request.ACK);

            // Set the From header
            Address fromAddress = SipUtils.ADDR_FACTORY.createAddress(dialog.getLocalParty());
            FromHeader fromHeader = SipUtils.HEADER_FACTORY.createFromHeader(fromAddress,
                    dialog.getLocalTag());

            // Set the To header
            Address toAddress = SipUtils.ADDR_FACTORY.createAddress(dialog.getRemoteParty());
            ToHeader toHeader = SipUtils.HEADER_FACTORY.createToHeader(toAddress,
                    dialog.getRemoteTag());

            // Set the Via branch
            List<ViaHeader> vias = dialog.getSipStack().getViaHeaders();
            vias.get(0).setBranch(Utils.getInstance().generateBranchId());

            // Create the ACK request
            ack = SipUtils.MSG_FACTORY.createRequest(requestURI, Request.ACK, callIdHeader,
                    cseqHeader, fromHeader, toHeader, vias, SipUtils.buildMaxForwardsHeader());

            // Set the Route header
            Vector<String> route = dialog.getRoute();
            for (int i = 0; i < route.size(); i++) {
                Header routeHeader = SipUtils.HEADER_FACTORY.createHeader(RouteHeader.NAME,
                        route.elementAt(i));
                ack.addHeader(routeHeader);
            }

            // Set Contact header
            ack.addHeader(dialog.getSipStack().getContact());

            // Set User-Agent header
            ack.addHeader(SipUtils.buildUserAgentHeader());

            // Set Allow header
            SipUtils.buildAllowHeader(ack);

            // Set "rport" (RFC3581)
            ViaHeader viaHeader = (ViaHeader) ack.getHeader(ViaHeader.NAME);
            viaHeader.setRPort();

            return new SipRequest(ack);

        } catch (ParseException e) {
            throw new PayloadException("Can't create SIP message!", e);

        } catch (InvalidArgumentException e) {
            throw new PayloadException("Can't create SIP message!", e);
        }
    }

    /**
     * Create a SIP response
     * 
     * @param request SIP request
     * @param code Response code
     * @return SIP response
     * @throws PayloadException
     */
    public static SipResponse createResponse(SipRequest request, int code) throws PayloadException {
        try {
            // Create the response
            Response response = SipUtils.MSG_FACTORY
                    .createResponse(code, request.getStackMessage());
            SipResponse resp = new SipResponse(response);
            resp.setStackTransaction(request.getStackTransaction());
            return resp;

        } catch (ParseException e) {
            throw new PayloadException("Can't create SIP response", e);
        }
    }

    /**
     * Works just like SipResponse createResponse(SipRequest request, String localTag, int code,
     * String warning) except the warning is always null
     * 
     * @see #SipResponse createResponse(SipRequest request, String localTag, int code, String
     *      warning)
     */
    public static SipResponse createResponse(SipRequest request, String localTag, int code)
            throws PayloadException {
        return createResponse(request, localTag, code, null);
    }

    /**
     * Create a SIP response with a specific local tag and warning
     * 
     * @param request the SIP request
     * @param localTag the Local tag
     * @param warning the warning message
     * @return the SIP response
     * @throws PayloadException
     */
    public static SipResponse createResponse(SipRequest request, String localTag, int code,
            String warning) throws PayloadException {
        try {
            // Create the response
            Response response = SipUtils.MSG_FACTORY
                    .createResponse(code, request.getStackMessage());

            // Set the local tag
            if (localTag != null) {
                ToHeader to = (ToHeader) response.getHeader(ToHeader.NAME);
                to.setTag(localTag);
            }
            if (warning != null) {
                WarningHeader warningHeader = SipUtils.HEADER_FACTORY.createWarningHeader("SIP",
                        403, warning);
                response.addHeader(warningHeader);
            }
            SipResponse resp = new SipResponse(response);
            resp.setStackTransaction(request.getStackTransaction());
            return resp;

        } catch (ParseException e) {
            throw new PayloadException(
                    new StringBuilder("Can't create SIP message for localTag : ").append(localTag)
                            .toString(), e);

        } catch (InvalidArgumentException e) {
            throw new PayloadException(
                    new StringBuilder("Can't create SIP message for localTag : ").append(localTag)
                            .toString(), e);
        }
    }

    /**
     * Create a SIP BYE request
     * 
     * @param dialog SIP dialog path
     * @return SIP request
     * @throws PayloadException
     */
    public static SipRequest createBye(SipDialogPath dialog) throws PayloadException {
        try {
            // Create the request
            Request bye = dialog.getStackDialog().createRequest(Request.BYE);

            // Set termination reason
            int reasonCode = dialog.getSessionTerminationReasonCode();
            if (reasonCode != -1) {
                ReasonHeader reasonHeader = SipUtils.HEADER_FACTORY.createReasonHeader("SIP",
                        reasonCode, dialog.getSessionTerminationReasonPhrase());
                bye.addHeader(reasonHeader);
            }

            // Set "rport" (RFC3581)
            ViaHeader viaHeader = (ViaHeader) bye.getHeader(ViaHeader.NAME);
            viaHeader.setRPort();

            return new SipRequest(bye);

        } catch (ParseException e) {
            throw new PayloadException("Can't create SIP message!", e);

        } catch (InvalidArgumentException e) {
            throw new PayloadException("Can't create SIP message!", e);

        } catch (SipException e) {
            throw new PayloadException("Can't create SIP message!", e);
        }
    }

    /**
     * Create a SIP CANCEL request
     * 
     * @param dialog SIP dialog path
     * @return SIP request
     * @throws PayloadException
     */
    public static SipRequest createCancel(SipDialogPath dialog) throws PayloadException {
        try {
            // Create the request
            ClientTransaction transaction = (ClientTransaction) dialog.getInvite()
                    .getStackTransaction();
            Request cancel = transaction.createCancel();

            // Set termination reason
            int reasonCode = dialog.getSessionTerminationReasonCode();
            if (reasonCode != -1) {
                ReasonHeader reasonHeader = SipUtils.HEADER_FACTORY.createReasonHeader("SIP",
                        reasonCode, dialog.getSessionTerminationReasonPhrase());
                cancel.addHeader(reasonHeader);
            }

            // Set "rport" (RFC3581)
            ViaHeader viaHeader = (ViaHeader) cancel.getHeader(ViaHeader.NAME);
            viaHeader.setRPort();

            return new SipRequest(cancel);

        } catch (ParseException e) {
            throw new PayloadException("Can't create SIP message!", e);

        } catch (InvalidArgumentException e) {
            throw new PayloadException("Can't create SIP message!", e);

        } catch (SipException e) {
            throw new PayloadException("Can't create SIP message!", e);
        }
    }

    /**
     * Create a SIP OPTIONS request
     * 
     * @param dialog SIP dialog path
     * @param featureTags Feature tags
     * @return SIP request
     * @throws PayloadException
     */
    public static SipRequest createOptions(SipDialogPath dialog, String[] featureTags)
            throws PayloadException {
        try {
            // Set request line header
            URI requestURI = SipUtils.ADDR_FACTORY.createURI(dialog.getTarget());

            // Set Call-Id header
            CallIdHeader callIdHeader = SipUtils.HEADER_FACTORY.createCallIdHeader(dialog
                    .getCallId());

            // Set the CSeq header
            CSeqHeader cseqHeader = SipUtils.HEADER_FACTORY.createCSeqHeader(dialog.getCseq(),
                    Request.OPTIONS);

            // Set the From header
            Address fromAddress = SipUtils.ADDR_FACTORY.createAddress(dialog.getLocalParty());
            FromHeader fromHeader = SipUtils.HEADER_FACTORY.createFromHeader(fromAddress,
                    dialog.getLocalTag());

            // Set the To header
            Address toAddress = SipUtils.ADDR_FACTORY.createAddress(dialog.getRemoteParty());
            ToHeader toHeader = SipUtils.HEADER_FACTORY.createToHeader(toAddress, null);

            // Create the request
            Request options = SipUtils.MSG_FACTORY.createRequest(requestURI, Request.OPTIONS,
                    callIdHeader, cseqHeader, fromHeader, toHeader, dialog.getSipStack()
                            .getViaHeaders(), SipUtils.buildMaxForwardsHeader());

            // Set Contact header
            options.addHeader(dialog.getSipStack().getContact());

            // Set Accept header
            Header acceptHeader = SipUtils.HEADER_FACTORY.createHeader(AcceptHeader.NAME,
                    "application/sdp");
            options.addHeader(acceptHeader);

            // Set feature tags
            SipUtils.setFeatureTags(options, featureTags);

            // Set Allow header
            SipUtils.buildAllowHeader(options);

            // Set the Route header
            Vector<String> route = dialog.getRoute();
            for (int i = 0; i < route.size(); i++) {
                Header routeHeader = SipUtils.HEADER_FACTORY.createHeader(RouteHeader.NAME,
                        route.elementAt(i));
                options.addHeader(routeHeader);
            }

            setPPreferedIdentityHeader(options);

            // Set User-Agent header
            options.addHeader(SipUtils.buildUserAgentHeader());

            // Set "rport" (RFC3581)
            ViaHeader viaHeader = (ViaHeader) options.getHeader(ViaHeader.NAME);
            viaHeader.setRPort();

            return new SipRequest(options);

        } catch (ParseException e) {
            throw new PayloadException("Can't create SIP message!", e);

        } catch (InvalidArgumentException e) {
            throw new PayloadException("Can't create SIP message!", e);
        }
    }

    /**
     * Create a 200 OK response for OPTIONS request
     * 
     * @param options SIP options
     * @param contact Contact header
     * @param featureTags Feature tags
     * @param sdp SDP part
     * @return SIP response
     * @throws PayloadException
     */
    public static SipResponse create200OkOptionsResponse(SipRequest options, ContactHeader contact,
            String[] featureTags, String sdp) throws PayloadException {
        try {
            // Create the response
            Response response = SipUtils.MSG_FACTORY.createResponse(200, options.getStackMessage());

            // Set the local tag
            ToHeader to = (ToHeader) response.getHeader(ToHeader.NAME);
            to.setTag(IdGenerator.getIdentifier());

            // Set Contact header
            response.addHeader(contact);

            // Set feature tags
            SipUtils.setFeatureTags(response, featureTags);

            // Set Allow header
            SipUtils.buildAllowHeader(response);

            // Set the Server header
            response.addHeader(SipUtils.buildServerHeader());

            // Set the content part if available
            if (sdp != null) {
                // Set the content type header
                response.setContent(sdp,
                        SipUtils.HEADER_FACTORY.createContentTypeHeader("application", "sdp"));

                // Set the content length header
                response.setContentLength(SipUtils.HEADER_FACTORY.createContentLengthHeader(sdp
                        .getBytes(UTF8).length));
            }

            SipResponse resp = new SipResponse(response);
            resp.setStackTransaction(options.getStackTransaction());
            return resp;

        } catch (ParseException e) {
            throw new PayloadException(new StringBuilder("Can't create SIP response for SDP : ")
                    .append(sdp).toString(), e);

        } catch (InvalidArgumentException e) {
            throw new PayloadException(new StringBuilder("Can't create SIP response for SDP : ")
                    .append(sdp).toString(), e);
        }
    }

    /**
     * Create a SIP REFER request
     * 
     * @param dialog SIP dialog path
     * @param toContact Refer to contact
     * @param subject Subject
     * @param contributionId Contribution ID
     * @return SIP request
     * @throws PayloadException
     */
    public static SipRequest createRefer(SipDialogPath dialog, Uri toContact, String subject,
            String contributionId) throws PayloadException {
        try {
            // Create the request
            Request refer = dialog.getStackDialog().createRequest(Request.REFER);

            // Set feature tags
            String[] tags = {
                FeatureTags.FEATURE_OMA_IM
            };
            SipUtils.setFeatureTags(refer, tags);

            // Set Refer-To header
            refer.addHeader(SipUtils.HEADER_FACTORY.createHeader(ReferToHeader.NAME,
                    toContact.toString()));

            // Set Refer-Sub header
            refer.addHeader(SipUtils.HEADER_FACTORY
                    .createHeader(SipUtils.HEADER_REFER_SUB, "false"));

            setPPreferedIdentityHeader(refer);

            // Set Subject header
            if (subject != null) {
                Header sub = SipUtils.HEADER_FACTORY.createHeader(Subject.NAME, subject);
                refer.addHeader(sub);
            }

            // Set Contribution-ID header
            refer.addHeader(SipUtils.HEADER_FACTORY.createHeader(ChatUtils.HEADER_CONTRIBUTION_ID,
                    contributionId));

            // Set User-Agent header
            refer.addHeader(SipUtils.buildUserAgentHeader());

            // Set "rport" (RFC3581)
            ViaHeader viaHeader = (ViaHeader) refer.getHeader(ViaHeader.NAME);
            viaHeader.setRPort();

            String remoteInstanceId = dialog.getRemoteSipInstance();
            if (remoteInstanceId != null) {
                // Add remote SIP instance ID
                SipUtils.setRemoteInstanceID(refer, remoteInstanceId);
            }

            return new SipRequest(refer);

        } catch (ParseException e) {
            throw new PayloadException(new StringBuilder("Can't create SIP REFER for contact '")
                    .append(toContact).append("' with contributionId : ").append(contributionId)
                    .toString(), e);

        } catch (SipException e) {
            throw new PayloadException(new StringBuilder("Can't create SIP REFER for contact '")
                    .append(toContact).append("' with contributionId : ").append(contributionId)
                    .toString(), e);
        }
    }

    /**
     * Create a SIP REFER request
     * 
     * @param dialog SIP dialog path
     * @param participants Set of participants
     * @param subject Subject
     * @param contributionId Contribution ID
     * @return SIP request
     * @throws PayloadException
     */
    public static SipRequest createRefer(SipDialogPath dialog, Set<ContactId> participants,
            String subject, String contributionId) throws PayloadException {
        try {
            // Create the request
            Request refer = dialog.getStackDialog().createRequest(Request.REFER);

            // Generate a list URI
            String listID = "Id_" + System.currentTimeMillis();

            // Set feature tags
            String[] tags = {
                FeatureTags.FEATURE_OMA_IM
            };
            SipUtils.setFeatureTags(refer, tags);

            // Set Require header
            refer.addHeader(SipUtils.HEADER_FACTORY.createHeader(RequireHeader.NAME,
                    "multiple-refer"));
            refer.addHeader(SipUtils.HEADER_FACTORY.createHeader(RequireHeader.NAME, "norefersub"));

            // Set Refer-To header
            String homeDomain = ImsModule.getImsUserProfile().getHomeDomain();
            StringBuilder referToValue = new StringBuilder("<cid:").append(listID).append("@")
                    .append(homeDomain).append(">");
            refer.addHeader(SipUtils.HEADER_FACTORY.createHeader(ReferToHeader.NAME,
                    referToValue.toString()));

            // Set Refer-Sub header
            refer.addHeader(SipUtils.HEADER_FACTORY
                    .createHeader(SipUtils.HEADER_REFER_SUB, "false"));

            setPPreferedIdentityHeader(refer);

            // Set Subject header
            refer.addHeader(SipUtils.HEADER_FACTORY.createHeader(Subject.NAME, subject));

            // Set Contribution-ID header
            refer.addHeader(SipUtils.HEADER_FACTORY.createHeader(ChatUtils.HEADER_CONTRIBUTION_ID,
                    contributionId));

            // Set User-Agent header
            refer.addHeader(SipUtils.buildUserAgentHeader());

            // Set the Content-ID header
            StringBuilder contentIdHeadervalue = new StringBuilder("<").append(listID).append("@")
                    .append(homeDomain).append(">");
            refer.addHeader(SipUtils.HEADER_FACTORY.createHeader(SipUtils.HEADER_CONTENT_ID,
                    contentIdHeadervalue.toString()));

            // Generate the resource list for given participants
            String resourceList = ChatUtils.generateChatResourceList(participants);

            // Set the message content
            refer.setContent(resourceList, SipUtils.HEADER_FACTORY.createContentTypeHeader(
                    "application", "resource-lists+xml"));

            // Set the message content length
            refer.setContentLength(SipUtils.HEADER_FACTORY.createContentLengthHeader(resourceList
                    .getBytes(UTF8).length));

            // Set the Content-Disposition header
            Header contentDispoHeader = SipUtils.HEADER_FACTORY.createHeader(
                    ContentDispositionHeader.NAME, "recipient-list");
            refer.addHeader(contentDispoHeader);

            // Set "rport" (RFC3581)
            ViaHeader viaHeader = (ViaHeader) refer.getHeader(ViaHeader.NAME);
            viaHeader.setRPort();

            String remoteInstanceId = dialog.getRemoteSipInstance();
            if (remoteInstanceId != null) {
                // Add remote SIP instance ID
                SipUtils.setRemoteInstanceID(refer, remoteInstanceId);
            }

            return new SipRequest(refer);

        } catch (ParseException e) {
            throw new PayloadException(new StringBuilder(
                    "Can't create SIP REFER for contributionId : ").append(contributionId)
                    .toString(), e);

        } catch (SipException e) {
            throw new PayloadException(new StringBuilder(
                    "Can't create SIP REFER for contributionId : ").append(contributionId)
                    .toString(), e);
        }
    }

    /**
     * Create a SIP RE-INVITE request
     * 
     * @param dialog SIP dialog path
     * @return SIP request
     * @throws PayloadException
     */
    public static SipRequest createReInvite(SipDialogPath dialog) throws PayloadException {
        try {
            // Build the request
            Request reInvite = dialog.getStackDialog().createRequest(Request.INVITE);
            SipRequest firstInvite = dialog.getInvite();

            // Set feature tags
            reInvite.removeHeader(ContactHeader.NAME);
            reInvite.addHeader(firstInvite.getHeader(ContactHeader.NAME));
            reInvite.removeHeader(SipUtils.HEADER_ACCEPT_CONTACT);
            reInvite.addHeader(firstInvite.getHeader(SipUtils.HEADER_ACCEPT_CONTACT));

            // Set Allow header
            SipUtils.buildAllowHeader(reInvite);

            // Set the Route header
            reInvite.addHeader(firstInvite.getHeader(RouteHeader.NAME));

            // Set the P-Preferred-Identity header
            reInvite.addHeader(firstInvite.getHeader(SipUtils.HEADER_P_PREFERRED_IDENTITY));

            // Set User-Agent header
            reInvite.addHeader(firstInvite.getHeader(UserAgentHeader.NAME));

            // Add session timer management
            if (dialog.getSessionExpireTime() >= SessionTimerManager.MIN_EXPIRE_PERIOD) {
                // Set the Supported header
                Header supportedHeader = SipUtils.HEADER_FACTORY.createHeader(SupportedHeader.NAME,
                        "timer");
                reInvite.addHeader(supportedHeader);

                // Set Session-Timer headers
                Header sessionExpiresHeader = SipUtils.HEADER_FACTORY.createHeader(
                        SipUtils.HEADER_SESSION_EXPIRES, "" + dialog.getSessionExpireTime());
                reInvite.addHeader(sessionExpiresHeader);
            }

            // Set "rport" (RFC3581)
            ViaHeader viaHeader = (ViaHeader) reInvite.getHeader(ViaHeader.NAME);
            viaHeader.setRPort();

            String remoteInstanceId = dialog.getRemoteSipInstance();
            if (remoteInstanceId != null) {
                // Add remote SIP instance ID
                SipUtils.setRemoteInstanceID(firstInvite.getStackMessage(), remoteInstanceId);
            }

            return new SipRequest(reInvite);

        } catch (ParseException e) {
            throw new PayloadException("Can't create SIP message!", e);

        } catch (SipException e) {
            throw new PayloadException("Can't create SIP message!", e);
        }
    }

    /**
     * Create a SIP RE-INVITE request with content using initial Invite request
     * 
     * @param dialog Dialog path SIP request
     * @param featureTags featureTags to set in request
     * @param content sdp content
     * @return SIP request
     * @throws PayloadException
     */
    public static SipRequest createReInvite(SipDialogPath dialog, String[] featureTags,
            String content) throws PayloadException {
        try {
            // Build the request
            Request reInvite = dialog.getStackDialog().createRequest(Request.INVITE);
            SipRequest firstInvite = dialog.getInvite();

            // Set the CSeq header
            CSeqHeader cseqHeader = SipUtils.HEADER_FACTORY.createCSeqHeader(dialog.getCseq(),
                    Request.INVITE);
            reInvite.removeHeader(CSeqHeader.NAME);
            reInvite.addHeader(cseqHeader);

            // Set Contact header
            reInvite.removeHeader(ContactHeader.NAME);
            reInvite.removeHeader(SipUtils.HEADER_ACCEPT_CONTACT);
            reInvite.addHeader(dialog.getSipStack().getContact());

            // Set feature tags
            SipUtils.setFeatureTags(reInvite, featureTags);

            String remoteInstanceId = dialog.getRemoteSipInstance();
            if (remoteInstanceId != null) {
                // Add remote SIP instance ID
                SipUtils.setRemoteInstanceID(firstInvite.getStackMessage(), remoteInstanceId);
            }

            // Set Allow header
            SipUtils.buildAllowHeader(reInvite);

            // Set the Route header
            if (reInvite.getHeader(RouteHeader.NAME) == null
                    && firstInvite.getHeader(RouteHeader.NAME) != null) {
                reInvite.addHeader(firstInvite.getHeader(RouteHeader.NAME));
            }

            // Set the P-Preferred-Identity header
            if (firstInvite.getHeader(SipUtils.HEADER_P_PREFERRED_IDENTITY) != null) {
                reInvite.addHeader(firstInvite.getHeader(SipUtils.HEADER_P_PREFERRED_IDENTITY));
            } else {
                setPPreferedIdentityHeader(reInvite);
            }

            // Set User-Agent header
            reInvite.addHeader(firstInvite.getHeader(UserAgentHeader.NAME));

            // Add session timer management
            if (dialog.getSessionExpireTime() >= SessionTimerManager.MIN_EXPIRE_PERIOD) {
                // Set the Supported header
                Header supportedHeader = SipUtils.HEADER_FACTORY.createHeader(SupportedHeader.NAME,
                        "timer");
                reInvite.addHeader(supportedHeader);

                // Set Session-Timer headers
                Header sessionExpiresHeader = SipUtils.HEADER_FACTORY.createHeader(
                        SipUtils.HEADER_SESSION_EXPIRES, "" + dialog.getSessionExpireTime());
                reInvite.addHeader(sessionExpiresHeader);
            }

            // Set "rport" (RFC3581)
            ViaHeader viaHeader = (ViaHeader) reInvite.getHeader(ViaHeader.NAME);
            viaHeader.setRPort();

            // Create the content type and set content
            ContentTypeHeader contentType = SipUtils.HEADER_FACTORY.createContentTypeHeader(
                    "application", "sdp");
            reInvite.setContent(content, contentType);

            // Set the content length
            reInvite.setContentLength(SipUtils.HEADER_FACTORY.createContentLengthHeader(content
                    .getBytes(UTF8).length));
            return new SipRequest(reInvite);

        } catch (ParseException e) {
            throw new PayloadException(
                    new StringBuilder("Can't create SIP message with content : ").append(content)
                            .toString(), e);

        } catch (SipException e) {
            throw new PayloadException(
                    new StringBuilder("Can't create SIP message with content : ").append(content)
                            .toString(), e);
        }

    }

    /**
     * Create a SIP response for RE-INVITE request
     * 
     * @param dialog Dialog path SIP request
     * @param request SIP request
     * @return SIP response
     * @throws PayloadException
     */
    public static SipResponse create200OkReInviteResponse(SipDialogPath dialog, SipRequest request)
            throws PayloadException {
        try {
            // Create the response
            Response response = SipUtils.MSG_FACTORY.createResponse(200, request.getStackMessage());

            // Set Contact header
            response.addHeader(dialog.getSipStack().getContact());

            // Set the Server header
            response.addHeader(SipUtils.buildServerHeader());

            // Set the Require header
            Header requireHeader = SipUtils.HEADER_FACTORY
                    .createHeader(RequireHeader.NAME, "timer");
            response.addHeader(requireHeader);

            // Add Session-Timer header
            Header sessionExpiresHeader = request.getHeader(SipUtils.HEADER_SESSION_EXPIRES);
            if (sessionExpiresHeader != null) {
                response.addHeader(sessionExpiresHeader);
            }

            SipResponse resp = new SipResponse(response);
            resp.setStackTransaction(request.getStackTransaction());
            return resp;

        } catch (ParseException e) {
            throw new PayloadException("Can't create response for re-invite!", e);
        }
    }

    /**
     * Create a SIP response for RE-INVITE request
     * 
     * @param dialog Dialog path SIP request
     * @param request SIP request
     * @param featureTags featureTags to set in request
     * @param content SDP content
     * @return SIP response
     * @throws PayloadException
     */
    public static SipResponse create200OkReInviteResponse(SipDialogPath dialog, SipRequest request,
            String[] featureTags, String content) throws PayloadException {
        try {
            // Create the response
            Response response = SipUtils.MSG_FACTORY.createResponse(200, request.getStackMessage());

            // Set the local tag
            ToHeader to = (ToHeader) response.getHeader(ToHeader.NAME);
            to.setTag(dialog.getLocalTag());

            // Set Contact header
            response.addHeader(dialog.getSipStack().getContact());

            // Set feature tags
            SipUtils.setFeatureTags(response, featureTags);

            // Set Allow header
            SipUtils.buildAllowHeader(response);

            // Set the Server header
            response.addHeader(SipUtils.buildServerHeader());

            // Add session timer management
            if (dialog.getSessionExpireTime() >= SessionTimerManager.MIN_EXPIRE_PERIOD) {
                // Set the Require header
                Header requireHeader = SipUtils.HEADER_FACTORY.createHeader(RequireHeader.NAME,
                        "timer");
                response.addHeader(requireHeader);

                // Set Session-Timer header
                Header sessionExpiresHeader = SipUtils.HEADER_FACTORY.createHeader(
                        SipUtils.HEADER_SESSION_EXPIRES, dialog.getSessionExpireTime()
                                + ";refresher=" + dialog.getInvite().getSessionTimerRefresher());
                response.addHeader(sessionExpiresHeader);
            }

            // Set the message content
            response.setContent(content,
                    SipUtils.HEADER_FACTORY.createContentTypeHeader("application", "sdp"));

            // Set the message content length
            response.setContentLength(SipUtils.HEADER_FACTORY.createContentLengthHeader(content
                    .getBytes(UTF8).length));

            SipResponse resp = new SipResponse(response);
            resp.setStackTransaction(request.getStackTransaction());
            return resp;

        } catch (ParseException e) {
            throw new PayloadException(new StringBuilder(
                    "Can't create response for re-invite with content : ").append(content)
                    .toString(), e);

        } catch (InvalidArgumentException e) {
            throw new PayloadException(new StringBuilder(
                    "Can't create response for re-invite with content : ").append(content)
                    .toString(), e);
        }
    }

    /**
     * Create a SIP UPDATE request
     * 
     * @param dialog SIP dialog path
     * @return SIP request
     * @throws PayloadException
     */
    public static SipRequest createUpdate(SipDialogPath dialog) throws PayloadException {
        try {
            // Create the request
            Request update = dialog.getStackDialog().createRequest(Request.UPDATE);

            // Set the Supported header
            Header supportedHeader = SipUtils.HEADER_FACTORY.createHeader(SupportedHeader.NAME,
                    "timer");
            update.addHeader(supportedHeader);

            // Add Session-Timer header
            Header sessionExpiresHeader = SipUtils.HEADER_FACTORY.createHeader(
                    SipUtils.HEADER_SESSION_EXPIRES, "" + dialog.getSessionExpireTime());
            update.addHeader(sessionExpiresHeader);

            // Set "rport" (RFC3581)
            ViaHeader viaHeader = (ViaHeader) update.getHeader(ViaHeader.NAME);
            viaHeader.setRPort();

            return new SipRequest(update);

        } catch (ParseException e) {
            throw new PayloadException("Can't create SIP message!", e);

        } catch (SipException e) {
            throw new PayloadException("Can't create SIP message!", e);
        }
    }

    /**
     * Create a SIP response for UPDATE request
     * 
     * @param dialog Dialog path SIP request
     * @param request SIP request
     * @return SIP response
     * @throws PayloadException
     */
    public static SipResponse create200OkUpdateResponse(SipDialogPath dialog, SipRequest request)
            throws PayloadException {
        try {
            // Create the response
            Response response = SipUtils.MSG_FACTORY.createResponse(200, request.getStackMessage());

            // Set Contact header
            response.addHeader(dialog.getSipStack().getContact());

            // Set the Server header
            response.addHeader(SipUtils.buildServerHeader());

            // Set the Require header
            Header requireHeader = SipUtils.HEADER_FACTORY
                    .createHeader(RequireHeader.NAME, "timer");
            response.addHeader(requireHeader);

            // Add Session-Timer header
            Header sessionExpiresHeader = request.getHeader(SipUtils.HEADER_SESSION_EXPIRES);
            if (sessionExpiresHeader != null) {
                response.addHeader(sessionExpiresHeader);
            }

            SipResponse resp = new SipResponse(response);
            resp.setStackTransaction(request.getStackTransaction());
            return resp;

        } catch (ParseException e) {
            throw new PayloadException("Can't create SIP message!", e);
        }
    }

    private static void setPPreferedIdentityHeader(Request request) throws ParseException {
        Uri preferedUri = ImsModule.getImsUserProfile().getPreferredUri();
        if (preferedUri != null) {
            Header prefHeader = SipUtils.HEADER_FACTORY.createHeader(
                    SipUtils.HEADER_P_PREFERRED_IDENTITY, preferedUri.toString());
            request.addHeader(prefHeader);
        }
    }

}
