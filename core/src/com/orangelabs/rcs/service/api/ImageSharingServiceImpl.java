package com.orangelabs.rcs.service.api;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.gsma.joyn.ish.IImageSharing;
import org.gsma.joyn.ish.IImageSharingListener;
import org.gsma.joyn.ish.IImageSharingService;
import org.gsma.joyn.ish.INewImageSharingListener;
import org.gsma.joyn.ish.ImageSharingIntent;

import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;

import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.content.ContentManager;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.service.richcall.image.ImageTransferSession;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.platform.file.FileDescription;
import com.orangelabs.rcs.platform.file.FileFactory;
import com.orangelabs.rcs.provider.sharing.RichCall;
import com.orangelabs.rcs.provider.sharing.RichCallData;
import com.orangelabs.rcs.service.api.server.ServerApiException;
import com.orangelabs.rcs.service.api.server.ServerApiUtils;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Image sharing service implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class ImageSharingServiceImpl extends IImageSharingService.Stub {
	/**
	 * List of image sharing sessions
	 */
	private static Hashtable<String, IImageSharing> ishSessions = new Hashtable<String, IImageSharing>();  

	/**
	 * List of image sharing invitation listeners
	 */
	private RemoteCallbackList<INewImageSharingListener> listeners = new RemoteCallbackList<INewImageSharingListener>();

	/**
	 * The logger
	 */
	private static Logger logger = Logger.getLogger(ImageSharingServiceImpl.class.getName());

	/**
	 * Lock used for synchronization
	 */
	private Object lock = new Object();

	/**
	 * Constructor
	 */
	public ImageSharingServiceImpl() {
		if (logger.isActivated()) {
			logger.info("Image sharing service API is loaded");
		}
	}

	/**
	 * Close API
	 */
	public void close() {
		// Clear lists of sessions
		ishSessions.clear();
	}

	/**
	 * Add an image sharing session in the list
	 * 
	 * @param session Image sharing session
	 */
	protected static void addImageSharingSession(ImageSharingImpl session) {
		if (logger.isActivated()) {
			logger.debug("Add an image sharing session in the list (size=" + ishSessions.size() + ")");
		}
		ishSessions.put(session.getSharingId(), session);
	}

	/**
	 * Remove an image sharing session from the list
	 * 
	 * @param sessionId Session ID
	 */
	protected static void removeImageSharingSession(String sessionId) {
		if (logger.isActivated()) {
			logger.debug("Remove an image sharing session from the list (size=" + ishSessions.size() + ")");
		}
		ishSessions.remove(sessionId);
	}
    
    /**
     * Receive a new image sharing invitation
     * 
     * @param session Image sharing session
     */
    public void receiveImageSharingInvitation(ImageTransferSession session) {
		if (logger.isActivated()) {
			logger.info("Receive image sharing invitation from " + session.getRemoteContact());
		}

        // Extract number from contact
		String number = PhoneUtils.extractNumberFromUri(session.getRemoteContact());

		// Update rich call history
		RichCall.getInstance().addCall(number, session.getSessionID(),
				RichCallData.EVENT_INCOMING,
				session.getContent(),
				RichCallData.STATUS_STARTED);

		// Add session in the list
		ImageSharingImpl sessionApi = new ImageSharingImpl(session);
		ImageSharingServiceImpl.addImageSharingSession(sessionApi);
    	
		// Broadcast intent related to the received invitation
    	Intent intent = new Intent(ImageSharingIntent.ACTION_NEW_IMAGE_SHARING);
    	intent.putExtra(ImageSharingIntent.EXTRA_CONTACT, number);
    	intent.putExtra(ImageSharingIntent.EXTRA_DISPLAY_NAME, session.getRemoteDisplayName());
    	intent.putExtra(ImageSharingIntent.EXTRA_SHARING_ID, session.getSessionID());
    	intent.putExtra(ImageSharingIntent.EXTRA_FILENAME, session.getContent().getName());
    	intent.putExtra(ImageSharingIntent.EXTRA_FILESIZE, session.getContent().getSize());
    	intent.putExtra(ImageSharingIntent.EXTRA_FILETYPE, session.getContent().getEncoding());
    	AndroidFactory.getApplicationContext().sendBroadcast(intent);
    	
    	// Notify image sharing invitation listeners
    	synchronized(lock) {
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onNewImageSharing(session.getSessionID());
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	    }
    }

    /**
     * Shares an image with a contact. The parameter file contains the complete filename
     * including the path of the image to be shared. An exception if thrown if there is
     * no ongoing CS call. The parameter contact supports the following formats: MSISDN
     * in national or international format, SIP address, SIP-URI or Tel-URI. If the format
     * of the contact is not supported an exception is thrown.
     * 
     * @param contact Contact
     * @param filename Filename to share
     * @param listener Image sharing event listener
     * @return Image sharing
     * @throws ServerApiException
     */
    public IImageSharing shareImage(String contact, String filename, IImageSharingListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Initiate an image sharing session with " + contact);
		}

		// Test IMS connection
		ServerApiUtils.testIms();

		try {
			// Create an image content
			FileDescription desc = FileFactory.getFactory().getFileDescription(filename);
			MmContent content = ContentManager.createMmContentFromUrl(filename, desc.getSize());
			
			// Initiate a sharing session
			ImageTransferSession session = Core.getInstance().getRichcallService().initiateImageSharingSession(contact, content, false);

			// Add session in the list
			ImageSharingImpl sessionApi = new ImageSharingImpl(session);
			sessionApi.addEventListener(listener);

			// Start the session
			session.startSession();
			
			// Update rich call history
			RichCall.getInstance().addCall(contact, session.getSessionID(),
                    RichCallData.EVENT_OUTGOING,
	    			session.getContent(),
	    			RichCallData.STATUS_STARTED);

			// Add session in the list
			addImageSharingSession(sessionApi);
			return sessionApi;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}

    /**
     * Returns the list of image sharings in progress
     * 
     * @return List of image sharings
     * @throws ServerApiException
     */
    public List<IBinder> getImageSharings() throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get image sharing sessions");
		}

		// Test core availability
		ServerApiUtils.testCore();
		
		try {
			ArrayList<IBinder> result = new ArrayList<IBinder>(ishSessions.size());
			for (Enumeration<IImageSharing> e = ishSessions.elements() ; e.hasMoreElements() ;) {
				IImageSharing sessionApi = e.nextElement() ;
				result.add(sessionApi.asBinder());
			}
			return result;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
    }    

    /**
     * Returns a current image sharing from its unique ID
     * 
     * @return Image sharing
     * @throws ServerApiException
     */
    public IImageSharing getImageSharing(String sharingId) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Get image sharing session " + sharingId);
		}

		// Test core availability
		ServerApiUtils.testCore();
		
		// Return a session instance
		return ishSessions.get(sharingId);
    }    
    
    /**
	 * Registers an image sharing invitation listener
	 * 
	 * @param listener New image sharing listener
	 * @throws ServerApiException
	 */
	public void addNewImageSharingListener(INewImageSharingListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Add an image sharing invitation listener");
		}

		listeners.register(listener);
	}

	/**
	 * Unregisters an image sharing invitation listener
	 * 
	 * @param listener New image sharing listener
	 * @throws ServerApiException
	 */
	public void removeNewImageSharingListener(INewImageSharingListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("Remove an image sharing invitation listener");
		}

		listeners.unregister(listener);
	}
}
