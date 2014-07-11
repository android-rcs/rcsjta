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
package com.orangelabs.rcs.ri.extension;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.util.Log;

import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.extension.MultimediaMessagingSessionIntent;
import com.gsma.services.rcs.extension.MultimediaStreamingSessionIntent;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.extension.messaging.MessagingSessionView;
import com.orangelabs.rcs.ri.extension.streaming.StreamingSessionView;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Messaging session invitation receiver
 *  
 * @author Jean-Marc AUFFRET
 */
public class SessionInvitationReceiver extends BroadcastReceiver {
	
	/**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(SessionInvitationReceiver.class.getSimpleName());
	
	@Override
	public void onReceive(Context context, Intent intent) {
        // Display invitation notification
		SessionInvitationReceiver.addSessionInvitationNotification(context, intent);
    }

    /**
     * Add session invitation notification
     * 
     * @param context Context
     * @param invitation Intent invitation
     */
	public static void addSessionInvitationNotification(Context context, Intent invitation) {
    	// Get remote contact and session
		ContactId contact = null;
		String sessionId = null;
		if (invitation.getAction().equals(MultimediaMessagingSessionIntent.ACTION_NEW_INVITATION)) {
			contact = invitation.getParcelableExtra(MultimediaMessagingSessionIntent.EXTRA_CONTACT);
			sessionId = invitation.getStringExtra(MultimediaMessagingSessionIntent.EXTRA_SESSION_ID);
		} else {
			contact = invitation.getParcelableExtra(MultimediaStreamingSessionIntent.EXTRA_CONTACT);
			sessionId = invitation.getStringExtra(MultimediaStreamingSessionIntent.EXTRA_SESSION_ID);
		}
		if (contact == null) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "SessionInvitationReceiver failed: cannot parse contact");
			}
			return;
		}
		
		// Create notification
		Class myClass;
        String notifTitle;
		if (invitation.getAction().equals(MultimediaMessagingSessionIntent.ACTION_NEW_INVITATION)) {
			myClass = MessagingSessionView.class;
	        notifTitle = context.getString(R.string.title_recv_messaging_session);
		} else {
			myClass = StreamingSessionView.class;
			notifTitle = context.getString(R.string.title_recv_streaming_session);
		}
		Intent intent = new Intent(invitation);
		intent.setClass(context, myClass);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(sessionId);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		Notification notif = new Notification(R.drawable.ri_notif_mm_session_icon, notifTitle, System.currentTimeMillis());
        notif.flags = Notification.FLAG_AUTO_CANCEL;
        notif.setLatestEventInfo(context, notifTitle, context.getString(R.string.label_session_from, contact.toString()), contentIntent);
		notif.sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    	notif.defaults |= Notification.DEFAULT_VIBRATE;
        
        // Send notification
		NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(sessionId, Utils.NOTIF_ID_MM_SESSION, notif);
    }
    
    /**
     * Remove session invitation notification
     * 
     * @param context Context
     * @param sessionId Session ID
     */
    public static void removeSessionInvitationNotification(Context context, String sessionId) {
		NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(sessionId, Utils.NOTIF_ID_MM_SESSION);
    }
}
