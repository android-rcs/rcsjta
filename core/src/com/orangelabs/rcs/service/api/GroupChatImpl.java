package com.orangelabs.rcs.service.api;

import java.util.List;

import android.os.RemoteCallbackList;
import android.os.RemoteException;

import com.gsma.services.rcs.JoynServiceException;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatMessage;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.IGroupChat;
import com.gsma.services.rcs.chat.IGroupChatListener;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.ft.IFileTransfer;
import com.gsma.services.rcs.ft.IFileTransferListener;
import com.orangelabs.rcs.core.Core;
import com.orangelabs.rcs.core.content.ContentManager;
import com.orangelabs.rcs.core.content.MmContent;
import com.orangelabs.rcs.core.ims.protocol.sip.SipDialogPath;
import com.orangelabs.rcs.core.ims.service.ImsServiceSession;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatError;
import com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.GeolocPush;
import com.orangelabs.rcs.core.ims.service.im.chat.GroupChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage;
import com.orangelabs.rcs.core.ims.service.im.chat.OriginatingAdhocGroupChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.RejoinGroupChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.RestartGroupChatSession;
import com.orangelabs.rcs.core.ims.service.im.chat.event.User;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.orangelabs.rcs.platform.file.FileDescription;
import com.orangelabs.rcs.platform.file.FileFactory;
import com.orangelabs.rcs.provider.messaging.RichMessagingHistory;
import com.orangelabs.rcs.utils.IdGenerator;
import com.orangelabs.rcs.utils.PhoneUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Group chat implementation
 * 
 * @author Jean-Marc AUFFRET
 */
public class GroupChatImpl extends IGroupChat.Stub implements ChatSessionListener {
	
	/**
	 * Core session
	 */
	private GroupChatSession session;
	
	/**
	 * List of listeners
	 */
	private RemoteCallbackList<IGroupChatListener> listeners = new RemoteCallbackList<IGroupChatListener>();

	/**
	 * Lock used for synchronisation
	 */
	private Object lock = new Object();

	/**
	 * The logger
	 */
	private Logger logger = Logger.getLogger(this.getClass().getName());

	/**
	 * Constructor
	 * 
	 * @param session Session
	 */
	public GroupChatImpl(GroupChatSession session) {
		this.session = session;
		
		session.addListener(this);
	}
	
	/**
	 * Get chat ID
	 * 
	 * @return Chat ID
	 */
	public String getChatId() {
		return session.getContributionID();
	}
	
	/**
	 * Get remote contact
	 * 
	 * @return Contact
	 */
	public String getRemoteContact() {
		return PhoneUtils.extractNumberFromUri(session.getRemoteContact());
	}
	
	/**
	 * Returns the direction of the group chat (incoming or outgoing)
	 * 
	 * @return Direction
	 */
	public int getDirection() {
		if ((session instanceof OriginatingAdhocGroupChatSession) ||
				(session instanceof RejoinGroupChatSession) ||
					(session instanceof RestartGroupChatSession)) {
			return GroupChat.Direction.OUTGOING;
		} else {
			return GroupChat.Direction.INCOMING;
		}
	}		
	
	/**
	 * Returns the state of the group chat
	 * 
	 * @return State 
	 */
	public int getState() {
		int result = GroupChat.State.UNKNOWN;
		SipDialogPath dialogPath = session.getDialogPath();
		if (dialogPath != null) {
			if (dialogPath.isSessionCancelled()) {
				// Session canceled
				result = GroupChat.State.ABORTED;
			} else
			if (dialogPath.isSessionEstablished()) {
				// Session started
				result = GroupChat.State.STARTED;
			} else
			if (dialogPath.isSessionTerminated()) {
				// Session terminated
				result = GroupChat.State.TERMINATED;
			} else {
				// Session pending
				if ((session instanceof OriginatingAdhocGroupChatSession) ||
						(session instanceof RestartGroupChatSession) ||
						(session instanceof RejoinGroupChatSession)) {
					result = GroupChat.State.INITIATED;
				} else {
					result = GroupChat.State.INVITED;
				}
			}
		}
		return result;			
	}		
	
	/**
	 * Is Store & Forward
	 * 
	 * @return Boolean
	 */
	public boolean isStoreAndForward() {
		return session.isStoreAndForward();
	}
	
