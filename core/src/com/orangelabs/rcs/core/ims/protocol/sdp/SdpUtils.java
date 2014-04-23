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

package com.orangelabs.rcs.core.ims.protocol.sdp;

import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.security.cert.KeyStoreManager;
import com.orangelabs.rcs.utils.IpAddressUtils;
import com.orangelabs.rcs.utils.StringUtils;

/**
 * SDP utility functions
 * 
 * @author jexa7410
 */
public class SdpUtils {
	// Changed by Deutsche Telekom
	// name of SDP fingerprint attribute
	final private static String FINGERPRINT = "fingerprint";
	
	// Changed by Deutsche Telekom
	// directions
	final public static String DIRECTION_SENDONLY = "sendonly";
	final public static String DIRECTION_RECVONLY = "recvonly";
	final public static String DIRECTION_SENDRECV = "sendrecv";

	// Changed by Deutsche Telekom
	// protocols
	final public static String MSRPS_PROTOCOL = "TCP/TLS/MSRP";
	final public static String MSRP_PROTOCOL  = "TCP/MSRP";
	
	/**
	 * Extract the remote host address from the connection info
	 * 
	 * @param connectionInfo Connection info
	 * @return Address
	 */
	public static String extractRemoteHost(String connectionInfo) {
		// c=IN IP4 172.20.138.145
		String[] tokens = connectionInfo.split(" ");
		if (tokens.length > 2) {
			return tokens[2];
		} else {
			return null;
		}
	}
	
    /**
     * Extract the remote host address from the connection info
     * 
     * @param sessionDescription Session description part of SDP
     * @param mediaDescription Media description part of SDP
     * @return Remote host address or null if not found
     */
    public static String extractRemoteHost(SessionDescription sessionDescription, MediaDescription mediaDescription) {
        String remoteHost = null;
        
        // First we need try to use the media description connection info
        if ((mediaDescription != null) &&
				(mediaDescription.connectionInfo != null)) {
            remoteHost = extractRemoteHost(mediaDescription.connectionInfo);
        }
        
        // If media description has no connection info or remote host information, we need to try
        // to use the session description connection info
        if ((remoteHost == null) &&
        		(sessionDescription != null) &&
        			(sessionDescription.connectionInfo != null)) {
            remoteHost = extractRemoteHost(sessionDescription.connectionInfo);
        }
        return remoteHost;
    }

	// Changed by Deutsche Telekom
	/**
	 * Extract the fingerprint from certificate announced by remote
	 * 
	 * @param parser
	 *            SDP parser
	 * @param mediaDescription
	 *            media description part of SDP
	 * @return fingerprint
	 */
	public static String extractFingerprint(SdpParser parser,
			MediaDescription mediaDescription) {
		String fingerprint = null;
		if (parser != null && parser.getSessionAttribute(FINGERPRINT) != null) {
			fingerprint = new String(parser.getSessionAttribute(FINGERPRINT)
					.getValue());
		}
		if (mediaDescription != null
				&& mediaDescription.getMediaAttribute(FINGERPRINT) != null) {
			fingerprint = new String(mediaDescription.getMediaAttribute(
					FINGERPRINT).getValue());
		}
		return fingerprint;
	}

    // Changed by Deutsche Telekom
    /**
     * Check if an SDP attribute from media description contains a specific value
     * 
     * @param mediaDesc {@link MediaDescription}
     * @param sdpAttribute SDP attribute string
     * @param sdpAttributeValue SDP attribute value
     * @param supportAllValuesCharacter {@link true} for assume that contains the value if attribute contains '*' as
     *            value, {@link false} to force find the specific value
     * @return {@link true} if contains the specific value, otherwise {@link false}
     */
    public static boolean attributeContains(MediaDescription mediaDesc, String sdpAttribute, String sdpAttributeValue, boolean supportAllValuesCharacter) {

        if (mediaDesc == null || StringUtils.isEmpty(sdpAttribute) || StringUtils.isEmpty(sdpAttributeValue)) {
            return false;
        }

        MediaAttribute mediaSdpAttrib = mediaDesc.getMediaAttribute(sdpAttribute);
        
        if (mediaSdpAttrib != null && !StringUtils.isEmpty(mediaSdpAttrib.getValue())) {
            if (supportAllValuesCharacter) {
                if ("*".equals(mediaSdpAttrib.getValue().trim())) {
                    return true;
                }
            }

            String[] attribValues = mediaSdpAttrib.getValue().split("[\\s]+");
            for (int i = 0; i < attribValues.length; i++) {
                if (sdpAttributeValue.equalsIgnoreCase(attribValues[i].trim())) {
                    return true;
                }
            }
        }
        
        return false;
    }

