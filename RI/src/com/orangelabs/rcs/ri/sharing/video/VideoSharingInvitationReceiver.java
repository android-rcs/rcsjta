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

package com.orangelabs.rcs.ri.sharing.video;

import org.gsma.joyn.vsh.VideoSharingIntent;

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
 * Video sharing invitation receiver
 *
 * @author jexa7410
 */
public class VideoSharingInvitationReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		// Display invitation notification
		VideoSharingInvitationReceiver.addVideoSharingInvitationNotification(context, intent);
    }
	
    /**
    * Add video share notification
    *
    * @param context Context
    * @param contact Contact
    * @param sessionId Session ID
    */
	public static void addVideoSharingInvitationNotification(Context context, Intent invitation) {
    	// Get remote contact
		String contact = invitation.getStringExtra(VideoSharingIntent.EXTRA_CONTACT);

    	// Get video format
		String format = invitation.getStringExtra(VideoSharingIntent.EXTRA_ENCODING) + " " +
				invitation.getStringExtra(VideoSharingIntent.EXTRA_FORMAT);

		// Create notification
        Intent intent = new Intent(invitation);
        intent.setClass(context, VisioSharing.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("incoming", true);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        String notifTitle = context.getString(R.string.title_recv_video_sharing, contact);
        Notification notif = new Notification(R.drawable.ri_notif_csh_icon, notifTitle, System.currentTimeMillis());
        notif.flags = Notification.FLAG_AUTO_CANCEL;
        notif.setLatestEventInfo(context, notifTitle, contact, contentIntent);
		notif.sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    	notif.defaults |= Notification.DEFAULT_VIBRATE;

        // Send notification
		String sharingId = invitation.getStringExtra(VideoSharingIntent.EXTRA_SHARING_ID);
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(sharingId, Utils.NOTIF_ID_VIDEO_SHARE, notif);
    }

    /**
     * Remove video share notification
     *
     * @param context Context
     * @param sessionId Session ID
     */
    public static void removeVideoSharingNotification(Context context, String sessionId) {
        NotificationManager notificationManager = (NotificationManager)context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(sessionId, Utils.NOTIF_ID_VIDEO_SHARE);
    }	
}