	/**
	 * Get subject associated to the session
	 * 
	 * @return String
	 */
	public String getSubject() {
		return session.getSubject();
	}

	/**
	 * Accepts chat invitation
	 */
	public void acceptInvitation() {
		if (logger.isActivated()) {
			logger.info("Accept session invitation");
		}
				
		// Accept invitation
        Thread t = new Thread() {
    		public void run() {
    			session.acceptSession();
    		}
    	};
    	t.start();
	}
	
	/**
	 * Rejects chat invitation
	 */ 
	public void rejectInvitation() {
		if (logger.isActivated()) {
			logger.info("Reject session invitation");
		}

		// Update rich messaging history
		RichMessagingHistory.getInstance().updateGroupChatStatus(getChatId(), GroupChat.State.ABORTED);
		
        // Reject invitation
        Thread t = new Thread() {
    		public void run() {
    			session.rejectSession(603);
    		}
    	};
    	t.start();
	}

	/**
	 * Quits a group chat conversation. The conversation will continue between
	 * other participants if there are enough participants.
	 */
	public void quitConversation() {
		if (logger.isActivated()) {
			logger.info("Cancel session");
		}
		
		// Abort the session
        Thread t = new Thread() {
    		public void run() {
    			session.abortSession(ImsServiceSession.TERMINATION_BY_USER);
    		}
    	};
    	t.start();
	}
	
	/**
	 * Returns the list of connected participants. A participant is identified
	 * by its MSISDN in national or international format, SIP address, SIP-URI or Tel-URI.
	 * 
	 * @return List of participants
	 */
	public List<String> getParticipants() {
		if (logger.isActivated()) {
			logger.info("Get list of connected participants in the session");
		}
		return session.getConnectedParticipants().getList();
	}

	/**
	 * Returns the max number of participants for a group chat from the group
	 * chat info subscription (this value overrides the provisioning parameter)
	 * 
	 * @return Number
	 */
	public int getMaxParticipants() {
        if (logger.isActivated()) {
            logger.info("Get max number of participants in the session");
        }
        return session.getMaxParticipants();
    }

	/**
	 * Adds participants to a group chat
	 * 
	 * @param participants List of participants
	 */
	public void addParticipants(final List<String> participants) {
		if (logger.isActivated()) {
			logger.info("Add " + participants.size() + " participants to the session");
		}

		int max = session.getMaxParticipants()-1;
		int connected = session.getConnectedParticipants().getList().size(); 
        if (connected < max) {
            // Add a list of participants to the session
	        Thread t = new Thread() {
	    		public void run() {
	                session.addParticipants(participants);
	    		}
	    	};
	    	t.start();
        } else {
        	// Max participants achieved
            handleAddParticipantFailed("Maximum number of participants reached");
        }
	}
	
	/**
	 * Sends a text message to the group
	 * 
	 * @param text Message
	 * @return Message ID
	 */
	public String sendMessage(final String text) {
		// Generate a message Id
		final String msgId = IdGenerator.generateMessageID();

		// Send text message
        Thread t = new Thread() {
    		public void run() {
    			session.sendTextMessage(msgId, text);
    		}
    	};
    	t.start();

		return msgId;
	}
	
	/**
     * Sends a geoloc message
     * 
     * @param geoloc Geoloc
     * @return Unique message ID or null in case of error
     */
    public String sendGeoloc(Geoloc geoloc) {
		// Generate a message Id
		final String msgId = IdGenerator.generateMessageID();

		// Send geoloc message
		final GeolocPush geolocPush = new GeolocPush(geoloc.getLabel(),
				geoloc.getLatitude(), geoloc.getLongitude(),
				geoloc.getExpiration(), geoloc.getAccuracy());
        Thread t = new Thread() {
    		public void run() {
    			session.sendGeolocMessage(msgId, geolocPush);
    		}
    	};
    	t.start();
		return msgId;
    }	

