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
package com.orangelabs.rcs.ri.session;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Handler;

import com.gsma.services.rcs.session.MultimediaMessageIntent;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Multimedia message receiver
 * 
 * @author Jean-Marc AUFFRET
 */
public class MultimediaMessageReceiver extends BroadcastReceiver {
	/**
	 * UI handler
	 */
	private final Handler handler = new Handler();

	@Override
	public void onReceive(Context context, Intent intent) {
		final Context ctx = context;
		String contact = intent.getStringExtra(MultimediaMessageIntent.EXTRA_CONTACT);
		final String msg = context.getString(R.string.label_recv_mm_msg, contact) +
			"\n" + intent.getStringExtra(MultimediaMessageIntent.EXTRA_CONTENT);
		handler.post(new Runnable(){
			public void run(){
		        // Display received message
				Utils.displayLongToast(ctx, msg);
				//MultimediaMessageReceiver.displayNotification(ctx, msg);
			}
		});
    }

    /**
     * Display a notification
     * 
     * @param context Context
     * @param msg Message to display
     */
	public static void displayNotification(Context context, String msg) {
		// Create notification
		Intent intent = new Intent();
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		Notification notif = new Notification(R.drawable.ri_notif_mm_session_icon,
				context.getString(R.string.title_recv_mm_session),
        		System.currentTimeMillis());
        notif.flags = Notification.FLAG_AUTO_CANCEL;
        notif.setLatestEventInfo(context,
        		context.getString(R.string.title_recv_mm_session),
        		msg,
        		contentIntent);
		notif.sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    	notif.defaults |= Notification.DEFAULT_VIBRATE;
        
        // Send notification
		NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(Utils.NOTIF_ID_MM_SESSION, notif);
    }
}
