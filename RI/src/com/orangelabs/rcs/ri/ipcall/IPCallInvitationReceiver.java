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
package com.orangelabs.rcs.ri.ipcall;

import org.gsma.joyn.ipcall.IPCallIntent;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;

/**
 * IP call invitation receiver
 * 
 * @author Jean-Marc AUFFRET
 */
public class IPCallInvitationReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
        // Display invitation notification
		IPCallInvitationReceiver.addSessionInvitationNotification(context, intent);
    }

    /**
     * Add call invitation notification
     * 
     * @param context Context
     * @param invitation Intent invitation
     */
	public static void addSessionInvitationNotification(Context context, Intent invitation) {
    	// Get remote contact
		String contact = invitation.getStringExtra(IPCallIntent.EXTRA_CONTACT);

		// Get call ID
		String callId = invitation.getStringExtra(IPCallIntent.EXTRA_CALL_ID);

		// Create notification
		Intent intent = new Intent(invitation);
		intent.setClass(context, IPCallView.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(callId);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        String notifTitle = context.getString(R.string.title_recv_ipcall);
		Notification notif = new Notification(R.drawable.ri_notif_ipcall_icon, notifTitle, System.currentTimeMillis());
        notif.flags = Notification.FLAG_AUTO_CANCEL;
        notif.setLatestEventInfo(context, notifTitle, context.getString(R.string.label_session_from, contact), contentIntent);
		notif.sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    	notif.defaults |= Notification.DEFAULT_VIBRATE;
        
        // Send notification
		NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(callId, Utils.NOTIF_ID_IP_CALL, notif);
    }
    
    /**
     * Remove call invitation notification
     * 
     * @param context Context
     * @param callId Call ID
     */
    public static void removeSessionInvitationNotification(Context context, String callId) {
		NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(callId, Utils.NOTIF_ID_IP_CALL);
    }
}
