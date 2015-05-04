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

package com.gsma.rcs.core.ims.protocol.sip;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.ims.network.sip.FeatureTags;
import com.gsma.rcs.core.ims.network.sip.Multipart;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.service.SessionTimerManager;
import com.gsma.rcs.utils.StringUtils;

import android.text.TextUtils;

import gov2.nist.javax2.sip.header.extensions.SessionExpiresHeader;

import java.text.ParseException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Set;

import javax2.sip.Transaction;
import javax2.sip.header.AcceptHeader;
import javax2.sip.header.CSeqHeader;
import javax2.sip.header.CallIdHeader;
import javax2.sip.header.ContactHeader;
import javax2.sip.header.ContentTypeHeader;
import javax2.sip.header.ExtensionHeader;
import javax2.sip.header.FromHeader;
import javax2.sip.header.Header;
import javax2.sip.header.SubjectHeader;
import javax2.sip.header.ToHeader;
import javax2.sip.header.ViaHeader;
import javax2.sip.message.Message;

/**
 * SIP message
 * 
 * @author jexa7410
 * @author Deutsche Telekom AG
 */
public abstract class SipMessage {

    private final String HEADER_OF_NON_BASED_FTAG = "+";
    
    private final String FTAG_VALUE_LIST_EQUAL = "=";
    private final String FTAG_VALUE_LIST_QUOT = "\"";
    private final String FTAG_VALUE_LIST_COMA = ",";
    
    private final String ACCEPT_CONTACT_PARAMS_SEMI = ";";

    /**
     * SIP stack API object
     */
    protected Message mStackMessage;

    private Transaction mStackTransaction;

    /**
     * Constructor
     * 
     * @param message SIP stack message
     */
    public SipMessage(Message message) {
        mStackMessage = message;
    }

    /**
     * Return the SIP stack message
     * 
     * @return SIP message
     */
    public abstract Message getStackMessage();

    /**
     * Return the SIP stack transaction
     * 
     * @return SIP transaction
     */
    public Transaction getStackTransaction() {
        return mStackTransaction;
    }

    /**
     * Set the SIP stack transaction
     * 
     * @param transaction SIP transaction
     */
    public void setStackTransaction(Transaction transaction) {
        mStackTransaction = transaction;
    }

