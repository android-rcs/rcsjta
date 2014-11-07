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
import android.util.Log;

import com.gsma.services.rcs.vsh.VideoSharingIntent;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Video sharing intent service
 * 
 * @author YPLO6403
 * 
 */
public class VideoSharingIntentService extends IntentService {

	/**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(VideoSharingIntentService.class.getSimpleName());

	static final String BUNDLE_VSHDAO_ID = "vshdao";

	public VideoSharingIntentService(String name) {
		super(name);
	}

	public VideoSharingIntentService() {
		super("VideoSharingIntentService");
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
		// Check action from incoming intent
		if (!intent.getAction().equalsIgnoreCase(VideoSharingIntent.ACTION_NEW_INVITATION)) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Unknown action " + intent.getAction());
			}
			return;
		}
		// Gets data from the incoming Intent
		String sharingId = intent.getStringExtra(VideoSharingIntent.EXTRA_SHARING_ID);
		if (sharingId != null) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onHandleIntent video sharing with ID " + sharingId);
			}
			try {
				// Get Video Sharing from provider
				VideoSharingDAO vshDao = new VideoSharingDAO(this, sharingId);
				// Save VideoSharingDAO into intent
				Bundle bundle = new Bundle();
				bundle.putParcelable(BUNDLE_VSHDAO_ID, vshDao);
				intent.putExtras(bundle);
				if (LogUtils.isActive) {
					Log.d(LOGTAG, "Video sharing invitation " + vshDao);
				}
				// TODO check VSH state to know if rejected
				// TODO check validity of direction, etc ...
				// Display invitation notification
				addVideoSharingInvitationNotification(intent, vshDao);
			} catch (Exception e) {
				if (LogUtils.isActive) {
					Log.e(LOGTAG, "Cannot read VSH data from provider", e);
				}
			}
		}
	}

	/**
	 * Add video share notification
	 * 
	 * @param invitation
	 *            Intent invitation
	 * @param vshDao
	 *            the video sharing data object
	 */
	public void addVideoSharingInvitationNotification(Intent invitation, VideoSharingDAO vshDao) {
		if (vshDao.getContact() == null) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "VideoSharingInvitationReceiver failed: cannot parse contact");
			}
			return;
		}
		// Create notification
		Intent intent = new Intent(invitation);
		intent.setClass(this, ReceiveVideoSharing.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		String displayName = RcsDisplayName.getInstance(this).getDisplayName(vshDao.getContact());
		
		String notifTitle = getString(R.string.title_recv_video_sharing, displayName);

		// Create notification
		NotificationCompat.Builder notif = new NotificationCompat.Builder(this);
		notif.setContentIntent(contentIntent);
		notif.setSmallIcon(R.drawable.ri_notif_csh_icon);
		notif.setWhen(Calendar.getInstance().getTimeInMillis());
		notif.setAutoCancel(true);
		notif.setOnlyAlertOnce(true);
		notif.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
		notif.setDefaults(Notification.DEFAULT_VIBRATE);
		notif.setContentTitle(notifTitle);
		notif.setContentText( vshDao.getContact().toString());
				
		// Send notification
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(vshDao.getSharingId(), Utils.NOTIF_ID_VIDEO_SHARE, notif.build());
	}
}
