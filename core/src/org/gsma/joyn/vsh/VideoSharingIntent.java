package org.gsma.joyn.vsh;

/**
 * Intent for video sharing invitations
 * 
 * @author Jean-Marc AUFFRET
 */
public class VideoSharingIntent {
    /**
     * Broadcast action: a new video sharing invitation has been received.
     * <p>Intent includes the following extras:
     * <ul>
     * <li> {@link #EXTRA_CONTACT} containing the MSISDN of the contact
     *  sending the invitation.
     * <li> {@link #EXTRA_DISPLAY_NAME} containing the display name of the
     *  contact sending the invitation (extracted from the SIP address).
     * <li> {@link #EXTRA_SHARING_ID} containing the unique ID of the video sharing.
     * <li> {@link #EXTRA_ENCODING} containing the video encoding.
     * <li> {@link #EXTRA_FORMAT} containing the video format.
     * </ul>
     */
	public final static String ACTION_NEW_INVITATION = "org.gsma.joyn.vsh.action.NEW_VIDEO_SHARING";

	/**
	 * MSISDN of the contact sending the invitation
	 */
	public final static String EXTRA_CONTACT = "contact";
	
	/**
	 * Display name of the contact sending the invitation
	 */
	public final static String EXTRA_DISPLAY_NAME = "contactDisplayname";

	/**
	 * Unique ID of the video sharing
	 */
	public final static String EXTRA_SHARING_ID = "sharingId";

	/**
	 * Video encoding (e.g. H264)
	 */
	public final static String EXTRA_ENCODING = "encoding";

	/**
	 * Video format (e.g. QCIF)
	 */
	public final static String EXTRA_FORMAT = "format";
}
