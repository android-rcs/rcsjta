package org.gsma.joyn;

/**
 * Intents related to joyn services
 * 
 * @author Jean-Marc AUFFRET
 */
public class Intents {
    /**
     * Intents for joyn client
     */
    public static class Client {
    	/**
    	 * Intent to load the settings activity to enable or disable the client
    	 */
    	public static final String ACTION_CLIENT_SETTINGS = "org.gsma.joyn.client.SETTINGS";

    	/**
    	 * Intent to request the client status. The result is received via an Intent
    	 * having the following extras:
    	 * <ul>
    	 * <li> {@link #EXTRA_CLIENT} containing the client package name.
    	 * <li> {@link #EXTRA_STATUS} containing the boolean status of the client. True
    	 *  means that the client is activated, else the client is not activated.
    	 */
    	public static final String ACTION_CLIENT_GET_STATUS = ".client.GET_STATUS";

    	/**
    	 * Client package name
    	 */
    	public final static String EXTRA_CLIENT = "client";
    	
    	/**
    	 * Client status
    	 */
    	public final static String EXTRA_STATUS = "status";

    	private Client() {
        }    	
    }
    
    /**
     * Intents for chat service
     */
    public static class Chat {
    	/**
    	 * Load the chat application to view a chat conversation. This
    	 * Intent takes into parameter an URI on the chat conversation
    	 * (i.e. content://chats/chat_ID). If no parameter found the main
    	 * entry of the chat application is displayed.
    	 */
		public static final String ACTION_VIEW_CHAT = "org.gsma.joyn.VIEW_CHAT";

		/**
		 * Load the chat application to start a new conversation with a
		 * given contact. This Intent takes into parameter a contact URI
		 * (i.e. content://contacts/people/contact_ID). If no parameter the
		 * main entry of the chat application is displayed.
		 */
		public static final String ACTION_INITIATE_CHAT = "org.gsma.joyn.INITIATE_CHAT";

		/**
		 * Load the group chat application. This Intent takes into parameter an
		 * URI on the group chat conversation (i.e. content://chats/chat_ID). If
		 * no parameter found the main entry of the group chat application is displayed.
		 */
		public static final String ACTION_VIEW_CHAT_GROUP = "org.gsma.joyn.VIEW_GROUP_CHAT";

		/**
		 * Load the group chat application to start a new conversation with a
		 * group of contacts. This Intent takes into parameter a list of contact
		 * URIs. If no parameter the main entry of the group chat application is displayed.
		 */
		public static final String ACTION_INITIATE_CHAT_GROUP = "org.gsma.joyn.INITIATE_CHAT_GROUP";

		private Chat() {
        }    	
    }

    /**
     * Intents for file transfer service
     */
    public static class FileTransfer {
    	/**
    	 * Load the file transfer application to view a file transfer. This Intent
    	 * takes into parameter an URI on the file transfer (i.e. content://filetransfers/ft_ID).
    	 * If no parameter found the main entry of the file transfer application is displayed.
    	 */
    	public static final String ACTION_VIEW_FT = "org.gsma.joyn.VIEW_FT";

    	/**
    	 * Load the file transfer application to start a new file transfer to a given
    	 * contact. This Intent takes into parameter a contact URI (i.e. content://contacts/people/contact_ID).
    	 * If no parameter the main entry of the file transfer application is displayed.
    	 */
    	public static final String ACTION_INITIATE_FT = "org.gsma.joyn.INITIATE_FT";

    	private FileTransfer() {
        }    	
    }
}