	/**
     * Format "IN IP" attribute (4 or 6)
     *
     * @param address IP address
     * @return "IN IP4 address" or "IN IP6 address"
     */
    public static String formatAddressType(String address) {
        if (IpAddressUtils.isIPv6(address)) {
            return "IN IP6 " + address;
        } else {
            return "IN IP4 " + address;
        }
    }
    
    // Changed by Deutsche Telekom
    /**
     * Get parameter value from SDP parameters string with parameter-value
     * format 'key1=value1; ... keyN=valueN'
     * 
     * @param paramKey parameter name
     * @param params parameters string
     * @return if parameter exists return {@link String} with value, otherwise
     *         return <code>null</code>
     */
    public static String getParameterValue(String paramKey, String params) {
        String value = null;
        if (params != null && params.length() > 0) {
            try {
                java.util.regex.Pattern p = java.util.regex.Pattern
                        .compile("(?<=" + paramKey + "=).*?(?=;|$)");
                java.util.regex.Matcher m = p.matcher(params);
                if (m.find()) {
                    value = m.group(0);
                }
            } catch (java.util.regex.PatternSyntaxException ex) {
            }
        }
        return value;
    }
    
    // Changed by Deutsche Telekom
    /**
     * Build an SDP block for an One-2-One chat sessions
     * 
     * @param ipAddress local IP address used in o and c line
     * @param localPort local port used in m line
     * @param protocol protocol used in m line
     * @param acceptTypes accepted MIME types
     * @param wrapperTypes accepted wrapper types
     * @param setup connection setup (active, passive, actpass)
     * @param path 
     * @param direction message direction (sendrecv, recvonly)
     * 
     * @return SDP
     */
    public static String buildChatSDP(String ipAddress, int localPort, String protocol, String acceptTypes,
            String wrapperTypes, String setup, String path, String direction) {
        return buildSDP(ipAddress, localPort, protocol, acceptTypes, wrapperTypes, null, null, null, setup, path,
                direction, null, 0);
	}
	
    // Changed by Deutsche Telekom
    /**
     * Build an SDP block for a group chat sessions
     * 
     * @param ipAddress local IP address used in o and c line
     * @param localPort local port used in m line
     * @param protocol protocol used in m line
     * @param acceptTypes accepted MIME types
     * @param wrapperTypes accepted wrapper types
     * @param setup connection setup (active, passive, actpass)
     * @param path 
     * @param direction message direction (sendrecv, recvonly)
     * 
     * @return SDP
     */
    public static String buildGroupChatSDP(String ipAddress, int localPort, String protocol, String acceptTypes,
            String wrapperTypes, String setup, String path, String direction) {
        return buildSDP(ipAddress, localPort, protocol, acceptTypes, wrapperTypes, null, null, null, setup, path,
                direction, null, 0);
	}
	
    // Changed by Deutsche Telekom
    /**
     * @param ipAddress local IP address used in o and c line
     * @param localPort local port used in m line
     * @param protocol protocol used in m line
     * @param acceptTypes accepted MIME types
     * @param transferId file-transfer-id
     * @param selector file-selector
     * @param disposition file-disposition (used only when sendonly)
     * @param setup connection setup (active, passive, actpass)
     * @param path 
     * @param direction message direction (sendonly, recvonly)
     * @param maxSize maximum file size
     * 
     * @return SDP
     */
	public static String buildFileSDP(String ipAddress, int localPort,
			String protocol, String acceptTypes, String transferId, String selector, String disposition,
			String setup, String path, String direction, int maxSize) {
        return buildSDP(ipAddress, localPort, protocol, acceptTypes, null, transferId, selector,
                disposition, setup, path, direction, null, maxSize);
	}

