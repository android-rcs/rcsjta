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
package com.orangelabs.rcs.core.ims.service.im.chat;

import static com.orangelabs.rcs.utils.StringUtils.UTF8;
import static com.orangelabs.rcs.utils.StringUtils.UTF8_STR;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import javax2.sip.header.ContactHeader;
import javax2.sip.header.ExtensionHeader;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.text.TextUtils;

import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.sip.FeatureTags;
import com.orangelabs.rcs.core.ims.network.sip.Multipart;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.cpim.CpimParser;
import com.orangelabs.rcs.core.ims.service.im.chat.geoloc.GeolocInfoDocument;
import com.orangelabs.rcs.core.ims.service.im.chat.geoloc.GeolocInfoParser;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnParser;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnUtils;
import com.orangelabs.rcs.core.ims.service.im.chat.iscomposing.IsComposingInfo;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpResumeInfo;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpResumeInfoParser;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.ContactUtils;
import com.orangelabs.rcs.utils.DateUtils;
import com.orangelabs.rcs.utils.IdGenerator;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Chat utility functions
 *
 * @author jexa7410
 */
public class ChatUtils {
	/**
	 * Anonymous URI
	 */
	public final static String ANOMYNOUS_URI = "sip:anonymous@anonymous.invalid";

	/**
	 * Contribution ID header
	 */
	public static final String HEADER_CONTRIBUTION_ID = "Contribution-ID";

	/**
	 * CRLF constant
	 */
	private static final String CRLF = "\r\n";

	 /**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(ChatUtils.class.getName());

	/**
	 * Get supported feature tags for a group chat
	 *
	 * @return List of tags
	 */
	public static List<String> getSupportedFeatureTagsForGroupChat() {
		List<String> tags = new ArrayList<String>();
		tags.add(FeatureTags.FEATURE_OMA_IM);

        List<String> additionalRcseTags = new ArrayList<String>();
		if (RcsSettings.getInstance().isGeoLocationPushSupported()) {
        	additionalRcseTags.add(FeatureTags.FEATURE_RCSE_GEOLOCATION_PUSH);
        }
        if (RcsSettings.getInstance().isFileTransferSupported()) {
        	additionalRcseTags.add(FeatureTags.FEATURE_RCSE_FT);
        }
        if (RcsSettings.getInstance().isFileTransferHttpSupported()) {
        	additionalRcseTags.add(FeatureTags.FEATURE_RCSE_FT_HTTP);
        }
        if (RcsSettings.getInstance().isFileTransferStoreForwardSupported()) {
        	additionalRcseTags.add(FeatureTags.FEATURE_RCSE_FT_SF);
        }
        if (!additionalRcseTags.isEmpty()) {
            tags.add(FeatureTags.FEATURE_RCSE + "=\"" + TextUtils.join(",", additionalRcseTags) + "\"");
        }

        return tags;
    }

    /**
     * Get Accept-Contact tags for a group chat
     *
     * @return List of tags
     */
    public static List<String> getAcceptContactTagsForGroupChat() {
        List<String> tags = new ArrayList<String>();
        tags.add(FeatureTags.FEATURE_OMA_IM);
        return tags;
	}

	/**
	 * Get supported feature tags for a chat
	 *
	 * @return List of tags
	 */
	public static List<String> getSupportedFeatureTagsForChat() {
		List<String> tags = new ArrayList<String>();
		tags.add(FeatureTags.FEATURE_OMA_IM);

        List<String> additionalRcseTags = new ArrayList<String>();
		if (RcsSettings.getInstance().isGeoLocationPushSupported()) {
        	additionalRcseTags.add(FeatureTags.FEATURE_RCSE_GEOLOCATION_PUSH);
        }
        if (RcsSettings.getInstance().isFileTransferSupported()) {
        	additionalRcseTags.add(FeatureTags.FEATURE_RCSE_FT);
        }
        if (RcsSettings.getInstance().isFileTransferHttpSupported()) {
        	additionalRcseTags.add(FeatureTags.FEATURE_RCSE_FT_HTTP);
        }
        if (RcsSettings.getInstance().isFileTransferStoreForwardSupported()) {
        	additionalRcseTags.add(FeatureTags.FEATURE_RCSE_FT_SF);
        }
        if (!additionalRcseTags.isEmpty()) {
            tags.add(FeatureTags.FEATURE_RCSE + "=\"" + TextUtils.join(",", additionalRcseTags) + "\"");
        }

	    return tags;
	}

