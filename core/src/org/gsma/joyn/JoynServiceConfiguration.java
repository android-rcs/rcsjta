package org.gsma.joyn;

/**
 * joyn Service configuration
 *  
 * @author Jean-Marc AUFFRET
 */
public class JoynServiceConfiguration {
	/**
	 * Returns True if the joyn service is activated, else returns False. The service may be activated or
	 * deactivated by the end user via the joyn settings application.
	 * 
	 * @return Boolean
	 */
	public static boolean isServiceActivated() {
		// TODO
		return true;
	}
	
	
	/**
	 * Returns the display name associated to the joyn user account. The display name may be updated by
	 * the end user via the joyn settings application.
	 * 
	 * @return Display name
	 */
	public static String getUserDisplayName() {
		// TODO
		return null;
	}
}