    /**
     * Add a SIP header
     * 
     * @param name Header name
     * @param value Header value
     */
    public void addHeader(String name, String value) {
        try {
            Header header = SipUtils.HEADER_FACTORY.createHeader(name, value);
            mStackMessage.setHeader(header);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Return a header value
     * 
     * @param name Header name
     * @return Header
     */
    public Header getHeader(String name) {
        return mStackMessage.getHeader(name);
    }

    /**
     * Return values of an header
     * 
     * @param name Header name
     * @return List of headers
     */
    public ListIterator<Header> getHeaders(String name) {
        return mStackMessage.getHeaders(name);
    }

    /**
     * Get Via headers list
     * 
     * @return List of headers
     */
    public ListIterator<ViaHeader> getViaHeaders() {
        return mStackMessage.getHeaders(ViaHeader.NAME);
    }

    /**
     * Return the From
     * 
     * @return String
     */
    public String getFrom() {
        FromHeader header = (FromHeader) mStackMessage.getHeader(FromHeader.NAME);
        return header.getAddress().toString();
    }

    /**
     * Return the From tag
     * 
     * @return String
     */
    public String getFromTag() {
        FromHeader header = (FromHeader) mStackMessage.getHeader(FromHeader.NAME);
        return header.getTag();
    }

    /**
     * Return the From URI
     * 
     * @return String
     */
    public String getFromUri() {
        FromHeader header = (FromHeader) mStackMessage.getHeader(FromHeader.NAME);
        return header.getAddress().getURI().toString();
    }

    /**
     * Return the To
     * 
     * @return String
     */
    public String getTo() {
        ToHeader header = (ToHeader) mStackMessage.getHeader(ToHeader.NAME);
        return header.getAddress().toString();
    }

    /**
     * Return the To tag
     * 
     * @return String
     */
    public String getToTag() {
        ToHeader header = (ToHeader) mStackMessage.getHeader(ToHeader.NAME);
        return header.getTag();
    }

    /**
     * Return the To URI
     * 
     * @return String
     */
    public String getToUri() {
        ToHeader header = (ToHeader) mStackMessage.getHeader(ToHeader.NAME);
        return header.getAddress().getURI().toString();
    }

    /**
     * Return the CSeq value
     * 
     * @return Number
     */
    public long getCSeq() {
        CSeqHeader header = (CSeqHeader) mStackMessage.getHeader(CSeqHeader.NAME);
        return header.getSeqNumber();
    }

    /**
     * Return the contact URI
     * 
     * @return String or null
     */
    public String getContactURI() {
        ContactHeader header = (ContactHeader) mStackMessage.getHeader(ContactHeader.NAME);
        if (header != null) {
            return header.getAddress().getURI().toString();
        }
        return null;
    }

    /**
     * Return the content part
     * 
     * @return String or null
     */
    public String getContent() {
        byte[] content = mStackMessage.getRawContent();
        if (content != null) {
            return new String(content, UTF8);
        }
        return null;
    }

    /**
     * Return the raw content part
     * 
     * @return Byte array or null
     */
    public byte[] getRawContent() {
        return mStackMessage.getRawContent();
    }

    /**
     * Return the SDP content part
     * 
     * @return String or null
     */
    public String getSdpContent() {
        String content = getContent();
        if (content == null) {
            return null;
        }

        String contentType = getContentType();
        if (contentType == null) {
            return null;
        }

        if (contentType.startsWith("multipart")) {
            String boundary = getBoundaryContentType();
            Multipart multi = new Multipart(content, boundary);
            return multi.getPart("application/sdp");

        } else if (contentType.equals("application/sdp")) {
            return content;
        }
        return null;
    }

    /**
     * Return the content part as bytes
     * 
     * @return String or null
     */
    public byte[] getContentBytes() {
        return mStackMessage.getRawContent();
    }

    /**
     * Return the content type
     * 
     * @return String or null
     */
    public String getContentType() {
        ContentTypeHeader header = (ContentTypeHeader) mStackMessage
                .getHeader(ContentTypeHeader.NAME);
        if (header != null) {
            return header.getContentType() + "/" + header.getContentSubType();
        }
        return null;
    }

    /**
     * Return the boudary parameter of the content type
     * 
     * @return String or null
     */
    public String getBoundaryContentType() {
        ContentTypeHeader header = (ContentTypeHeader) mStackMessage
                .getHeader(ContentTypeHeader.NAME);
        if (header != null) {
            String value = header.getParameter("boundary");
            if (value != null) {
                // Remove quotes
                value = StringUtils.removeQuotes(value);
            }
            return value;
        }
        return null;
    }

    /**
     * Returns the call-ID value
     * 
     * @return String or null
     */
    public String getCallId() {
        CallIdHeader header = (CallIdHeader) mStackMessage.getHeader(CallIdHeader.NAME);
        if (header != null) {
            return header.getCallId();
        }
        return null;
    }

    /**
     * Returns the subject value
     * 
     * @return String or empty
     */
    public String getSubject() {
        SubjectHeader header = (SubjectHeader) getHeader(SubjectHeader.NAME);
        if (header != null) {
            return header.getSubject();
        }
        return "";
    }

    /**
     * Return the accept type
     * 
     * @return String or null
     */
    public String getAcceptType() {
        AcceptHeader header = (AcceptHeader) getHeader(AcceptHeader.NAME);
        if (header != null) {
            return header.getContentType() + "/" + header.getContentSubType();
        }
        return null;
    }

    private StringBuilder formatFeatureTag(String name, String value) {
        StringBuilder parameter = new StringBuilder(name);
        parameter.append(FTAG_VALUE_LIST_EQUAL);
        parameter.append(FTAG_VALUE_LIST_QUOT);
        parameter.append(value);
        parameter.append(FTAG_VALUE_LIST_QUOT);
        return parameter;
    }

    private void addContactFeatureTags(Set<String> tags, ContactHeader contactHeader) {
        for (Iterator<?> i = contactHeader.getParameterNames(); i.hasNext();) {
            /* Extract parameter name & value */
            String pname = (String) i.next();
            if (!pname.startsWith(HEADER_OF_NON_BASED_FTAG)
                    && !pname.equals(FeatureTags.FEATURE_RCSE_IP_VIDEO_CALL)
                    && !pname.equals(FeatureTags.FEATURE_SIP_AUTOMATA)) {
                /*
                 * Only keep non based feature tags except AUTOMATA and VIDEO_CALL. Reject parameter
                 * not starting with a + except FEATURE_SIP_AUTOMATA and FEATURE_RCSE_IP_VIDEO_CALL
                 * which do not start with '+'
                 */
                continue;
            }
            String pvalue = contactHeader.getParameter(pname);
            if (StringUtils.isEmpty(pvalue)) {
                /* Add single parameter */
                tags.add(pname);
            } else {
                /* Add pair parameters */
                String[] values = pvalue.split(FTAG_VALUE_LIST_COMA);
                for (String tag : values) {
                    tags.add(formatFeatureTag(pname, tag.trim()).toString());
                }
            }
        }
    }

    private void addAcceptContactFeatureTags(Set<String> tags, ListIterator<Header> acceptHeaders) {
        /* Extract header parameters */
        while (acceptHeaders.hasNext()) {
            ExtensionHeader acceptHeader = (ExtensionHeader) acceptHeaders.next();
            String acceptHeaderValue = acceptHeader.getValue();
            String[] parameters = acceptHeaderValue.split(ACCEPT_CONTACT_PARAMS_SEMI);
            for (String parameter : parameters) {
                /* Extract parameter name & value */
                String[] param = parameter.split(FTAG_VALUE_LIST_EQUAL);
                String pname = param[0];
                if (!pname.startsWith(HEADER_OF_NON_BASED_FTAG)) {
                    /* only keep non based feature tags */
                    continue;
                }
                String pvalue = null;
                if (param.length == 2) {
                    pvalue = param[1];
                }
                if (TextUtils.isEmpty(pvalue)) {
                    /* Add single parameter */
                    tags.add(pname);
                } else {
                    /* Add pair parameter */
                    pvalue = pvalue.replace(FTAG_VALUE_LIST_QUOT, "");
                    String[] values = pvalue.split(FTAG_VALUE_LIST_COMA);
                    for (String tag : values) {
                        tags.add(formatFeatureTag(pname, tag.trim()).toString());
                    }
                }
            }
        }
    }

    /**
     * Get the features tags from Contact header and Accept-Contact header
     * 
     * @return Set of feature tags
     */
    public Set<String> getFeatureTags() {
        Set<String> tags = new HashSet<String>();
        /* Read Contact header */
        ContactHeader contactHeader = (ContactHeader) mStackMessage.getHeader(ContactHeader.NAME);
        if (contactHeader != null) {
            addContactFeatureTags(tags, contactHeader);
        }

        /* Read Accept-Contact header */
        ListIterator<Header> acceptHeaders = getHeaders(SipUtils.HEADER_ACCEPT_CONTACT);
        if (acceptHeaders == null || !acceptHeaders.hasNext()) {
            /* Check contracted form */
            acceptHeaders = getHeaders(SipUtils.HEADER_ACCEPT_CONTACT_C);
        }

        if (acceptHeaders != null) {
            addAcceptContactFeatureTags(tags, acceptHeaders);
        }

        Set<String> result = new HashSet<String>();
        /* Filter irrelevant feature tags */
        for (String tag : tags) {
            /* Reject sip.instance parameter */
            if (tag.startsWith(SipUtils.SIP_INSTANCE_PARAM)) {
                continue;
            }
            result.add(tag);
        }
        return result;
    }

    /**
     * Get session timer expire
     * 
     * @return Expire time or -1 if no session timer
     */
    public int getSessionTimerExpire() {
        SessionExpiresHeader sessionExpiresHeader = (SessionExpiresHeader) getHeader(SessionExpiresHeader.NAME);
        if (sessionExpiresHeader != null) {
            return sessionExpiresHeader.getExpires();
        }
        return -1;
    }

    /**
     * Get session timer refresher role
     * 
     * @return "uac" or "uas"
     */
    public String getSessionTimerRefresher() {
        String role = null;
        SessionExpiresHeader sessionExpiresHeader = (SessionExpiresHeader) getHeader(SessionExpiresHeader.NAME);
        if (sessionExpiresHeader != null) {
            role = sessionExpiresHeader.getRefresher();
        }
        if (role == null) {
            return SessionTimerManager.UAC_ROLE;
        }
        return role;
    }
}
