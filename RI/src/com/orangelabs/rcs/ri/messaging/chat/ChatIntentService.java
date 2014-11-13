/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.orangelabs.rcs.ri.messaging.chat;

import java.util.Calendar;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.gsma.services.rcs.RcsCommon;
import com.gsma.services.rcs.chat.OneToOneChatIntent;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.GroupChatIntent;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.ri.ApiConnectionManager;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * File transfer intent service
 * 
 * @author YPLO6403
 * 
 */
public class ChatIntentService extends IntentService {

	/**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(ChatIntentService.class.getSimpleName());

	/* package private */static final String BUNDLE_CHATMESSAGE_DAO_ID = "ChatMessageDao";
	/* package private */static final String BUNDLE_GROUPCHAT_DAO_ID = "GroupChatDao";

	public ChatIntentService(String name) {
		super(name);
	}

	public ChatIntentService() {
		super("ChatIntentService");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		// We want this service to stop running if forced stop
		// so return not sticky.
		return START_NOT_STICKY;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent == null || intent.getAction() == null) {
			return;
		}
		if (intent.getAction().equalsIgnoreCase(SingleChatInvitationReceiver.ACTION_NEW_121_CHAT_MSG)) {
			handleNewOneToOneChatMessage(intent);
		} else {
			if (intent.getAction().equalsIgnoreCase(GroupChatMessageReceiver.ACTION_NEW_GC_MSG)) {
				handleNewGroupChatMessage(intent);
			} else {
				if (intent.getAction().equalsIgnoreCase(GroupChatInvitationReceiver.ACTION_NEW_GC)) {
					handleNewGroupChatInvitation(intent);
				} else {
					if (LogUtils.isActive) {
						Log.e(LOGTAG, "Unknown action " + intent.getAction());
					}
				}
			}
		}
	}

	/**
	 * Handle new one to one chat message
	 * 
	 * @param messageIntent
	 *            intent with chat message
	 */
	private void handleNewOneToOneChatMessage(Intent messageIntent) {
		// Gets data from the incoming Intent
		String msgId = messageIntent.getStringExtra(OneToOneChatIntent.EXTRA_MESSAGE_ID);
		if (msgId != null) {
			try {
				// Read message from provider
				ChatMessageDAO messageDAO = new ChatMessageDAO(this, msgId);
				// Save ChatMessageDAO into intent
				Bundle bundle = new Bundle();
				bundle.putParcelable(BUNDLE_CHATMESSAGE_DAO_ID, messageDAO);
				messageIntent.putExtras(bundle);
				if (LogUtils.isActive) {
					Log.d(LOGTAG, "One to one chat message " + messageDAO);
				}
				forwardSingleChatMessage2UI(this, messageIntent, messageDAO);
			} catch (Exception e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "Cannot read chat message from provider", e);
				}
			}
		} else {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Cannot read message ID");
			}
		}
	}

	/**
	 * Handle Group chat invitation
	 * 
	 * @param invitation
	 */
	private void handleNewGroupChatInvitation(Intent invitation) {
		// Gets data from the incoming Intent
		String chatId = invitation.getStringExtra(GroupChatIntent.EXTRA_CHAT_ID);
		if (chatId != null) {
			try {
				// Get CHAT from provider
				GroupChatDAO groupChatDAO = new GroupChatDAO(this, chatId);
				// Save ChatDAO into intent
				Bundle bundle = new Bundle();
				bundle.putParcelable(BUNDLE_GROUPCHAT_DAO_ID, groupChatDAO);
				invitation.putExtras(bundle);
				if (LogUtils.isActive) {
					Log.d(LOGTAG, "Group chat chat message =" + groupChatDAO);
				}
				forwardGCInvitation2UI(this, invitation, groupChatDAO);
			} catch (Exception e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "Cannot read group chat from provider", e);
				}
			}
		} else {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Cannot read group chat ID");
			}
		}
	}

	/**
	 * Handle new group chat message
	 * 
	 * @param messageIntent
	 *            intent with chat message
	 */
	private void handleNewGroupChatMessage(Intent messageIntent) {
		// Gets data from the incoming Intent
		String msgId = messageIntent.getStringExtra(GroupChatIntent.EXTRA_MESSAGE_ID);
		if (msgId != null) {
			try {
				// Get ChatMessage from provider
				ChatMessageDAO messageDAO = new ChatMessageDAO(this, msgId);
				// Save ChatMessageDAO into intent
				Bundle bundle = new Bundle();
				bundle.putParcelable(BUNDLE_CHATMESSAGE_DAO_ID, messageDAO);
				messageIntent.putExtras(bundle);
				if (LogUtils.isActive) {
					Log.d(LOGTAG, "Group chat chat message =" + messageDAO);
				}
				forwardGCMessage2UI(this, messageIntent, messageDAO);
			} catch (Exception e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "Cannot read chat message from provider", e);
				}
			}
		} else {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Cannot read message ID");
			}
		}
	}

	/**
	 * Forward Group CHAT message to view activity
	 * 
	 * @param context
	 * @param newMessageIntent
	 *            new message intent
	 * @param message
	 *            message
	 */
	private void forwardGCMessage2UI(Context context, Intent newMessageIntent, ChatMessageDAO message) {
		if (message.getContact() == null) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Contact is null");
			}
			return;
		}
		// Create notification
		Intent intent = new Intent(newMessageIntent);
		intent.setClass(context, GroupChatView.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(ChatView.EXTRA_MODE, ChatView.MODE_INCOMING);

		// Do not display notification if activity is on foreground for this ChatID
		if (ChatView.isDisplayed() && message.getChatId().equals(ChatView.keyChat)) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "New message '" + message.getBody() + "' for chatId " + message.getChatId());
			}
			context.startActivity(intent);
		} else {
			PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			ContactId contact = message.getContact();
			String displayName = RcsDisplayName.get(context, contact);
			displayName = RcsDisplayName.convert(context, RcsCommon.Direction.INCOMING, contact, displayName);
			String title = context.getString(R.string.title_recv_chat, displayName);
			
			String msg;
			if (message.getMimeType().equals(ChatLog.Message.MimeType.GEOLOC_MESSAGE)) {
				msg = context.getString(R.string.label_geoloc_msg);
			} else {
				if (message.getMimeType().equals(ChatLog.Message.MimeType.TEXT_MESSAGE)) {
					msg = message.getBody();
				} else  {
					if (message.getMimeType().equals(ChatLog.Message.MimeType.GROUPCHAT_EVENT)) {
						// TODO handle GC event
						return;
					} else {
						if (LogUtils.isActive) {
							Log.w(LOGTAG, "Unknown message type '" + message.getMimeType());
						}
						return;
					}
				}
			}

			// Create notification
			Notification notif = buildNotification(context, contentIntent, title, msg);

			// Send notification
			NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.notify(message.getChatId(), Utils.NOTIF_ID_GROUP_CHAT, notif);
		}
	}

	/**
	 * Forward one to one chat message to view activity
	 * 
	 * @param context
	 *            Context
	 * @param newMessageIntent
	 *            Intent for new message
	 * @param message
	 *            the chat message DAO
	 */
	private void forwardSingleChatMessage2UI(Context context, Intent newMessageIntent, ChatMessageDAO message) {
		if (message.getContact() == null) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Contact is null");
			}
			return;
		}
		Intent intent = new Intent(newMessageIntent);
		intent.setClass(context, SingleChatView.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(ChatView.EXTRA_MODE, ChatView.MODE_INCOMING);

		// Do not display notification if activity is on foreground for this contact
		if (ChatView.isDisplayed() && message.getContact().equals(ChatView.keyChat)) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "New message '" + message.getBody() + "' for contact " + message.getContact());
			}
			context.startActivity(intent);
		} else {
			// Create notification
			PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			ContactId contact = message.getContact();
			String displayName = RcsDisplayName.get(context, contact);
			displayName = RcsDisplayName.convert(context, RcsCommon.Direction.INCOMING, contact, displayName);
			String title = context.getString(R.string.title_recv_chat, displayName);
			String msg;
			if (message.getMimeType().equals(ChatLog.Message.MimeType.GEOLOC_MESSAGE)) {
				msg = context.getString(R.string.label_geoloc_msg);
			} else {
				if (message.getMimeType().equals(ChatLog.Message.MimeType.TEXT_MESSAGE)) {
					msg = message.getBody();
				} else {
					if (LogUtils.isActive) {
						Log.w(LOGTAG, "Unknown message type '" + message.getMimeType());
					}
					return;
				}
			}
			// Create notification
			Notification notif = buildNotification(context, contentIntent, title, msg);

			// Send notification
			NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.notify(message.getContact().toString(), Utils.NOTIF_ID_SINGLE_CHAT, notif);
		}
	}

	/**
	 * Forward Group chat invitation to View Activity
	 * 
	 * @param context
	 *            Context
	 * @param invitation
	 *            Intent invitation
	 * @param groupChat
	 *            Group CHAT DAO
	 */
	public void forwardGCInvitation2UI(Context context, Intent invitation, GroupChatDAO groupChat) {
		// Get subject
		String subject = groupChat.getSubject();
		if (TextUtils.isEmpty(subject)) {
			subject = "<" + context.getString(R.string.label_no_subject) + ">";
		}
		// Create notification
		Intent intent = new Intent(invitation);
		intent.setClass(context, GroupChatView.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(ChatView.EXTRA_MODE, ChatView.MODE_INCOMING);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		String title = context.getString(R.string.title_group_chat);
		// Try to retrieve display name of remote contact
		String displayName = getGcDisplayNameOfRemoteContact(context, groupChat.getChatId());
		if (displayName != null) {
			title = context.getString(R.string.title_recv_group_chat, displayName);
		}
		String msg = context.getString(R.string.label_subject) + " " + subject;

		// Create notification
		Notification notif = buildNotification(context, contentIntent, title, msg);
					
		// Send notification
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(groupChat.getChatId(), Utils.NOTIF_ID_GROUP_CHAT, notif);
	}
	
	/**
	 * Get the RCS display name of remote contact in Group CHAT
	 * @param context
	 * @param chatId
	 * @return the RCS display name or null
	 */
	private String getGcDisplayNameOfRemoteContact(Context ctx, String chatId) {
		try {
			GroupChat gc = ApiConnectionManager.getInstance(ctx).getChatApi().getGroupChat(chatId);
			if (gc != null) {
				ContactId contact = gc.getRemoteContact();
				if (contact != null) {
					return RcsDisplayName.get(ctx, contact);
				}
			}
		} catch (Exception e) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Cannot get displayName", e);
			}
		}
		return null;
	}
	
	/**
	 * Generate a notification
	 * @param context
	 * @param invitation
	 * @param title
	 * @param message
	 * @return
	 */
	private Notification buildNotification(Context context, PendingIntent invitation, String title, String message) {
		// Create notification
		NotificationCompat.Builder notif = new NotificationCompat.Builder(context);
		notif.setContentIntent(invitation);
		notif.setSmallIcon(R.drawable.ri_notif_chat_icon);
		notif.setWhen(Calendar.getInstance().getTimeInMillis());
		notif.setAutoCancel(true);
		notif.setOnlyAlertOnce(true);
		notif.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
		notif.setDefaults(Notification.DEFAULT_VIBRATE);
		notif.setContentTitle(title);
		notif.setContentText(message);
		return notif.build();
	}
}
