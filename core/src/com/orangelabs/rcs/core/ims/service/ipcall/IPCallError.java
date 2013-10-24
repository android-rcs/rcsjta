package com.orangelabs.rcs.core.ims.service.ipcall;

import com.orangelabs.rcs.core.ims.service.ImsServiceError;
import com.orangelabs.rcs.core.ims.service.ImsSessionBasedServiceError;

/**
 * IP call error
 * 
 * @author opob7414
 */
public class IPCallError extends ImsSessionBasedServiceError {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Renderer is not initialized
	 */
	public final static int RENDERER_NOT_INITIALIZED = IPCALL_ERROR_CODES + 1;
	
	/**
	 * Player is not initialized
	 */
	public final static int PLAYER_NOT_INITIALIZED = IPCALL_ERROR_CODES + 2;

	/**
	 * Media player has failed
	 */
	public final static int PLAYER_FAILED = IPCALL_ERROR_CODES + 3;
	
	/**
	 * Media renderer has failed (e.g. video player failure)
	 */
	public final static int RENDERER_FAILED = IPCALL_ERROR_CODES + 4;
	
	/**
	 * Unsupported audio type (e.g. codec not supported)
	 */
	public final static int UNSUPPORTED_AUDIO_TYPE = IPCALL_ERROR_CODES + 5;

	/**
	 * Unsupported video type (e.g. codec not supported)
	 */
	public final static int UNSUPPORTED_VIDEO_TYPE = IPCALL_ERROR_CODES + 6;
	
	/**
	 * Command not accepted (e.g. add video requested while a previous addVideo request is being processed)
	 */
	public final static int INVALID_COMMAND = IPCALL_ERROR_CODES + 7;
	
	/**
     * Constructor
     *
     * @param error Error
     */
	public IPCallError(ImsServiceError error) {
		super(error);
	}
	
	/**
	 * Constructor
	 * 
	 * @param code Error code
	 */
	public IPCallError(int code) {
		super(code);
	}
	
	/**
	 * Constructor
	 * 
	 * @param code Error code
	 * @param msg Detail message 
	 */
	public IPCallError(int code, String msg) {
		super(code, msg);
	}
}