    /**
     * Transfers a file to participants. The parameter filename contains the complete
     * path of the file to be transferred.
     * 
     * @param filename Filename to transfer
     * @param fileicon Filename of the file icon associated to the file to be transfered
     * @param listener File transfer event listener
     * @return File transfer
     * @throws ServerApiException 
     */
	public IFileTransfer sendFile(String filename, String fileicon, IFileTransferListener listener) throws ServerApiException {
		if (logger.isActivated()) {
			logger.info("sendFile (filename=" + filename + ") (fileicon=" + fileicon + ")");
		}
		try {
			// Initiate the session
			FileDescription desc = FileFactory.getFactory().getFileDescription(filename);
			MmContent content = ContentManager.createMmContentFromUrl(filename, desc.getSize());

			String chatSessionId = session.getSessionID();
			String chatId = session.getContributionID();
			final FileSharingSession fileSharingsession = Core.getInstance().getImService()
					.initiateGroupFileTransferSession(getParticipants(), content, fileicon, chatSessionId, chatId);

			// Add session listener
			FileTransferImpl sessionApi = new FileTransferImpl(fileSharingsession);
			sessionApi.addEventListener(listener);

			// Update rich messaging history
			RichMessagingHistory.getInstance().addOutgoingGroupFileTransfer(chatSessionId, fileSharingsession.getSessionID(),
					fileSharingsession.getContent());

			// Start the session
			new Thread() {
				public void run() {
					// Start the session
					fileSharingsession.startSession();
				}
			}.start();

			// Add session in the list
			FileTransferServiceImpl.addFileTransferSession(sessionApi);
			return sessionApi;
		} catch (Exception e) {
			if (logger.isActivated()) {
				logger.error("Unexpected error", e);
			}
			throw new ServerApiException(e.getMessage());
		}
	}

    /**
	 * Sends a “is-composing” event. The status is set to true when typing
	 * a message, else it is set to false.
	 * 
	 * @param status Is-composing status
	 */
	public void sendIsComposingEvent(final boolean status) {
        Thread t = new Thread() {
    		public void run() {
    			session.sendIsComposingStatus(status);
    		}
    	};
    	t.start();
	}
	
