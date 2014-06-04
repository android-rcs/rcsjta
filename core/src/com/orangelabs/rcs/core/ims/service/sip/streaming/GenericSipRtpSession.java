package com.orangelabs.rcs.core.ims.service.sip.streaming;

import com.orangelabs.rcs.core.ims.network.sip.SipMessageFactory;
import com.orangelabs.rcs.core.ims.protocol.rtp.MediaRtpReceiver;
import com.orangelabs.rcs.core.ims.protocol.rtp.MediaRtpSender;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.Format;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.data.DataFormat;
import com.orangelabs.rcs.core.ims.protocol.rtp.stream.RtpStreamListener;
import com.orangelabs.rcs.core.ims.protocol.sip.SipException;
import com.orangelabs.rcs.core.ims.protocol.sip.SipRequest;
import com.orangelabs.rcs.core.ims.service.ImsService;
import com.orangelabs.rcs.core.ims.service.ImsServiceError;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.capability.CapabilityUtils;
import com.orangelabs.rcs.core.ims.service.sip.SipSessionError;
import com.orangelabs.rcs.core.ims.service.sip.SipSessionListener;
import com.orangelabs.rcs.utils.NetworkRessourceManager;
import com.orangelabs.rcs.utils.logger.Logger;

public class GenericSipRtpSession extends ImsServiceSession implements RtpStreamListener {
	/**
	 * Feature tag
	 */
	private String featureTag;
	
	/**
	 * RTP payload format
	 */
	private DataFormat format = new DataFormat();
	
	/**
	 * Local RTP port
	 */
	private int localRtpPort = -1;
	
	/**
	 * RTP receiver
	 */
	private MediaRtpReceiver rtpRecv;
	
	/**
	 * RTP sender
	 */
	private MediaRtpSender rtpSnd;

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
	public GenericSipRtpSession(ImsService parent, String contact, String featureTag) {
		super(parent, contact);

		// Set the service feature tag
		this.featureTag = featureTag;

		// Create the RTP receiver
		localRtpPort = NetworkRessourceManager.generateLocalRtpPort();
		rtpRecv = new MediaRtpReceiver(localRtpPort);
		rtpSnd = new MediaRtpSender(format, localRtpPort);
	}

    /**
     * Get local port
     * 
     * @return RTP port
     */
    public int getLocalRtpPort() {
    	return localRtpPort;
    }	
	
	/**
	 * Returns feature tag of the service
	 * 
	 * @return Feature tag
	 */
	public String getFeatureTag() {
		return featureTag;
	}
	
	/**
	 * Returns the service ID
	 * 
	 * @return Service ID
	 */
	public String getServiceId() {
		return CapabilityUtils.extractServiceId(featureTag);
	}	

	/**
	 * Returns the RTP receiver
	 * 
	 * @return RTP receiver
	 */
	public MediaRtpReceiver getRtpReceiver() {
		return rtpRecv;
	}	
	
	/**
	 * Returns the RTP sender
	 * 
	 * @return RTP sender
	 */
	public MediaRtpSender getRtpSender() {
		return rtpSnd;
	}
	
	/**
	 * Returns the RTP format
	 * 
	 * @return RTP format
	 */
	public Format getRtpFormat() {
		return format;
	}	
	
	/**
     * Create an INVITE request
     *
     * @return the INVITE request
     * @throws SipException 
     */
    public SipRequest createInvite() throws SipException {
        return SipMessageFactory.createInvite(
                getDialogPath(),
                new String [] { getFeatureTag() },
                getDialogPath().getLocalContent());
    }

    /**
     * Prepare media session
     * 
     * @throws Exception 
     */
    public void prepareMediaSession() throws Exception {
        // TODO
/*    	rtpSender.prepareSession(rtpInput, remoteHost, remotePort, this);
        rtpReceiver.prepareSession(remoteHost, remotePort, orientationHeaderId, rtpOutput, videoFormat, this);
        rtpDummySender.prepareSession(remoteHost, remotePort, rtpReceiver.getInputStream());
        rtpDummySender.startSession();
*/
    }

    /**
     * Start media session
     * 
     * @throws Exception 
     */
    public void startMediaSession() throws Exception {
        //TODO
    }

    /**
     * Close media session
     */
    public void closeMediaSession() {
    	// TODO
    }

    /**
     * Handle error
     * 
     * @param error Error
     */
    public void handleError(ImsServiceError error) {
        if (isSessionInterrupted()) {
        	return;
        }
        
        // Error
        if (logger.isActivated()) {
            logger.info("Session error: " + error.getErrorCode() + ", reason="
                    + error.getMessage());
        }

        // Close media session
        closeMediaSession();

        // Remove the current session
        getImsService().removeSession(this);

        // Notify listeners
        for (int j = 0; j < getListeners().size(); j++) {
            ((SipSessionListener) getListeners().get(j))
                    .handleSessionError(new SipSessionError(error));
        }
    }
    
    /**
     * Sends a payload in real time
     * 
     * @param content Payload content
	 * @return Returns true if sent successfully else returns false
     */
    public boolean sendPlayload(byte[] content) {
		// TODO
    	return false;
    }
    
    /**
     * Invoked when the RTP stream was aborted.
     */
    public void rtpStreamAborted() {
    	// TODO    	
    }
}
