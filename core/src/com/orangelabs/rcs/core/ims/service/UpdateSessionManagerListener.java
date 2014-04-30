package com.orangelabs.rcs.core.ims.service;

import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.protocol.sip.SipResponse;


/**
 * Listener of events sent to Update Session Manager
 * 
 * @author O. Magnon
 */
public interface UpdateSessionManagerListener {
	
	/**
	 * ReInvite Response received
	 * 
	 * @param code  Sip response code
	 * @param response  Sip response request
	 */
	public void handleReInviteResponse(int code, SipResponse response) ;

	
    /**
     * User answer received 
     * 
	 * @param code  user response code
     */
    public void handleReInviteUserAnswer(int code) ;
    
    
    /**
     * ReInvite Ack received  
     * 
     * @param code  Sip response code
     */
    public void handleReInviteAck(int code) ;
    
    
    /**
     * Sdp requested for ReInvite Response 
     * 
     * @param reInvite  Sip reInvite request received
     */
    public String buildReInviteSdpResponse(SipRequest reInvite);
}