    /**
     * Sends a displayed delivery report for a given message ID
     * 
     * @param msgId Message ID
     */
    public void sendDisplayedDeliveryReport(final String msgId) {
		try {
			if (logger.isActivated()) {
				logger.debug("Set displayed delivery report for " + msgId);
			}
			
			// Send MSRP delivery status
	        Thread t = new Thread() {
	    		public void run() {
	    			session.sendMsrpMessageDeliveryStatus(session.getRemoteContact(), msgId, ImdnDocument.DELIVERY_STATUS_DISPLAYED);
	    		}
	    	};
	    	t.start();
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Could not send MSRP delivery status",e);
			}
		}
    }	
	
	/**
	 * Adds a listener on chat events
	 * 
	 * @param listener Group chat event listener 
	 */
	public void addEventListener(IGroupChatListener listener) {
		if (logger.isActivated()) {
			logger.info("Add an event listener");
		}

    	synchronized(lock) {
    		listeners.register(listener);
    	}
	}
	
	/**
	 * Removes a listener on chat events
	 * 
	 * @param listener Group chat event listener 
	 */
	public void removeEventListener(IGroupChatListener listener) {
		if (logger.isActivated()) {
			logger.info("Remove an event listener");
		}

    	synchronized(lock) {
    		listeners.unregister(listener);
    	}
	}
	
    /*------------------------------- SESSION EVENTS ----------------------------------*/
	
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.ImsSessionListener#handleSessionStarted()
     */
    public void handleSessionStarted() {
    	synchronized(lock) {
	    	if (logger.isActivated()) {
				logger.info("Session started");
			}

			// Update rich messaging history
			RichMessagingHistory.getInstance().updateGroupChatStatus(getChatId(), GroupChat.State.STARTED);
			RichMessagingHistory.getInstance().updateGroupChatRejoinId(getChatId(), session.getImSessionIdentity());
			
			// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onSessionStarted();
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	    }
    }
    
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.ImsSessionListener#handleSessionAborted(int)
     */
    public void handleSessionAborted(int reason) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Session aborted (reason " + reason + ")");
			}
	
			// Update rich messaging history
			if (reason == ImsServiceSession.TERMINATION_BY_USER) {
				RichMessagingHistory.getInstance().updateGroupChatStatus(getChatId(), GroupChat.State.CLOSED_BY_USER);
			} else {
				if (session.getDialogPath().isSessionCancelled()) {
					RichMessagingHistory.getInstance().updateGroupChatStatus(getChatId(), GroupChat.State.ABORTED);
				} else {
					RichMessagingHistory.getInstance().updateGroupChatStatus(getChatId(), GroupChat.State.TERMINATED);
				}
			}
			
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onSessionAborted();
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	        
	        // Remove session from the list
	        ChatServiceImpl.removeGroupChatSession(getChatId());
	    }
    }
    
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.ImsSessionListener#handleSessionTerminatedByRemote()
     */
    public void handleSessionTerminatedByRemote() {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Session terminated by remote");
			}
	
			// Update rich messaging history
			if (session.getDialogPath().isSessionCancelled()) {
				RichMessagingHistory.getInstance().updateGroupChatStatus(getChatId(), GroupChat.State.ABORTED);
			} else {
				RichMessagingHistory.getInstance().updateGroupChatStatus(getChatId(), GroupChat.State.TERMINATED);
			}
			
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onSessionAborted();
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	        
	        // Remove session from the list
	        ChatServiceImpl.removeGroupChatSession(getChatId());
	    }
    }
    
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#handleReceiveMessage(com.orangelabs.rcs.core.ims.service.im.chat.InstantMessage)
     */
    public void handleReceiveMessage(InstantMessage message) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("New IM received");
			}
			
			// Update rich messaging history
			RichMessagingHistory.getInstance().addGroupChatMessage(session.getContributionID(),
					message, ChatLog.Message.Direction.INCOMING);
			
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	ChatMessage msgApi = new ChatMessage(message.getMessageId(),
	            			PhoneUtils.extractNumberFromUri(message.getRemote()),
	            			message.getTextMessage(),
	            			message.getServerDate(), message.isImdnDisplayedRequested());
	            	listeners.getBroadcastItem(i).onNewMessage(msgApi);
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();		
	    }
    }
    
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#handleImError(com.orangelabs.rcs.core.ims.service.im.chat.ChatError)
     */
    public void handleImError(ChatError error) {
    	synchronized(lock) {
			if (error.getErrorCode() == ChatError.SESSION_INITIATION_CANCELLED) {
				// Do nothing here, this is an aborted event
				return;
			}
    		
			if (logger.isActivated()) {
				logger.info("IM error " + error.getErrorCode());
			}
			
			// Update rich messaging history
			switch(error.getErrorCode()){
	    		case ChatError.SESSION_NOT_FOUND:
	    		case ChatError.SESSION_RESTART_FAILED:
	    			// These errors are not logged
	    			break;
		    	default:
					RichMessagingHistory.getInstance().updateGroupChatStatus(session.getContributionID(), GroupChat.State.FAILED);
		    		break;
	    	}
	    	
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	int code;
	            	switch(error.getErrorCode()) {
            			case ChatError.SESSION_INITIATION_DECLINED:
	            			code = GroupChat.Error.INVITATION_DECLINED;
	            			break;
            			case ChatError.SESSION_NOT_FOUND:
	            			code = GroupChat.Error.CHAT_NOT_FOUND;
	            			break;
	            		default:
	            			code = GroupChat.Error.CHAT_FAILED;
	            	}
	            	listeners.getBroadcastItem(i).onSessionError(code);
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	    	
	        // Remove session from the list
	        ChatServiceImpl.removeGroupChatSession(getChatId());
	    }
    }
    
	/* (non-Javadoc)
	 * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#handleIsComposingEvent(java.lang.String, boolean)
	 */
	public void handleIsComposingEvent(String contact, boolean status) {
    	synchronized(lock) {
        	contact = PhoneUtils.extractNumberFromUri(contact);

        	if (logger.isActivated()) {
				logger.info(contact + " is composing status set to " + status);
			}
	
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	listeners.getBroadcastItem(i).onComposingEvent(contact, status);
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
		}
	}
	
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#handleConferenceEvent(java.lang.String, java.lang.String, java.lang.String)
     */
    public void handleConferenceEvent(String contact, String contactDisplayname, String state) {
    	synchronized(lock) {
        	contact = PhoneUtils.extractNumberFromUri(contact);

        	if (logger.isActivated()) {
				logger.info("New conference event " + state + " for " + contact);
			}
			
	  		// Update history and notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	if (state.equals(User.STATE_CONNECTED)) {
	        			// Update rich messaging history
	        			RichMessagingHistory.getInstance().addGroupChatSystemMessage(session.getContributionID(), contact, ChatLog.Message.Status.System.JOINED);

	        	  		// Notify event listener
	        			listeners.getBroadcastItem(i).onParticipantJoined(contact, contactDisplayname);
	            	} else
	            	if (state.equals(User.STATE_DISCONNECTED)) {
	        			// Update rich messaging history
	        			RichMessagingHistory.getInstance().addGroupChatSystemMessage(session.getContributionID(), contact, ChatLog.Message.Status.System.DISCONNECTED);

	        	  		// Notify event listener
	        			listeners.getBroadcastItem(i).onParticipantDisconnected(contact);
	            	} else
	            	if (state.equals(User.STATE_DEPARTED)) {
	        			// Update rich messaging history
	        			RichMessagingHistory.getInstance().addGroupChatSystemMessage(session.getContributionID(), contact, ChatLog.Message.Status.System.GONE);

	        	  		// Notify event listener
	        			listeners.getBroadcastItem(i).onParticipantLeft(contact);
	            	}
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	    }
    }
    
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#handleMessageDeliveryStatus(java.lang.String, java.lang.String, java.lang.String)
     */
    public void handleMessageDeliveryStatus(String msgId, String status, String contact) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("New message delivery status for message " + msgId + ", status " + status);
			}
	
			// Update rich messaging history
			RichMessagingHistory.getInstance().updateChatMessageDeliveryStatus(msgId, status, contact);
        	
			// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	if (status.equals(ImdnDocument.DELIVERY_STATUS_DELIVERED)) {
	            		listeners.getBroadcastItem(i).onReportMessageDelivered(msgId);
	            	} else
	            	if (status.equals(ImdnDocument.DELIVERY_STATUS_DISPLAYED)) {
	        			listeners.getBroadcastItem(i).onReportMessageDisplayed(msgId);
	            	} else
	            	if (status.equals(ImdnDocument.DELIVERY_STATUS_ERROR)) {
	            		listeners.getBroadcastItem(i).onReportMessageFailed(msgId);
	            	}
	            } catch(Exception e) {
	            	if (logger.isActivated()) {
	            		logger.error("Can't notify listener", e);
	            	}
	            }
	        }
	        listeners.finishBroadcast();
	    }
    }
    
    /* (non-Javadoc)
     * @see com.orangelabs.rcs.core.ims.service.im.chat.ChatSessionListener#handleAddParticipantSuccessful()
     */
    public void handleAddParticipantSuccessful() {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Add participant request is successful");
			}
	
			// TODO: nothing send over API?
	    }
    }
    
    /**
     * Request to add participant has failed
     * 
     * @param reason Error reason
     */
    public void handleAddParticipantFailed(String reason) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("Add participant request has failed " + reason);
			}
	
			// TODO: nothing send over API?
	    }  
    }

    /**
     * New geoloc message received
     * 
     * @param geoloc Geoloc message
     */
    public void handleReceiveGeoloc(GeolocMessage geoloc) {
    	synchronized(lock) {
			if (logger.isActivated()) {
				logger.info("New geoloc received");
			}
			
			// Update rich messaging history
			RichMessagingHistory.getInstance().addGroupChatMessage(session.getContributionID(),
					geoloc, ChatLog.Message.Direction.INCOMING);
			
	  		// Notify event listeners
			final int N = listeners.beginBroadcast();
	        for (int i=0; i < N; i++) {
	            try {
	            	Geoloc geolocApi = new Geoloc(geoloc.getGeoloc().getLabel(),
	            			geoloc.getGeoloc().getLatitude(), geoloc.getGeoloc().getLongitude(),
	            			geoloc.getGeoloc().getExpiration());
	            	com.gsma.services.rcs.chat.GeolocMessage msgApi = new com.gsma.services.rcs.chat.GeolocMessage(geoloc.getMessageId(),
	            			PhoneUtils.extractNumberFromUri(geoloc.getRemote()),
	            			geolocApi, geoloc.getDate(), geoloc.isImdnDisplayedRequested());
	            	listeners.getBroadcastItem(i).onNewGeoloc(msgApi);
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
	 * Returns the list of participants in the group conversation
	 * 
	 * @return List of participants
	 * @throws JoynServiceException
     * @see ParticipantInfo
	 */
    public List<ParticipantInfo> getParticipantInfo() throws RemoteException {
		if (logger.isActivated()) {
			logger.info("Get list of participant information");
		}
		// TODO FUSION return List<ParticipantInfo> from session
		return null;
	}
}