package com.orangelabs.rcs.service.api.client.gsma;

/**
 * GSMA UI connector 1.0
 * 
 * @author jexa7410
 */
public class GsmaUiConnector {
	/**
     * Intent request broadcasted to determine if the device is RCS compliant or
     * not. The boolean extra EXTRA_RCS_STATUS is set to true if the device is RCS
     * compliant and activated by the Telco, else it is set to false. The boolean
     * extra EXTRA_REGISTRATION_STATUS is set to true if the device is registered
     * to the IMS platform, else it is set to false.
     */
	public final static String ACTION_GET_RCS_STATUS = "android.net.rcs.GET_RCS_STATUS";	
	
	/**
     * Intent broadcasted when the IMS registration has changed. The boolean extra
     * EXTRA_REGISTRATION_STATUS is set to true if the device is registered to the
     * IMS platform, else it is set to false.
     */
	public final static String ACTION_REGISTRATION_CHANGED = "android.net.rcs.REGISTRATION_CHANGED";
	
	/**
	 * The lookup key for a boolean that indicates whether the device is RCS compliant
	 * or not. Retrieve it with getBooleanExtra(String, boolean).
	 */
	public static final String EXTRA_RCS_STATUS = "rcs";

	/**
	 * The lookup key for a boolean that indicates whether the device is registered or
	 * not. Retrieve it with getBooleanExtra(String, boolean).
	 */
	public static final String EXTRA_REGISTRATION_STATUS = "registration";
	
	/**
	 * Intent broadcasted to get the supported capabilities of a given contact. The
	 * data contains a contact URI (e.g. content://contacts/people/x). The boolean
	 * extra EXTRA_CAPABILITITY_XXX is set to true if the contact supports the XXX
	 * capability, else it is set to false. 
	 */
	public static final String ACTION_GET_CONTACT_CAPABILITIES = "android.net.rcs.GET_CONTACT_CAPABILITIES";

	/**
	 * Intent broadcasted to get the capabilities supported by the device. The
	 * boolean extra EXTRA_CAPABILITITY_XXX is set to true if the device supports
	 * the XXX capability, else it is set to false.
	 */
	public static final String ACTION_GET_MY_CAPABILITIES = "android.net.rcs.GET_MY_CAPABILITIES";

	/**
	 * Intent broadcasted when capabilities of a contact have changed. The boolean
	 * extra EXTRA_CAPABILITITY_XXX is set to true if the contact supports the XXX
	 * capability, else it is set to false. The string extra EXTRA_CONTACT contains
	 * the contact URI.
	 */
	public static final String ACTION_CAPABILITIES_CHANGED = "android.net.rcs.CAPABILITIES_CHANGED";

	/**
	 * The lookup key for a string that contains URI of the contact. Retrieve it with
	 * getStringExtra(String). 
	 */
	public static final String EXTRA_CONTACT = "contact";

	/**
	 * The lookup key for a boolean that indicates whether the device supports the chat
	 * service or not. Retrieve it with getBooleanExtra(String, boolean). 
	 */
	public static final String EXTRA_CAPABILITY_CHAT = "chat";

	/**
	 * The lookup key for a boolean that indicates whether the device supports the file
	 * transfer service or not. Retrieve it with getBooleanExtra(String, boolean). 
	 */
	public static final String EXTRA_CAPABILITY_FT = "filetransfer";
	
	/**
	 * The lookup key for a boolean that indicates whether the device supports the file
	 * transfer service or not. Retrieve it with getBooleanExtra(String, boolean). 
	 */
	public static final String EXTRA_CAPABILITY_IMAGE_SHARE = "imageshare";

	/**
	 * The lookup key for a boolean that indicates whether the device supports the file
	 * transfer service or not. Retrieve it with getBooleanExtra(String, boolean). 
	 */
	public static final String EXTRA_CAPABILITY_VIDEO_SHARE = "videoshare";

	/**
	 * The lookup key for a boolean that indicates whether the device supports the geoloc
	 * push service or not. Retrieve it with getBooleanExtra(String, boolean). 
	 */
	public static final String EXTRA_CAPABILITY_GEOLOCATION_PUSH = "geolocpush";

	/**
	 * The lookup key for a boolean that indicates whether the device supports the
	 * Store&Forward service or not. Retrieve it with getBooleanExtra(String, boolean). 
	 */
	public static final String EXTRA_CAPABILITY_SF = "standfw";

	/**
	 * The lookup key for a boolean that indicates whether the device supports the
	 * presence discovery service or not. Retrieve it with getBooleanExtra(String, boolean). 
	 */
	public static final String EXTRA_CAPABILITY_PRESENCE_DISCOVERY = "presencediscovery";

	/**
	 * The lookup key for a boolean that indicates whether the device supports the social
	 * presence service or not. Retrieve it with getBooleanExtra(String, boolean). 
	 */
	public static final String EXTRA_CAPABILITY_SOCIAL_PRESENCE = "socialpresence";

	/**
	 * The lookup key for a boolean that indicates whether the device supports the
	 * CS video service or not. Retrieve it with getBooleanExtra(String, boolean). 
	 */
	public static final String EXTRA_CAPABILITY_CS_VIDEO = "csvideo";

	/**
	 * The lookup key for a string array that indicates whether the device supports
	 * extensions or not. Retrieve it with getStringArrayExtra(String). 
	 */
	public static final String EXTRA_CAPABILITY_EXTENSIONS = "extensions";
	
	/**
	 * Intent to load the chat application or chat view. Input: the data contains the
	 * URI (e.g. content://chats/x) of the chat conversation. If no data the main entry
	 * of the chat application is displayed. 
	 */
	public static final String ACTION_VIEW_CHAT = "android.net.rcs.VIEW_CHAT";

	/**
	 * Intent to load the chat view in order to start a chat conversation with a given
	 * contact. The data contains a contact URI (e.g. content://contacts/people/x). If
	 * no data the main entry of the chat application is displayed. 
	 */
	public static final String ACTION_INITIATE_CHAT = "android.net.rcs.INITIATE_CHAT";

	/**
	 * Intent to load the group chat application or group chat view. Input: the data
	 * contains the URI (e.g. content://chats/x) of the group chat conversation. If
	 * no data the main entry of the chat application is displayed.
	 */
	public static final String ACTION_VIEW_CHAT_GROUP = "android.net.rcs.VIEW_CHAT_GROUP"; 

	/**
	 * Intent to load the chat view in order to start a chat conversation with a group
	 * of contacts. The data contains a list of contact URI. If no data the main entry
	 * of the group chat application is displayed.
	 */
	public static final String ACTION_INITIATE_CHAT_GROUP = "android.net.rcs.INITIATE_CHAT_GROUP";

	/**
	 * Intent to load the file transfer application. Input: the data contains the URI
	 * of the file transfer conversation. If no data the main entry of the file transfer
	 * application is displayed. 
	 */
	public static final String ACTION_VIEW_FT = "android.net.rcs.VIEW_FT";

	/**
	 * Intent to load the file transfer view in order to start a file transfer with a
	 * given contact. The data contains a contact URI (e.g. content://contacts/people/x)
	 * and file URI (absolute path or media content URI). If no data the main entry of
	 * the file transfer application is displayed.
	 */
	public static final String ACTION_INITIATE_FT = "android.net.rcs.INITIATE_FT";
}