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
package org.gsma.joyn.samples.session;

import org.gsma.joyn.samples.session.utils.Utils;
import org.gsma.joyn.session.MultimediaSessionIntent;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;


/**
 * Multimedia session invitation receiver
 *  
 * @author Jean-Marc AUFFRET
 */
public class MultimediaSessionInvitationReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
        // Display invitation notification
		MultimediaSessionInvitationReceiver.addSessionInvitationNotification(context, intent);
    }

    /**
     * Add session invitation notification
     * 
     * @param context Context
     * @param invitation Intent invitation
     */
	public static void addSessionInvitationNotification(Context context, Intent invitation) {
    	// Get remote contact
		String contact = invitation.getStringExtra(MultimediaSessionIntent.EXTRA_CONTACT);

		// Get session ID
		String sessionId = invitation.getStringExtra(MultimediaSessionIntent.EXTRA_SESSION_ID);

		// Create notification
		Intent intent = new Intent(invitation);
		intent.setClass(context, MultimediaSessionView.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(sessionId);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        String notifTitle = context.getString(R.string.title_invitation, contact);
		Notification notif = new Notification(R.drawable.notif_invitation_icon, notifTitle, System.currentTimeMillis());
        notif.flags = Notification.FLAG_AUTO_CANCEL;
        notif.setLatestEventInfo(context, notifTitle, getString(R.string.label_invittaion), contentIntent);
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
