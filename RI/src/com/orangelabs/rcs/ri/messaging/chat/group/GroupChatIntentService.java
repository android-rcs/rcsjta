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
package com.orangelabs.rcs.ri.messaging.chat.group;

import java.util.Calendar;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.GroupChatIntent;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.ri.ApiConnectionManager;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.messaging.chat.ChatMessageDAO;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * File transfer intent service
 * 
 * @author YPLO6403
 * 
 */
public class GroupChatIntentService extends IntentService {

	/**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(GroupChatIntentService.class.getSimpleName());

	public GroupChatIntentService(String name) {
		super(name);
	}

	public GroupChatIntentService() {
		super("GroupChatIntentService");
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
		if (intent == null) {
			return;
		}
		if (GroupChatMessageReceiver.ACTION_NEW_GC_MSG.equals(intent.getAction())) {
			// Gets message ID from the incoming Intent
			String messageId = intent.getStringExtra(GroupChatIntent.EXTRA_MESSAGE_ID);
			if (messageId != null) {
				handleNewGroupChatMessage(messageId);
			}
		} else {
			if (GroupChatInvitationReceiver.ACTION_NEW_GC.equals(intent.getAction())) {
				// Gets chat ID from the incoming Intent
				String chatId = intent.getStringExtra(GroupChatIntent.EXTRA_CHAT_ID);
				if (chatId != null) {
					handleNewGroupChatInvitation(chatId);
				}
			} else {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "Unknown action " + intent.getAction());
				}
			}
		}
	}

	/**
	 * Handle Group chat invitation
	 * 
	 * @param invitation
	 */
	private void handleNewGroupChatInvitation(String chatId) {
		try {
			// Get Chat from provider
			GroupChatDAO groupChatDAO = new GroupChatDAO(this, chatId);
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "Group chat invitation =" + groupChatDAO);
			}
			forwardGCInvitation2UI(chatId, groupChatDAO);
		} catch (Exception e) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Cannot read group chat from provider", e);
			}
		}
	}

	/**
	 * Handle new group chat message
	 * 
	 * @param messageIntent
	 *            intent with chat message
	 */
	private void handleNewGroupChatMessage(String messageId) {
		try {
			// Get ChatMessage from provider
			ChatMessageDAO messageDAO = new ChatMessageDAO(this, messageId);
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "Group chat message =" + messageDAO);
			}
			forwardGCMessage2UI(messageDAO);
		} catch (Exception e) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Cannot read chat message from provider", e);
			}
		}
	}

	/**
	 * Forward Group Chat message to view activity
	 * 
	 * @param message
	 *            message
	 */
	private void forwardGCMessage2UI(ChatMessageDAO message) {
		// Create intent
		Intent intent = GroupChatView.forgeIntentNewMessage(this, message);

		// Do not display notification if activity is on foreground for this ChatID
		if (GroupChatView.isDisplayed() && message.getChatId().equals(GroupChatView.chatIdOnForeground)) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "New message '" + message.getContent() + "' for chatId " + message.getChatId());
			}
			startActivity(intent);
		} else {
			// Create pending intent
			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			ContactId contact = message.getContact();
			String displayName = RcsDisplayName.getInstance(this).getDisplayName(contact);
			String title = getString(R.string.title_recv_chat, displayName);

			String msg;
			if (ChatLog.Message.MimeType.GEOLOC_MESSAGE.equals(message.getMimeType())) {
				msg = getString(R.string.label_geoloc_msg);
			} else {
				if (ChatLog.Message.MimeType.TEXT_MESSAGE.equals(message.getMimeType())) {
					msg = message.getContent();
				} else {
					// If the GC message does not convey user content then discards.
					if (LogUtils.isActive) {
						Log.w(LOGTAG, "Discard message of type '" + message.getMimeType() + "' for chatId " + message.getChatId());
					}
					return;
				}
			}

			// Create notification
			Notification notif = buildNotification(contentIntent, title, msg);

			// Send notification
			NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.notify(message.getChatId(), Utils.NOTIF_ID_GROUP_CHAT, notif);
		}
	}

	/**
	 * Forward Group chat invitation to View Activity
	 * 
	 * @param chatId
	 *            the Chat ID
	 * @param groupChat
	 *            Group Chat DAO
	 */
	public void forwardGCInvitation2UI(String chatId, GroupChatDAO groupChat) {
		// Get subject
		String subject = groupChat.getSubject();
		if (TextUtils.isEmpty(subject)) {
			subject = "<" + getString(R.string.label_no_subject) + ">";
		}
		// Create intent
		Intent intent = GroupChatView.forgeIntentInvitation(this, chatId, groupChat);
		// Create pending intent
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		String title = getString(R.string.title_group_chat);
		// Try to retrieve display name of remote contact
		String displayName = getGcDisplayNameOfRemoteContact(groupChat.getChatId());
		if (displayName != null) {
			title = getString(R.string.title_recv_group_chat, displayName);
		}
		String msg = getString(R.string.label_subject_notif, subject);

		// Create notification
		Notification notif = buildNotification(contentIntent, title, msg);

		// Send notification
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(chatId, Utils.NOTIF_ID_GROUP_CHAT, notif);
	}

	/**
	 * Get the RCS display name of remote contact in Group Chat
	 * 
	 * @param chatId
	 * @return the RCS display name or null
	 */
	private String getGcDisplayNameOfRemoteContact(String chatId) {
		try {
			GroupChat gc = ApiConnectionManager.getInstance(this).getChatApi().getGroupChat(chatId);
			if (gc != null) {
				ContactId contact = null; // TODO gc.getRemoteContact();
				return RcsDisplayName.getInstance(this).getDisplayName(contact);
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
	 * 
	 * @param invitation
	 * @param title
	 * @param message
	 * @return notification
	 */
	private Notification buildNotification(PendingIntent invitation, String title, String message) {
		// Create notification
		NotificationCompat.Builder notif = new NotificationCompat.Builder(this);
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