	/**
	 * Get contribution ID
	 *
	 * @return String
	 */
	public static String getContributionId(SipRequest request) {
		ExtensionHeader contribHeader = (ExtensionHeader)request.getHeader(ChatUtils.HEADER_CONTRIBUTION_ID);
		if (contribHeader != null) {
			return contribHeader.getValue();
		} else {
			return null;
		}
	}

	/**
	 * Is a group chat session invitation
	 *
	 * @param request Request
	 * @return Boolean
	 */
	public static boolean isGroupChatInvitation(SipRequest request) {
        ContactHeader contactHeader = (ContactHeader)request.getHeader(ContactHeader.NAME);
		String param = contactHeader.getParameter("isfocus");
		if (param != null) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Get referred identity as a ContactId
	 *
	 * @param request SIP request
	 * @return ContactId
	 * @throws RcsContactFormatException
	 */
	public static ContactId getReferredIdentityAsContactId(SipRequest request) throws RcsContactFormatException {
		try {
			// Use the Referred-By header
			return ContactUtils.createContactId(SipUtils.getReferredByHeader(request));
		} catch (Exception e) {
			// Use the Asserted-Identity header if parsing of Referred-By header failed
			return ContactUtils.createContactId(SipUtils.getAssertedIdentity(request));
		}
	}

	/**
	 * Get referred identity as a contact URI
	 *
	 * @param request SIP request
	 * @return SIP URI
	 */
	public static String getReferredIdentityAsContactUri(SipRequest request) {
		String referredBy = SipUtils.getReferredByHeader(request);
		if (referredBy != null) {
			// Use the Referred-By header
			return referredBy;
		} else {
			// Use the Asserted-Identity header
			return SipUtils.getAssertedIdentity(request);
		}
	}

	/**
     * Is a plain text type
     *
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isTextPlainType(String mime) {
        return mime != null && mime.toLowerCase().startsWith(MimeType.TEXT_MESSAGE);
    }

    /**
     * Is a composing event type
     *
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isApplicationIsComposingType(String mime) {
        return mime != null && mime.toLowerCase().startsWith(IsComposingInfo.MIME_TYPE);
    }

    /**
     * Is a CPIM message type
     *
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isMessageCpimType(String mime) {
        return mime != null && mime.toLowerCase().startsWith(CpimMessage.MIME_TYPE);
    }

    /**
     * Is an IMDN message type
     *
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isMessageImdnType(String mime) {
        return mime != null && mime.toLowerCase().startsWith(ImdnDocument.MIME_TYPE);
    }

    /**
     * Is a geolocation event type
     *
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isGeolocType(String mime) {
		return mime != null && mime.toLowerCase().startsWith(GeolocInfoDocument.MIME_TYPE);
    }

    /**
     * Generate resource-list for a chat session
     *
     * @param participants Set of participants
     * @return XML document
     */
    public static String generateChatResourceList(Set<ContactId> participants) {
        StringBuilder resources = new StringBuilder("<?xml version=\"1.0\" encoding=\"")
                .append(UTF8_STR).append("\"?>").append(CRLF)
                .append("<resource-lists xmlns=\"urn:ietf:params:xml:ns:resource-lists\" ")
                .append("xmlns:cp=\"urn:ietf:params:xml:ns:copycontrol\">").append("<list>")
                .append(CRLF);
        for (ContactId contact : participants) {
            resources.append(" <entry uri=\"").append(PhoneUtils.formatContactIdToUri(contact))
                    .append("\" cp:copyControl=\"to\"/>").append(CRLF);
        }
        return resources.append("</list></resource-lists>").toString();
    }

    /**
     * Is IMDN service
     *
     * @param request Request
     * @return Boolean
     */
    public static boolean isImdnService(SipRequest request) {
    	String content = request.getContent();
    	String contentType = request.getContentType();
    	if ((content != null) && (content.contains(ImdnDocument.IMDN_NAMESPACE)) &&
    			(contentType != null) && (contentType.equalsIgnoreCase(CpimMessage.MIME_TYPE))) {
    		return true;
    	} else {
    		return false;
    	}
    }

    /**
     * Is IMDN notification "delivered" requested
     *
     * @param request Request
     * @return Boolean
     */
    public static boolean isImdnDeliveredRequested(SipRequest request) {
    	boolean result = false;
		try {
			// Read ID from multipart content
		    String content = request.getContent();
			int index = content.indexOf(ImdnUtils.HEADER_IMDN_DISPO_NOTIF);
			if (index != -1) {
				index = index+ImdnUtils.HEADER_IMDN_DISPO_NOTIF.length()+1;
				String part = content.substring(index);
				String notif = part.substring(0, part.indexOf(CRLF));
		    	if (notif.indexOf(ImdnDocument.POSITIVE_DELIVERY) != -1) {
		    		result = true;
		    	}
			}
		} catch(Exception e) {
			result = false;
		}
		return result;
    }

    /**
     * Is IMDN notification "displayed" requested
     *
     * @param request Request
     * @return Boolean
     */
    public static boolean isImdnDisplayedRequested(SipRequest request) {
    	boolean result = false;
		try {
			// Read ID from multipart content
		    String content = request.getContent();
			int index = content.indexOf(ImdnUtils.HEADER_IMDN_DISPO_NOTIF);
			if (index != -1) {
				index = index+ImdnUtils.HEADER_IMDN_DISPO_NOTIF.length()+1;
				String part = content.substring(index);
				String notif = part.substring(0, part.indexOf(CRLF));
		    	if (notif.indexOf(ImdnDocument.DISPLAY) != -1) {
		    		result = true;
		    	}
			}
		} catch(Exception e) {
			result = false;
		}
		return result;
    }

	/**
	 * Returns the message ID from a SIP request
	 *
     * @param request Request
	 * @return Message ID
	 */
	public static String getMessageId(SipRequest request) {
		String result = null;
		try {
			// Read ID from multipart content
		    String content = request.getContent();
			int index = content.indexOf(ImdnUtils.HEADER_IMDN_MSG_ID);
			if (index != -1) {
				index = index+ImdnUtils.HEADER_IMDN_MSG_ID.length()+1;
				String part = content.substring(index);
				String msgId = part.substring(0, part.indexOf(CRLF));
				result = msgId.trim();
			}
		} catch(Exception e) {
			result = null;
		}
		return result;
	}

    /**
     * Format to a SIP-URI for CPIM message
     *
     * @param input Input
     * @return SIP-URI
     */
    private static String formatCpimSipUri(String input) {
    	input = input.trim();

    	if (input.startsWith("<")) {
    		// Already a SIP-URI format
    		return input;
    	}

    	// It's already a SIP address with display name
		if (input.startsWith("\"")) {
			return input;
		}

    	if (input.startsWith("sip:") || input.startsWith("tel:")) {
    		// Just add URI delimiter
    		return "<" + input + ">";
    	} else {
    		// It's a number, format it
    		return "<" + PhoneUtils.formatNumberToSipUri(input) + ">";
    	}
    }

	/**
	 * Build a CPIM message
	 *
	 * @param from From
	 * @param to To
	 * @param content Content
	 * @param contentType Content type
	 * @return String
	 */
    public static String buildCpimMessage(String from, String to, String content, String contentType) {
        return new StringBuilder(CpimMessage.HEADER_FROM).append(": ")
                .append(ChatUtils.formatCpimSipUri(from)).append(CRLF)
                .append(CpimMessage.HEADER_TO).append(": ").append(ChatUtils.formatCpimSipUri(to))
                .append(CRLF).append(CpimMessage.HEADER_DATETIME).append(": ")
                .append(DateUtils.encodeDate(System.currentTimeMillis())).append(CRLF).append(CRLF)
                .append(CpimMessage.HEADER_CONTENT_TYPE).append(": ").append(contentType)
                .append(";charset=").append(UTF8_STR).append(CRLF).append(CRLF)
                .append(content).toString();
    }

	/**
	 * Build a CPIM message with full IMDN headers
	 *
	 * @param from From URI
	 * @param to To URI
	 * @param messageId Message ID
	 * @param content Content
	 * @param contentType Content type
	 * @return String
	 */
	public static String buildCpimMessageWithImdn(String from, String to, String messageId,
			String content, String contentType) {
        return new StringBuilder(CpimMessage.HEADER_FROM).append(": ")
                .append(ChatUtils.formatCpimSipUri(from)).append(CRLF)
                .append(CpimMessage.HEADER_TO).append(": ").append(ChatUtils.formatCpimSipUri(to))
                .append(CRLF).append(CpimMessage.HEADER_NS).append(": ")
                .append(ImdnDocument.IMDN_NAMESPACE).append(CRLF)
                .append(ImdnUtils.HEADER_IMDN_MSG_ID).append(": ").append(messageId).append(CRLF)
                .append(CpimMessage.HEADER_DATETIME).append(": ")
                .append(DateUtils.encodeDate(System.currentTimeMillis())).append(CRLF)
                .append(ImdnUtils.HEADER_IMDN_DISPO_NOTIF).append(": ")
                .append(ImdnDocument.POSITIVE_DELIVERY).append(", ").append(ImdnDocument.DISPLAY)
                .append(CRLF).append(CRLF).append(CpimMessage.HEADER_CONTENT_TYPE).append(": ")
                .append(contentType).append(";charset=").append(UTF8_STR).append(CRLF)
                .append(CpimMessage.HEADER_CONTENT_LENGTH).append(": ")
                .append(content.getBytes(UTF8).length).append(CRLF)
                .append(CRLF).append(content).toString();
	}

	/**
	 * Build a CPIM message with IMDN delivered header
	 *
	 * @param from From URI
	 * @param to To URI
	 * @param messageId Message ID
	 * @param content Content
	 * @param contentType Content type
	 * @return String
	 */
	public static String buildCpimMessageWithDeliveredImdn(String from, String to,
			String messageId, String content, String contentType) {
        return new StringBuilder(CpimMessage.HEADER_FROM).append(": ")
                .append(ChatUtils.formatCpimSipUri(from)).append(CRLF)
                .append(CpimMessage.HEADER_TO).append(": ").append(ChatUtils.formatCpimSipUri(to))
                .append(CRLF).append(CpimMessage.HEADER_NS).append(": ")
                .append(ImdnDocument.IMDN_NAMESPACE).append(CRLF)
                .append(ImdnUtils.HEADER_IMDN_MSG_ID).append(": ").append(messageId).append(CRLF)
                .append(CpimMessage.HEADER_DATETIME).append(": ")
                .append(DateUtils.encodeDate(System.currentTimeMillis())).append(CRLF)
                .append(ImdnUtils.HEADER_IMDN_DISPO_NOTIF).append(": ")
                .append(ImdnDocument.POSITIVE_DELIVERY).append(CRLF).append(CRLF)
                .append(CpimMessage.HEADER_CONTENT_TYPE).append(": ").append(contentType)
                .append(";charset=").append(UTF8_STR).append(CRLF)
                .append(CpimMessage.HEADER_CONTENT_LENGTH).append(": ")
                .append(content.getBytes(UTF8).length).append(CRLF)
                .append(CRLF).append(content).toString();
	}

	/**
	 * Build a CPIM delivery report
	 *
	 * @param from From
	 * @param to To
	 * @param imdn IMDN report
	 * @return String
	 */
	public static String buildCpimDeliveryReport(String from, String to, String imdn) {
		return new StringBuilder(CpimMessage.HEADER_FROM).append(": ")
				.append(ChatUtils.formatCpimSipUri(from)).append(CRLF)
				.append(CpimMessage.HEADER_TO).append(": ").append(ChatUtils.formatCpimSipUri(to))
				.append(CRLF).append(CpimMessage.HEADER_NS).append(": ")
				.append(ImdnDocument.IMDN_NAMESPACE).append(CRLF)
				.append(ImdnUtils.HEADER_IMDN_MSG_ID).append(": ")
				.append(IdGenerator.generateMessageID()).append(CRLF)
				.append(CpimMessage.HEADER_DATETIME).append(": ")
				.append(DateUtils.encodeDate(System.currentTimeMillis())).append(CRLF).append(CRLF)
				.append(CpimMessage.HEADER_CONTENT_TYPE).append(": ")
				.append(ImdnDocument.MIME_TYPE).append(CRLF)
				.append(CpimMessage.HEADER_CONTENT_DISPOSITION).append(": ")
				.append(ImdnDocument.NOTIFICATION).append(CRLF)
				.append(CpimMessage.HEADER_CONTENT_LENGTH).append(": ")
				.append(imdn.getBytes(UTF8).length)
				.append(CRLF).append(CRLF).append(imdn).toString();
	}

	/**
	 * Parse a CPIM delivery report
	 *
	 * @param cpim CPIM document
	 * @return IMDN document
	 * @throws Exception
	 */
	public static ImdnDocument parseCpimDeliveryReport(String cpim) throws Exception {
		ImdnDocument imdn = null;
		// Parse CPIM document
		CpimParser cpimParser = new CpimParser(cpim);
		CpimMessage cpimMsg = cpimParser.getCpimMessage();
		if (cpimMsg != null) {
			// Check if the content is a IMDN message
			String contentType = cpimMsg.getContentType();
			if ((contentType != null) && ChatUtils.isMessageImdnType(contentType)) {
				// Parse the IMDN document
				imdn = parseDeliveryReport(cpimMsg.getMessageContent());
			}
		}
		return imdn;
	}

	/**
	 * Parse a delivery report
	 *
	 * @param xml XML document
	 * @return IMDN document
	 */
	public static ImdnDocument parseDeliveryReport(String xml) {
		try {
			InputSource input = new InputSource(new ByteArrayInputStream(
					xml.getBytes()));
			ImdnParser parser = new ImdnParser(input);
			return parser.getImdnDocument();
    	} catch(Exception e) {
    		return null;
    	}
	}

	/**
	 * Build a delivery report
	 *
	 * @param msgId Message ID
	 * @param status Status
	 * @return XML document
	 */
	public static String buildDeliveryReport(String msgId, String status) {
		String method;
		if (status.equals(ImdnDocument.DELIVERY_STATUS_DISPLAYED)) {
			method = "display-notification";
		} else
		if (status.equals(ImdnDocument.DELIVERY_STATUS_DELIVERED)) {
			method = "delivery-notification";
		} else {
			method = "processing-notification";
		}

		return new StringBuilder("<?xml version=\"1.0\" encoding=\"")
				.append(UTF8_STR).append("\"?>").append(CRLF)
				.append("<imdn xmlns=\"urn:ietf:params:xml:ns:imdn\">").append(CRLF)
				.append("<message-id>").append(msgId).append("</message-id>").append(CRLF)
				.append("<datetime>").append(DateUtils.encodeDate(System.currentTimeMillis()))
				.append("</datetime>").append(CRLF).append("<").append(method).append("><status><")
				.append(status).append("/></status></").append(method).append(">").append(CRLF)
				.append("</imdn>").toString();
	}

	/**
	* Build a geoloc document
	*
	* @param geoloc Geolocation
	* @param contact Contact
	* @param msgId Message ID
	* @return XML document
	*/
	public static String buildGeolocDocument(Geoloc geoloc, String contact, String msgId) {
		String expire = DateUtils.encodeDate(geoloc.getExpiration());
		return new StringBuilder("<?xml version=\"1.0\" encoding=\"")
				.append(UTF8_STR).append("\"?>").append(CRLF)
				.append("<rcsenvelope xmlns=\"urn:gsma:params:xml:ns:rcs:rcs:geolocation\"")
				.append(" xmlns:rpid=\"urn:ietf:params:xml:ns:pidf:rpid\"")
				.append(" xmlns:gp=\"urn:ietf:params:xml:ns:pidf:geopriv10\"")
				.append(" xmlns:gml=\"http://www.opengis.net/gml\"")
				.append(" xmlns:gs=\"http://www.opengis.net/pidflo/1.0\"").append(" entity=\"")
				.append(contact).append("\">").append(CRLF).append("<rcspushlocation id=\"")
				.append(msgId).append("\" label=\"").append(geoloc.getLabel()).append("\" >")
				.append("<rpid:place-type rpid:until=\"").append(expire).append("\">")
				.append("</rpid:place-type>").append(CRLF)
				.append("<rpid:time-offset rpid:until=\"").append(expire)
				.append("\"></rpid:time-offset>").append(CRLF).append("<gp:geopriv>").append(CRLF)
				.append("<gp:location-info>").append(CRLF)
				.append("<gs:Circle srsName=\"urn:ogc:def:crs:EPSG::4326\">").append(CRLF)
				.append("<gml:pos>").append(geoloc.getLatitude()).append(" ")
				.append(geoloc.getLongitude()).append("</gml:pos>").append(CRLF)
				.append("<gs:radius uom=\"urn:ogc:def:uom:EPSG::9001\">")
				.append(geoloc.getAccuracy()).append("</gs:radius>").append(CRLF)
				.append("</gs:Circle>").append(CRLF).append("</gp:location-info>").append(CRLF)
				.append("<gp:usage-rules>").append(CRLF).append("<gp:retention-expiry>")
				.append(expire).append("</gp:retention-expiry>").append(CRLF)
				.append("</gp:usage-rules>").append(CRLF).append("</gp:geopriv>").append(CRLF)
				.append("<timestamp>").append(DateUtils.encodeDate(System.currentTimeMillis()))
				.append("</timestamp>").append(CRLF).append("</rcspushlocation>").append(CRLF)
				.append("</rcsenvelope>").append(CRLF).toString();
	}

	/**
	 * Parse a geoloc document
	 *
	 * @param xml XML document
	 * @return Geolocation
	 * @throws IllegalArgumentException
	 */
	public static Geoloc parseGeolocDocument(String xml) {
		try {
			InputSource geolocInput = new InputSource(new ByteArrayInputStream(
					xml.getBytes(UTF8)));
			GeolocInfoParser geolocParser = new GeolocInfoParser(geolocInput);
			GeolocInfoDocument geolocDocument = geolocParser.getGeoLocInfo();
			if (geolocDocument == null) {
				throw new IllegalArgumentException("Unable to parse geoloc document!");
			}
			Geoloc geoloc = new Geoloc(geolocDocument.getLabel(), geolocDocument.getLatitude(),
					geolocDocument.getLongitude(), geolocDocument.getExpiration(),
					geolocDocument.getRadius());
			return geoloc;
		} catch (ParserConfigurationException e) {
			throw new IllegalArgumentException("Unable to parse geoloc document!", e);
		} catch (SAXException e) {
			throw new IllegalArgumentException("Unable to parse geoloc document!", e);
		} catch (IOException e) {
			throw new IllegalArgumentException("Unable to parse geoloc document!", e);
		}
	}

	/**
	 * Parse a file transfer resume info
	 *
	 * @param xml XML document
	 * @return File transfer resume info
	 */
	public static FileTransferHttpResumeInfo parseFileTransferHttpResumeInfo(byte[] xml) {
		try {
		    InputSource ftHttpInput = new InputSource(new ByteArrayInputStream(xml));
		    FileTransferHttpResumeInfoParser ftHttpParser = new FileTransferHttpResumeInfoParser(ftHttpInput);
		    return ftHttpParser.getResumeInfo();
		} catch(Exception e) {
			return null;
		}
	}

	/**
	 * Create a text message
	 *
	 * @param remote Remote contact identifier
	 * @param msg Text message
	 * @return Text message
	 */
	public static ChatMessage createTextMessage(ContactId remote, String msg) {
		String msgId = IdGenerator.generateMessageID();
		return new ChatMessage(msgId, remote, msg, MimeType.TEXT_MESSAGE, null, null);
	}

	/**
	 * Create a file transfer message
	 *
	 * @param remote Remote contact identifier
	 * @param fileInfo File XML description
	 * @param imdn IMDN flag
	 * @param msgId Message ID
	 * @return File message
	 */
	public static ChatMessage createFileTransferMessage(ContactId remote, String fileInfo,
			boolean imdn, String msgId) {
		return new ChatMessage(msgId, remote, fileInfo, FileTransferHttpInfoDocument.MIME_TYPE,
				null, null);
	}

	/**
	 * Create a geoloc message
	 *
	 * @param remote Remote contact
	 * @param geoloc Geolocation
	 * @return Geolocation message
	 */
	public static ChatMessage createGeolocMessage(ContactId remote, Geoloc geoloc) {
		String msgId = IdGenerator.generateMessageID();
		String geolocContent = ChatUtils.buildGeolocDocument(geoloc,
				ImsModule.IMS_USER_PROFILE.getPublicUri(), msgId);
		return new ChatMessage(msgId, remote, geolocContent, GeolocInfoDocument.MIME_TYPE, null, null);
	}

	/**
	 * Get the first message
	 *
	 * @param invite Request
	 * @return First message
	 */
	public static ChatMessage getFirstMessage(SipRequest invite) {
		ChatMessage msg = getFirstMessageFromCpim(invite);
		if (msg != null) {
			return msg;
		} else {
			return getFirstMessageFromSubject(invite);
		}
	}

	/**
	 * Get the subject
	 *
	 * @param invite Request
	 * @return String
	 */
	public static String getSubject(SipRequest invite) {
		return invite.getSubject();
	}

	/**
	 * Get the first message from CPIM content
	 *
	 * @param invite Request
	 * @return First message
	 */
	private static ChatMessage getFirstMessageFromCpim(SipRequest invite) {
		CpimMessage cpimMsg = ChatUtils.extractCpimMessage(invite);
		if (cpimMsg == null) {
			return null;
		}
		ContactId remote = null;
		try {
			remote = ChatUtils.getReferredIdentityAsContactId(invite);
		} catch (RcsContactFormatException e) {
			if (logger.isActivated()) {
				logger.warn("getFirstMessageFromCpim: cannot parse contact");
			}
			return null;
		}
		String msgId = ChatUtils.getMessageId(invite);
		String content = cpimMsg.getMessageContent();
		Date date = cpimMsg.getMessageDate();
		String mime = cpimMsg.getContentType();
		if (msgId == null || content == null || mime == null) {
			return null;
		}
		if (ChatUtils.isGeolocType(mime)) {
			return new ChatMessage(msgId, remote, content, GeolocInfoDocument.MIME_TYPE, date, null);
		} else if (FileTransferUtils.isFileTransferHttpType(mime)) {
			return new ChatMessage(msgId, remote, content, FileTransferHttpInfoDocument.MIME_TYPE,
					date, null);
		} else if (ChatUtils.isTextPlainType(mime)) {
			return new ChatMessage(msgId, remote, content, MimeType.TEXT_MESSAGE, date, null);
		}
		logger.warn(new StringBuilder("Unknown MIME-type in first message; msgId=").append(msgId)
				.append(", mime='").append(mime).append("'.").toString());
		return null;
	}

	/**
	 * Get the first message from the Subject header
	 *
	 * @param invite Request
	 * @return First message
	 */
	private static ChatMessage getFirstMessageFromSubject(SipRequest invite) {
		String subject = invite.getSubject();
		if (TextUtils.isEmpty(subject)) {
			return null;
		}
		ContactId remote = null;
		try {
			remote = ChatUtils.getReferredIdentityAsContactId(invite);
		} catch (RcsContactFormatException e) {
			if (logger.isActivated()) {
				logger.debug("getFirstMessageFromSubject: cannot parse contact");
			}
			return null;
		}
		return new ChatMessage(IdGenerator.generateMessageID(), remote, subject,
				MimeType.TEXT_MESSAGE, new Date(), null);
	}

    /**
     * Extract CPIM message from incoming INVITE request
     *
     * @param request Request
     * @return Boolean
     */
    public static CpimMessage extractCpimMessage(SipRequest request) {
    	CpimMessage message = null;
		try {
			// Extract message from content/CPIM
		    String content = request.getContent();
		    String boundary = request.getBoundaryContentType();
			Multipart multi = new Multipart(content, boundary);
		    if (multi.isMultipart()) {
		    	String cpimPart = multi.getPart(CpimMessage.MIME_TYPE);
		    	if (cpimPart != null) {
					// CPIM part
	    			message = new CpimParser(cpimPart.getBytes(UTF8)).getCpimMessage();
		    	}
		    }
		} catch(Exception e) {
			message = null;
		}
		return message;
    }

    /**
     * Get list of participants from 'resource-list' present in XML document and
     * include the 'remote' as participant.
     *
     * @param request Request
     * @return {@link Set<ParticipantInfo>} participant list
     */
	public static Set<ParticipantInfo> getListOfParticipants(SipRequest request)  {
		Set<ParticipantInfo> participants = new HashSet<ParticipantInfo>();
		String content = request.getContent();
		String boundary = request.getBoundaryContentType();
		Multipart multi = new Multipart(content, boundary);
		if (multi.isMultipart()) {
			// Extract resource-lists
			String listPart = multi.getPart("application/resource-lists+xml");
			if (listPart != null) {
				// Create list from XML
				participants = ParticipantInfoUtils.parseResourceList(listPart);
				try {
					ContactId remote = getReferredIdentityAsContactId(request);
					// Include remote contact if format if correct
					ParticipantInfoUtils.addParticipant(participants, remote);
				} catch (RcsContactFormatException e) {
				}
			}
		}
		return participants;
	}

    /**
     * Is request is for FToHTTP
     *
     * @param request SIP request
     * @return true if FToHTTP
     */
    public static boolean isFileTransferOverHttp(SipRequest request) {
        CpimMessage message = extractCpimMessage(request);
        if (message != null && message.getContentType().startsWith(FileTransferHttpInfoDocument.MIME_TYPE)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Generate persisted MIME-type from network pay-load
     *
     * @param Network pay-load MIME-type
     * @return API MIME-type
     */
    public static String networkMimeTypeToApiMimeType(String networkMimeType) {
        /*
         * Geolocation chat messages does not have the same mimetype in the
         * payload as in the TAPI. Text chat messages do.
         */
        if (ChatUtils.isGeolocType(networkMimeType)) {
            return MimeType.GEOLOC_MESSAGE;
        }
        return networkMimeType;
    }

    /**
     * Generate persisted content from network pay-load
     * @param msg 
     *
     * @return Persisted content
     */
    public static String networkContentToPersistedContent(ChatMessage msg) {
        /*
         * Geolocation chat messages does not have the same mimetype in the
         * payload as in the TAPI. Text chat messages do.
         */
        if (ChatUtils.isGeolocType(msg.getMimeType())) {
            Geoloc geoloc = parseGeolocDocument(msg.getContent());
            return geoloc.toString();
        }
        return msg.getContent();
    }
}
