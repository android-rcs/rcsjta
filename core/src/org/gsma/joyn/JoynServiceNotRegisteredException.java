package org.gsma.joyn;

/**
 * Joyn service not registered exception
 *  
 * @author jexa7410
 */
public class JoynServiceNotRegisteredException extends JoynServiceException {
	static final long serialVersionUID = 1L;
	
	/**
	 * Constructor
	 */
	public JoynServiceNotRegisteredException() {
		super("joyn service not registered");
	}
}
