package org.gsma.joyn.chat;

/**
 * Intent for group chat conversation
 * 
 * @author Jean-Marc AUFFRET
 */
public class GroupChatIntent {
    /**
     * Broadcast action: a new group chat invitation has been received.
     * <p>Intent includes the following extras:
     * <ul>
     * <li> {@link #EXTRA_CONTACT} containing the MSISDN of the contact
     *  sending the invitation.
     * <li> {@link #EXTRA_DISPLAY_NAME} containing the display name of the
     *  contact sending the invitation (extracted from the SIP address).
     * <li> {@link #EXTRA_CHAT_ID} containing the unique ID of the chat conversation.
     * <li> {@link #EXTRA_SUBJECT} containing the subject associated to the conversation.
     * </ul>
     */
	public final static String ACTION_NEW_INVITATION = "org.gsma.joyn.chat.action.NEW_GROUP_CHAT";

	/**
	 * MSISDN of the contact sending the invitation
	 */
	public final static String EXTRA_CONTACT = "contact";
	
	/**
	 * Display name of the contact sending the invitation (extracted from the SIP address)
	 */
	public final static String EXTRA_DISPLAY_NAME = "contactDisplayname";

	/**
	 * Unique ID of the chat conversation
	 */
	public final static String EXTRA_CHAT_ID = "chatId";

	/**
	 * Subject associated to the conversation (optional)
	 */
	public final static String EXTRA_SUBJECT = "subject";
}
