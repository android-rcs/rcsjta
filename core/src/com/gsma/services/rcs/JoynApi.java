package com.gsma.services.rcs;

import com.gsma.services.rcs.capability.CapabilityService;
import com.gsma.services.rcs.chat.ChatService;
import com.gsma.services.rcs.contacts.ContactsService;
import com.gsma.services.rcs.ft.FileTransferService;
import com.gsma.services.rcs.gsh.GeolocSharingService;
import com.gsma.services.rcs.ipcall.IPCallService;
import com.gsma.services.rcs.ish.ImageSharingService;
import com.gsma.services.rcs.session.MultimediaSessionService;
import com.gsma.services.rcs.vsh.VideoSharingService;

import android.content.Context;

/**
 * Joyn API framework
 * 
 * @author Jean-Marc AUFFRET
 */
public class JoynApi {
	/**
	 * Chat service
	 */
	public enum SERVICE {
		CONTACT,
		CAPABILITIES,
		CHAT,
		FILETRANSFER,
		IMAGE_SHARING,
		VIDEO_SHARING,
		IPCALL,
		MM_SESSION
	}
	
	/**
	 * Table of joyn services
	 */
	private JoynService services[] = new JoynService[9]; 
	
	/**
	 * Singleton instance
	 */
	private static JoynApi instance = null; 
	
	/**
	 * Table of service
	 */
	
	/**
	 * Instanciates a given service from its service ID
	 * 
     * @param ctx Application context
     * @param listener Service listener
	 * @return Singleton instance
	 */
	public synchronized JoynApi getInstance(Context ctx) {
		if (instance != null) {
			return null;
		} else {
			instance = new JoynApi(ctx);
		}
		return instance;
	}
	
	/**
	 * Constructor
	 * 
     * @param ctx Application context
	 */
	private JoynApi(Context ctx) {
		services[0] = new ContactsService(ctx, null);
		services[1] = new CapabilityService(ctx, null);
		services[2] = new ChatService(ctx, null);
		services[3] = new FileTransferService(ctx, null);
		services[4] = new ImageSharingService(ctx, null);
		services[5] = new VideoSharingService(ctx, null);
		services[6] = new GeolocSharingService(ctx, null);
		services[7] = new IPCallService(ctx, null);
		services[8] = new MultimediaSessionService(ctx, null);
		// Add connection listener
	}
	
	/**
	 * Connects the API framework
	 */
	public void connect() {
		for (int i=0; i < 9; i++) {
			services[i].connect();			
		}
	}
	
	/**
	 * Disconnects the API framework
	 */
	public void disconnect() {
		for (int i=0; i < 9; i++) {
			services[i].disconnect();			
		}
	}
	
	
	/**
	 * Get a joyn service from its ID
	 * 
	 * @param service Service ID
	 * @return Joyn service
	 */
	public JoynService getJoynService(int service) {
		return services[service];
	}
}

