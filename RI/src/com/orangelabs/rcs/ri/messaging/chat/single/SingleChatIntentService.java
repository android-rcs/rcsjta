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
package com.orangelabs.rcs.ri.messaging.chat.single;

import java.util.Calendar;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.OneToOneChatIntent;
import com.gsma.services.rcs.contacts.ContactId;
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
public class SingleChatIntentService extends IntentService {

	/**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(SingleChatIntentService.class.getSimpleName());

	/**
	 * Creates an IntentService.
	 * @param name of the thread
	 */
	public SingleChatIntentService(String name) {
		super(name);
	}

	/**
	 * Creates an IntentService.
	 */
	public SingleChatIntentService() {
		super("SingleChatIntentService");
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
		String action = intent.getAction();
		if (SingleChatInvitationReceiver.ACTION_NEW_121_CHAT_MSG.equals(action)) {
			handleNewOneToOneChatMessage(intent);
		} else {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Unknown action ".concat(action));
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
		if (msgId == null) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Cannot read message ID");
			}
			return;
		}
		String mimeType = messageIntent.getStringExtra(OneToOneChatIntent.EXTRA_MIME_TYPE);
		if (mimeType == null) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Cannot read message mime-type");
			}
			return;
		}
		try {
			// Read message from provider
			ChatMessageDAO messageDAO = new ChatMessageDAO(this, msgId);
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "One to one chat message ".concat(messageDAO.toString()));
			}
			forwardSingleChatMessage2UI(this, messageDAO);
		} catch (Exception e) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Cannot read chat message from provider", e);
			}
		}
	}

	/**
	 * Forward one to one chat message to view activity
	 * 
	 * @param context
	 *            Context
	 * @param message
	 *            the chat message DAO
	 */
	private void forwardSingleChatMessage2UI(Context context, ChatMessageDAO message) {
		ContactId contact = message.getContact();
		if (contact == null) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Contact is null");
			}
			return;
		}
		String mimeType = message.getMimeType();
		String content = message.getContent();
		Intent intent = SingleChatView.forgeIntentToStart(context, contact);
		// Do not display notification if activity is on foreground for this contact
		if (SingleChatView.isDisplayed() && contact.equals(SingleChatView.contactOnForeground)) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, new StringBuilder("New message '").append(content)
						.append("' for contact ").append(contact.toString()).toString());
			}
			context.startActivity(intent);
		} else {
			// Create notification
			PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			String displayName = RcsDisplayName.getInstance(this).getDisplayName(contact);
			String title = context.getString(R.string.title_recv_chat, displayName);
			String msg;
			if (ChatLog.Message.MimeType.GEOLOC_MESSAGE.equals(mimeType)) {
				msg = context.getString(R.string.label_geoloc_msg);
			} else {
				if (ChatLog.Message.MimeType.TEXT_MESSAGE.equals(mimeType)) {
					msg = content;
				} else {
					if (LogUtils.isActive) {
						Log.e(LOGTAG, "Unknown message type '".concat(mimeType));
					}
					return;
				}
			}
			// Create notification
			Notification notif = buildNotification(contentIntent, title, msg);

			// Send notification
			NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
			notificationManager.notify(contact.toString(), Utils.NOTIF_ID_SINGLE_CHAT, notif);
		}
	}


	/**
	 * Generate a notification
	 * @param invitation
	 * @param title
	 * @param message
	 * @return the notification
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
