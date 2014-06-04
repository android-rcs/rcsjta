package com.orangelabs.rcs.core.ims.service.sip.streaming;

import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.network.sip.SipUtils;
import com.orangelabs.rcs.core.ims.protocol.sdp.SdpUtils;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.sip.SipSessionError;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Originating SIP RTP session
 *  
 * @author Jean-Marc AUFFRET
 */
public class OriginatingSipRtpSession extends GenericSipRtpSession {
	/**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * Constructor
	 * 
	 * @param parent IMS service
	 * @param contact Remote contact
	 * @param featureTag Feature tag
	 */
	public OriginatingSipRtpSession(ImsService parent, String contact, String featureTag) {
		super(parent, contact, featureTag);
		
		// Create dialog path
		createOriginatingDialogPath();
	}

	/**
	 * Background processing
	 */
	public void run() {
		try {
	    	if (logger.isActivated()) {
	    		logger.info("Initiate a new RTP session as originating");
	    	}
	    	
			// Set SDP offer
	    	String ntpTime = SipUtils.constructNTPtime(System.currentTimeMillis());
	    	String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();
	    	String sdp =
	    		"v=0" + SipUtils.CRLF +
	            "o=- " + ntpTime + " " + ntpTime + " " + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF +
	            "s=-" + SipUtils.CRLF +
				"c=" + SdpUtils.formatAddressType(ipAddress) + SipUtils.CRLF +
	            "t=0 0" + SipUtils.CRLF +			
	            "m=application " + getLocalRtpPort() + " RTP/AVP " + getRtpFormat().getPayload() + SipUtils.CRLF + 
	    		"a=sendrecv" + SipUtils.CRLF;
	    	
	    	// Set the local SDP part in the dialog path
	        getDialogPath().setLocalContent(sdp);

	        // Create an INVITE request
	        if (logger.isActivated()) {
	        	logger.info("Send INVITE");
	        }
	        SipRequest invite = SipMessageFactory.createInvite(getDialogPath(),
	        		new String [] { getFeatureTag() }, sdp);

	        // Set the Authorization header
	        getAuthenticationAgent().setAuthorizationHeader(invite);
	        
	        // Set initial request in the dialog path
	        getDialogPath().setInvite(invite);
	        
	        // Send INVITE request
	        sendInvite(invite);	        
		} catch(Exception e) {
        	if (logger.isActivated()) {
        		logger.error("Session initiation has failed", e);
        	}

        	// Unexpected error
			handleError(new SipSessionError(SipSessionError.UNEXPECTED_EXCEPTION,
					e.getMessage()));
		}
	}
}