    // Changed by Deutsche Telekom
    /**
     * @param ipAddress local IP address used in o and c line
     * @param media media part
     * @param direction message direction (sendonly, recvonly)
     * 
     * @return SDP
     */
	public static String buildVideoSDP(String ipAddress, String media, String direction) {
        return buildSDP(ipAddress, 0, null, null, null, null, null, null, null, null, direction,
                media, 0);
	}

    // Changed by Deutsche Telekom
	/**
	 * @param ipAddress local IP address used in o and c line
	 * @param protocol protocol used in m line
	 * @param acceptTypes accepted MIME types
	 * @param selector file-selector
	 * @param media media description (used for video share)
	 * @param maxSize maximum file size
	 * @return
	 */
    public static String buildCapabilitySDP(String ipAddress, String protocol, String acceptTypes,
            String selector, String media, int maxSize) {
        return buildSDP(ipAddress, 0, protocol, acceptTypes, null, null, selector, null, null, null,
                null, media, maxSize);
	}

	// Changed by Deutsche Telekom
    /**
     * @param ipAddress local IP address used in o and c line
     * @param localPort local port used in m line
     * @param protocol protocol used in m line
     * @param acceptTypes accepted MIME types
     * @param wrapperTypes accepted wrapper types
     * @param transferId file-transfer-id
     * @param selector file-selector
     * @param disposition file-disposition
     * @param setup connection setup (active, passive, actpass)
     * @param path 
     * @param direction message direction (sendrecv, recvonly, sendonly)
     * @param media media description (used for video share)
     * @param maxSize maximum file size
     * 
     * @return SDP
     */
    private static String buildSDP(String ipAddress, int localPort, String protocol,
            String acceptTypes, String wrapperTypes, String transferId, String selector,
            String disposition, String setup, String path, String direction, String media,
            int maxSize) {
		String ntpTime = SipUtils.constructNTPtime(System.currentTimeMillis());

		StringBuffer sdp = new StringBuffer("v=0" + SipUtils.CRLF + 
				"o=- " + ntpTime + " " + ntpTime + " " + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF + 
				"s=-" + SipUtils.CRLF + 
				"c=" + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF + 
				"t=0 0" + SipUtils.CRLF);

		if (media != null) {
			sdp.append(media);
		}

		if (protocol != null) {
			sdp.append("m=message " + localPort + " " + protocol + " *" + SipUtils.CRLF);
		}

		appendIfNotNull(sdp, "a=accept-types:", acceptTypes);

		appendIfNotNull(sdp, "a=accept-wrapped-types:", wrapperTypes);
		
        appendIfNotNull(sdp, "a=file-transfer-id:", transferId);
        
        appendIfNotNull(sdp, "a=file-disposition:", disposition);

        if ((selector != null) && (selector.length() > 0)){
		    // SDP used for file sharing services
			appendIfNotNull(sdp, "a=file-selector:", selector);
		} else {
		    // SDP that may be used for capability exchange
			appendIfNotNull(sdp, "a=file-selector", selector);
		}
		
		appendIfNotNull(sdp, "a=setup:", setup);
		
		appendIfNotNull(sdp, "a=path:", path);
		
		if (MSRPS_PROTOCOL.equalsIgnoreCase(protocol)) {
			appendIfNotNull(sdp, "a=" + FINGERPRINT + ":SHA-1 ",
					KeyStoreManager.getClientCertificateFingerprint());
		}

		appendIfNotNull(sdp, "a=", direction);
		
		if (maxSize > 0) {
			sdp.append("a=max-size:" + maxSize + SipUtils.CRLF);
		}
		
		return new String(sdp);
	}

	// Changed by Deutsche Telekom
	private static void appendIfNotNull (StringBuffer sdp, String tag, String value){
		if (value != null) {
			sdp.append(tag + value + SipUtils.CRLF);
		}
	}
}
