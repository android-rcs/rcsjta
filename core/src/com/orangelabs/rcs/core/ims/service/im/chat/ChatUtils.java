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
package com.orangelabs.rcs.core.ims.service.im.chat;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax2.sip.header.ContactHeader;
import javax2.sip.header.ExtensionHeader;

import org.xml.sax.InputSource;

import android.text.TextUtils;

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
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoParser;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpResumeInfo;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpResumeInfoParser;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.utils.Base64;
import com.orangelabs.rcs.utils.DateUtils;
import com.orangelabs.rcs.utils.IdGenerator;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.StringUtils;
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
	 * Get referred identity
	 * 
	 * @param request SIP request
	 * @return SIP URI
	 */
	public static String getReferredIdentity(SipRequest request) {
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
    	if ((mime != null) && mime.toLowerCase().startsWith(InstantMessage.MIME_TYPE)) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    /**
     * Is a composing event type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isApplicationIsComposingType(String mime) {
    	if ((mime != null) && mime.toLowerCase().startsWith(IsComposingInfo.MIME_TYPE)) {
    		return true;
    	} else {
    		return false;
    	}
    }    

    /**
     * Is a CPIM message type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isMessageCpimType(String mime) {
    	if ((mime != null) && mime.toLowerCase().startsWith(CpimMessage.MIME_TYPE)) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    /**
     * Is an IMDN message type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isMessageImdnType(String mime) {
    	if ((mime != null) && mime.toLowerCase().startsWith(ImdnDocument.MIME_TYPE)) {
    		return true;
    	} else {
    		return false;
    	}
    }

    /**
     * Is a geolocation event type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isGeolocType(String mime) {
    	if ((mime != null) && mime.toLowerCase().startsWith(GeolocInfoDocument.MIME_TYPE)) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    /**
     * Is a file transfer HTTP event type
     * 
     * @param mime MIME type
     * @return Boolean
     */
    public static boolean isFileTransferHttpType(String mime) {
    	if ((mime != null) && mime.toLowerCase().startsWith(FileTransferHttpInfoDocument.MIME_TYPE)) {
    		return true;
    	} else {
    		return false;
    	}
    }

    /**
     * Generate resource-list for a chat session
     * 
     * @param participants List of participants
     * @return XML document
     */
    public static String generateChatResourceList(List<String> participants) {
		StringBuffer uriList = new StringBuffer();
		for(int i=0; i < participants.size(); i++) {
			String contact = participants.get(i);
			uriList.append(" <entry uri=\"" +
					PhoneUtils.formatNumberToSipUri(contact) + "\" cp:copyControl=\"to\"/>" 
					+ CRLF);
		}
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + CRLF +
			"<resource-lists xmlns=\"urn:ietf:params:xml:ns:resource-lists\" " +
			"xmlns:cp=\"urn:ietf:params:xml:ns:copycontrol\">" +
			"<list>" + CRLF +
			uriList.toString() +
			"</list></resource-lists>";
		return xml;
    }    

    /**
     * Generate resource-list for a extended chat session
     * 
     * @param existingParticipant Replaced participant
     * @param replaceHeader Replace header
     * @param newParticipants List of new participants
     * @return XML document
     */
    public static String generateExtendedChatResourceList(String existingParticipant, String replaceHeader, List<String> newParticipants) {
		StringBuffer uriList = new StringBuffer();
		for(int i=0; i < newParticipants.size(); i++) {
			String contact = newParticipants.get(i);
			if (contact.equals(existingParticipant)) {
				uriList.append(" <entry cp:copyControl=\"to\" uri=\"" + PhoneUtils.formatNumberToSipUri(existingParticipant) +
					StringUtils.encodeXML(replaceHeader) + "\"/>" + CRLF);
			} else {
				uriList.append(" <entry cp:copyControl=\"to\" uri=\"" + PhoneUtils.formatNumberToSipUri(contact) + "\"/>" + CRLF);
			}
		}
		
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + CRLF +
			"<resource-lists xmlns=\"urn:ietf:params:xml:ns:resource-lists\" " + 
			"xmlns:cp=\"urn:ietf:params:xml:ns:copycontrol\">" +
			"<list>" + CRLF +
			uriList.toString() +
			"</list></resource-lists>";
		return xml;
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
			result = false;;
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

    	// It's a SIP address: remove display name
		if (input.startsWith("\"")) {
			int index1 = input.indexOf("\"", 1);
			if (index1 > 0) {
				input = input.substring(index1+2);
			}
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
		String cpim =
			CpimMessage.HEADER_FROM + ": " + ChatUtils.formatCpimSipUri(from) + CRLF + 
			CpimMessage.HEADER_TO + ": " + ChatUtils.formatCpimSipUri(to) + CRLF + 
			CpimMessage.HEADER_DATETIME + ": " + DateUtils.encodeDate(System.currentTimeMillis()) + CRLF + 
			CRLF +  
			CpimMessage.HEADER_CONTENT_TYPE + ": " + contentType + ";charset=utf-8" + CRLF + 
			CRLF + 
			content;	
		   
		return cpim;
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
	public static String buildCpimMessageWithImdn(String from, String to, String messageId, String content, String contentType) {
		String cpim =
			CpimMessage.HEADER_FROM + ": " + ChatUtils.formatCpimSipUri(from) + CRLF + 
			CpimMessage.HEADER_TO + ": " + ChatUtils.formatCpimSipUri(to) + CRLF + 
			CpimMessage.HEADER_NS + ": " + ImdnDocument.IMDN_NAMESPACE + CRLF +
			ImdnUtils.HEADER_IMDN_MSG_ID + ": " + messageId + CRLF +
			CpimMessage.HEADER_DATETIME + ": " + DateUtils.encodeDate(System.currentTimeMillis()) + CRLF + 
			ImdnUtils.HEADER_IMDN_DISPO_NOTIF + ": " + ImdnDocument.POSITIVE_DELIVERY + ", " + ImdnDocument.DISPLAY + CRLF +
			CRLF +  
			CpimMessage.HEADER_CONTENT_TYPE + ": " + contentType + ";charset=utf-8" + CRLF +
			CpimMessage.HEADER_CONTENT_LENGTH + ": " + content.getBytes().length + CRLF + 
			CRLF + 
			content;	
		return cpim;
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
	public static String buildCpimMessageWithDeliveredImdn(String from, String to, String messageId, String content, String contentType) {
		String cpim =
			CpimMessage.HEADER_FROM + ": " + ChatUtils.formatCpimSipUri(from) + CRLF + 
			CpimMessage.HEADER_TO + ": " + ChatUtils.formatCpimSipUri(to) + CRLF + 
			CpimMessage.HEADER_NS + ": " + ImdnDocument.IMDN_NAMESPACE + CRLF +
			ImdnUtils.HEADER_IMDN_MSG_ID + ": " + messageId + CRLF +
			CpimMessage.HEADER_DATETIME + ": " + DateUtils.encodeDate(System.currentTimeMillis()) + CRLF + 
			ImdnUtils.HEADER_IMDN_DISPO_NOTIF + ": " + ImdnDocument.POSITIVE_DELIVERY + CRLF +
			CRLF +  
			CpimMessage.HEADER_CONTENT_TYPE + ": " + contentType + ";charset=utf-8" + CRLF +
			CpimMessage.HEADER_CONTENT_LENGTH + ": " + content.getBytes().length + CRLF + 
			CRLF + 
			content;	
		return cpim;
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
		// @formatter:off
		String cpim =
			CpimMessage.HEADER_FROM + ": " + ChatUtils.formatCpimSipUri(from) + CRLF + 
			CpimMessage.HEADER_TO + ": " + ChatUtils.formatCpimSipUri(to) + CRLF + 
			CpimMessage.HEADER_NS + ": " + ImdnDocument.IMDN_NAMESPACE + CRLF +
			ImdnUtils.HEADER_IMDN_MSG_ID + ": " + IdGenerator.generateMessageID() + CRLF +
			CpimMessage.HEADER_DATETIME + ": " + DateUtils.encodeDate(System.currentTimeMillis()) + CRLF + 
			CRLF + 			
			CpimMessage.HEADER_CONTENT_TYPE + ": " + ImdnDocument.MIME_TYPE + CRLF +
			CpimMessage.HEADER_CONTENT_DISPOSITION + ": " + ImdnDocument.NOTIFICATION + CRLF +
			CpimMessage.HEADER_CONTENT_LENGTH + ": " + imdn.getBytes().length + CRLF + 
			CRLF + 
			imdn;	
		return cpim;
		// @formatter:on
	}
	
	/**
	 * Parse a CPIM delivery report
	 * 
	 * @param cpim CPIM document
	 * @return IMDN document
	 */
	public static ImdnDocument parseCpimDeliveryReport(String cpim) {
		ImdnDocument imdn = null;
    	try {
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
    	} catch(Exception e) {
    		imdn = null;
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
			InputSource input = new InputSource(new ByteArrayInputStream(xml.getBytes()));
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
		
		return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + CRLF +
			"<imdn xmlns=\"urn:ietf:params:xml:ns:imdn\">" + CRLF +
	        "<message-id>" + msgId + "</message-id>" + CRLF +
	        "<datetime>" + DateUtils.encodeDate(System.currentTimeMillis()) + "</datetime>" + CRLF +
	        "<" + method + "><status><" + status + "/></status></" + method + ">" + CRLF +
	        "</imdn>";
	}
	
	/**
	* Build a geoloc document
	* 
	* @param geoloc Geoloc info
	* @param contact Contact
	* @param msgId Message ID
	* @return XML document
	*/
	public static String buildGeolocDocument(GeolocPush geoloc, String contact, String msgId) {		
		String document= "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + CRLF +
				"<rcsenvelope xmlns=\"urn:gsma:params:xml:ns:rcs:rcs:geolocation\"" +
				" xmlns:rpid=\"urn:ietf:params:xml:ns:pidf:rpid\"" +
				" xmlns:gp=\"urn:ietf:params:xml:ns:pidf:geopriv10\"" +
				" xmlns:gml=\"http://www.opengis.net/gml\"" +
				" xmlns:gs=\"http://www.opengis.net/pidflo/1.0\"" +
				" entity=\""+ contact +"\">" + CRLF;
		String expire = DateUtils.encodeDate(geoloc.getExpiration());
		document += "<rcspushlocation id=\""+ msgId +"\" label=\""+ geoloc.getLabel() +"\" >" +
				"<rpid:place-type rpid:until=\""+ expire +"\">" +				
				"</rpid:place-type>" + CRLF + 
				"<rpid:time-offset rpid:until=\""+ expire +"\"></rpid:time-offset>" + CRLF +
				"<gp:geopriv>" + CRLF + 
				"<gp:location-info>" + CRLF +
				"<gs:Circle srsName=\"urn:ogc:def:crs:EPSG::4326\">" + CRLF +
				"<gml:pos>"+ geoloc.getLatitude()+" "+geoloc.getLongitude() +"</gml:pos>" + CRLF +
				"<gs:radius uom=\"urn:ogc:def:uom:EPSG::9001\">" + geoloc.getAccuracy() + "</gs:radius>" + CRLF +
				"</gs:Circle>" + CRLF +
				"</gp:location-info>" + CRLF + 
				"<gp:usage-rules>" + CRLF +
				"<gp:retention-expiry>"+ expire +"</gp:retention-expiry>" + CRLF +
				"</gp:usage-rules>" + CRLF + 
				"</gp:geopriv>" + CRLF + 
				"<timestamp>"+ DateUtils.encodeDate(System.currentTimeMillis()) +"</timestamp>" + CRLF + 
				"</rcspushlocation>" + CRLF;
		document += "</rcsenvelope>" + CRLF;
		return document;
	}
	
	/**
	 * Parse a geoloc document
	 *
	 * @param xml XML document
	 * @return Geoloc info
	 */
	public static GeolocPush parseGeolocDocument(String xml) {
		try {
		    InputSource geolocInput = new InputSource(new ByteArrayInputStream(xml.getBytes()));
		    GeolocInfoParser geolocParser = new GeolocInfoParser(geolocInput);
		    GeolocInfoDocument geolocDocument = geolocParser.getGeoLocInfo();
		    if (geolocDocument != null) {
			    GeolocPush geoloc = new GeolocPush(geolocDocument.getLabel(),
			    		geolocDocument.getLatitude(),
			    		geolocDocument.getLongitude(),
			    		geolocDocument.getExpiration(),
			    		geolocDocument.getRadius());
			    return geoloc;
		    }
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error(e.getMessage(),e);
			}
			return null;
		}
	    return null;
	}
	
	/**
	 * Parse a file transfer over HTTP document
	 *
	 * @param xml XML document
	 * @return File transfer document
	 */
	public static FileTransferHttpInfoDocument parseFileTransferHttpDocument(byte[] xml) {
		try {
		    InputSource ftHttpInput = new InputSource(new ByteArrayInputStream(xml));
		    FileTransferHttpInfoParser ftHttpParser = new FileTransferHttpInfoParser(ftHttpInput);
		    return ftHttpParser.getFtInfo();
		} catch(Exception e) {
			return null;
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
	 * Create a first message
	 * 
	 * @param remote Remote contact
	 * @param msg Text message
	 * @param imdn IMDN flag
	 * @return First message
	 */
	public static InstantMessage createFirstMessage(String remote, String msg, boolean imdn) {
		if ((msg != null) && (msg.length() > 0)) {
			String msgId = IdGenerator.generateMessageID();
			return new InstantMessage(msgId, remote, StringUtils.encodeUTF8(msg), imdn, null);
		} else {
			return null;
		}	
	}
	
	/**
	 * Create a text message
	 * 
	 * @param remote Remote contact
	 * @param txt Text message
	 * @param imdn IMDN flag
	 * @return Text message
	 */
	public static InstantMessage createTextMessage(String remote, String msg, boolean imdn) {
		String msgId = IdGenerator.generateMessageID();
		return new InstantMessage(msgId, remote, StringUtils.encodeUTF8(msg), imdn, null);
	}

	/**
	 * Create a file transfer message
	 * 
	 * @param remote Remote contact
	 * @param file File info
	 * @param imdn IMDN flag
	 * @param displayName the display name
	 * @return File message
	 */
	public static FileTransferMessage createFileTransferMessage(String remote, String file, boolean imdn) {
		String msgId = IdGenerator.generateMessageID();
		return new FileTransferMessage(msgId, remote, file, imdn, null);
	}
	
	/**
	 * Create a geoloc message
	 * 
	 * @param remote Remote contact
	 * @param geoloc Geoloc info
	 * @param imdn IMDN flag
	 * @param displayName the display name
	 * @return Geoloc message
	 */
	public static GeolocMessage createGeolocMessage(String remote, GeolocPush geoloc, boolean imdn) {
		String msgId = IdGenerator.generateMessageID();
		return new GeolocMessage(msgId, remote, geoloc, imdn, null);
	}
	
	/**
	 * Get the first message
	 * 
	 * @param invite Request
	 * @return First message
	 */
	public static InstantMessage getFirstMessage(SipRequest invite) {
		InstantMessage msg = getFirstMessageFromCpim(invite);
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
	private static InstantMessage getFirstMessageFromCpim(SipRequest invite) {
		CpimMessage cpimMsg = ChatUtils.extractCpimMessage(invite);
		if (cpimMsg != null) {
			String remote = ChatUtils.getReferredIdentity(invite);
			String msgId = ChatUtils.getMessageId(invite);
			String content = cpimMsg.getMessageContent();
			Date date = cpimMsg.getMessageDate();
			String mime = cpimMsg.getContentType();
			if ((remote != null) && (msgId != null) && (content != null) && (mime != null)) {
				if (mime.contains(GeolocMessage.MIME_TYPE)) {
					return new GeolocMessage(msgId,
							remote,
							ChatUtils.parseGeolocDocument(content),
							ChatUtils.isImdnDisplayedRequested(invite),
							date,null);
				} else
				if (mime.contains(FileTransferMessage.MIME_TYPE)) {
					return new FileTransferMessage(msgId,
							remote,
							StringUtils.decodeUTF8(content),
							ChatUtils.isImdnDisplayedRequested(invite),
							date, null);
				} else {
					return new InstantMessage(msgId,
							remote,
							StringUtils.decodeUTF8(content),
							ChatUtils.isImdnDisplayedRequested(invite),
							date, null);
				}
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
	
	/**
	 * Get the first message from the Subject header
	 * 
	 * @param invite Request
	 * @return First message
	 */
	private static InstantMessage getFirstMessageFromSubject(SipRequest invite) {
		String subject = invite.getSubject();
		if ((subject != null) && (subject.length() > 0)) {
			String remote = ChatUtils.getReferredIdentity(invite);
			if ((remote != null) && (subject != null)) {
				return new InstantMessage(IdGenerator.generateMessageID(),
						remote,
						StringUtils.decodeUTF8(subject),
						ChatUtils.isImdnDisplayedRequested(invite),
						new Date(),
						null);
			} else {
				return null;
			}
		} else {
			return null;
		}
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
	    			CpimParser cpimParser = new CpimParser(cpimPart.getBytes());
	    			message = cpimParser.getCpimMessage();
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
     * @return {@link ListOfParticipant} participant list
     * @author Deutsche Telekom AG
     */
    public static ListOfParticipant getListOfParticipants(SipRequest request) {
        ListOfParticipant participants = new ListOfParticipant();
        try {
            String content = request.getContent();
            String boundary = request.getBoundaryContentType();
            Multipart multi = new Multipart(content, boundary);
            if (multi.isMultipart()) {
                // Extract resource-lists
                String listPart = multi.getPart("application/resource-lists+xml");
                if (listPart != null) {
                	// Create list from XML
                    participants = new ListOfParticipant(listPart);

                    // Include remote contact
                    String remote = getReferredIdentity(request);
                    participants.addParticipant(remote);
                }
            }
        } catch (Exception e) {
	    	// Nothing to do
        }
        return participants;
    }

    /**
     * Returns the data of the thumbnail file
     * 
     * @param filename Filename
     * @return Bytes or null in case of error
     */
    public static byte[] getFileThumbnail(String filename) {
    	if (filename == null) {
    		return null;
    	}
    	try {
	    	File file = new File(filename);
	    	byte [] data = new byte[(int)file.length()];
	    	FileInputStream fis = new FileInputStream(file);
	    	fis.read(data);
	    	fis.close();
	    	return data;
    	} catch(Exception e) {
    		return null;
    	}
    }
    
    /**
     * Extract thumbnail from incoming INVITE request
     * 
     * @param request Request
     * @return Thumbnail
     */
    public static byte[] extractFileThumbnail(SipRequest request) {
		try {
			// Extract message from content/CPIM
		    String content = request.getContent();
		    String boundary = request.getBoundaryContentType();
			Multipart multi = new Multipart(content, boundary);
			if (multi.isMultipart()) {
		    	// Get image/jpeg content
		    	String jpeg = multi.getPart("image/jpeg");
		    	if (jpeg != null) {
		    		// Decode the content
		    		return Base64.decodeBase64(jpeg.getBytes());
		    	}
		    	
		    	// Get image/png content
		    	String png = multi.getPart("image/png");
		    	if (png != null) {
		    		// Decode the content
		    		return Base64.decodeBase64(png.getBytes());
		    	}
		    }
		} catch(Exception e) {
			return null;
		}		
		return null;
    }

    /**
     * Is request is for FToHTTP
     *
     * @param request
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
     * Get the HTTP file transfer info document
     *
     * @param request Request
     * @return FT HTTP info
     */
	public static FileTransferHttpInfoDocument getHttpFTInfo(SipRequest request) {
        InstantMessage message = getFirstMessage(request);
        if ((message != null) && (message instanceof FileTransferMessage)) {
        	FileTransferMessage ftMsg = (FileTransferMessage)message;
			byte[] xml = ftMsg.getFileInfo().getBytes();
            return parseFileTransferHttpDocument(xml);
        } else {
            return null;
        }
	}
}
