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

package com.orangelabs.rcs.ri.sharing.geoloc;

import java.util.Calendar;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.gsh.GeolocSharingIntent;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Geoloc sharing invitation receiver
 * 
 * @author vfml3370
 */
public class GeolocSharingInvitationReceiver extends BroadcastReceiver {
	
	/**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(GeolocSharingInvitationReceiver.class.getSimpleName());
	
	@Override
	public void onReceive(Context context, Intent intent) {
		// Display invitation notification
		GeolocSharingInvitationReceiver.addGeolocSharingInvitationNotification(context, intent);
    }
	
    /**
     * Add geoloc share notification
     * 
     * @param context Context
     * @param invitation Intent invitation
     */
	public static void addGeolocSharingInvitationNotification(Context context, Intent invitation) {
		// Get remote contact
		// TODO CR025 implement provider for geolocation sharing
		ContactId contact = invitation.getParcelableExtra("TODO");
		if (contact == null) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "GeolocSharingInvitationReceiver failed: cannot parse contact");
			}
			return;
		}

		// Create notification
		Intent intent = new Intent(invitation);
		intent.setClass(context, ReceiveGeolocSharing.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);		
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        String title = context.getString(R.string.title_recv_geoloc_sharing, contact.toString());
        
    	// Create notification
		NotificationCompat.Builder notif = new NotificationCompat.Builder(context);
		notif.setContentIntent(contentIntent);
		notif.setSmallIcon(R.drawable.ri_notif_gsh_icon);
		notif.setWhen(Calendar.getInstance().getTimeInMillis());
		notif.setAutoCancel(true);
		notif.setOnlyAlertOnce(true);
		notif.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
		notif.setDefaults(Notification.DEFAULT_VIBRATE);
		notif.setContentTitle(title);
		notif.setContentText(title);
    			
        // Send notification
		String sharingId = invitation.getStringExtra(GeolocSharingIntent.EXTRA_SHARING_ID);
		NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(sharingId, Utils.NOTIF_ID_GEOLOC_SHARE, notif.build());
	}
	
    /**
     * Remove geoloc share notification
     * 
     * @param context Context
     * @param sharingId SharingId ID
     */
	public static void removeGeolocSharingNotification(Context context, String sharingId) {
		NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(sharingId, Utils.NOTIF_ID_GEOLOC_SHARE);
	}	
}
