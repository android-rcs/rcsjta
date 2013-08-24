package org.gsma.joyn.chat;

/**
 * Intent for chat conversation
 * 
 * @author Jean-Marc AUFFRET
 */
public class ChatIntent {
    /**
     * Broadcast action: a new chat conversation has been received.
     * <p>Intent includes the following extras:
     * <ul>
     * <li> {@link #EXTRA_CONTACT} containing the MSISDN of the contact
     *  sending the invitation.
     * <li> {@link #EXTRA_DISPLAY_NAME} containing the display name of the
     *  contact sending the invitation (extracted from the SIP address).
     * <li> {@link #EXTRA_FIRST_MESSAGE} containing the first message in the new
     *  conversation (parcelable object).
     * <li> {@link #EXTRA_CHAT_ID} containing the MIME type of the file to be transferred.
     * </ul>
     */
	public final static String ACTION_NEW_INVITATION = "org.gsma.joyn.chat.action.NEW_CHAT";

	/**
	 * MSISDN of the contact sending the invitation
	 */
	public final static String EXTRA_CONTACT = "contact";
	
	/**
	 * Display name of the contact sending the invitation (extracted from the SIP address)
	 */
	public final static String EXTRA_DISPLAY_NAME = "contactDisplayname";

	/**
	 * First message
	 * 
	 * @see ChatMessage
	 */
	public final static String EXTRA_FIRST_MESSAGE = "firstMessage";
}
