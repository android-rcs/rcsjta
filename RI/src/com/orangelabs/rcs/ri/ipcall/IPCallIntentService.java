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

import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.ipcall.IPCallIntent;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * IP Call sharing intent service
 * 
 * @author YPLO6403
 * 
 */
public class IPCallIntentService extends IntentService {

	/**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(IPCallIntentService.class.getSimpleName());

	/* package private */static final String BUNDLE_IPCALLDAO_ID = "ipcalldao";

	public IPCallIntentService(String name) {
		super(name);
	}

	public IPCallIntentService() {
		super("IPCallIntentService");
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
		if (!intent.getAction().equalsIgnoreCase(IPCallIntent.ACTION_NEW_INVITATION)) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Unknown action " + intent.getAction());
			}
			return;
		}
		// Gets data from the incoming Intent
		String callId = intent.getStringExtra(IPCallIntent.EXTRA_CALL_ID);
		if (callId == null) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Cannot read call ID");
			}
			return;
		}
		try {
			// Get IP Call from provider
			IPCallDAO ipCallDao = new IPCallDAO(this, callId);
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onHandleIntent IP Call DAO " + ipCallDao);
			}
			// Save IPCallDAO into intent
			Bundle bundle = new Bundle();
			bundle.putParcelable(BUNDLE_IPCALLDAO_ID, ipCallDao);
			intent.putExtras(bundle);
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "IP Call invitation " + ipCallDao);
			}
			// TODO check state to know if rejected
			// TODO check validity of direction, etc ...
			// Display invitation notification
			addIPCallInvitationNotification(intent, ipCallDao);
		} catch (Exception e) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Cannot read IP Call data from provider", e);
			}
		}
	}

	/**
	 * Add IP Call notification
	 * 
	 * @param intent
	 *            Intent invitation
	 * @param ipCallDao
	 *            the IP Call data object
	 */
	private void addIPCallInvitationNotification(Intent invitation, IPCallDAO ipCallDao) {
		if (ipCallDao.getContact() == null) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "addIPCallInvitationNotification failed: cannot parse contact");
			}
			return;
		}

		// Create notification
		Intent intent = new Intent(invitation);
		intent.setClass(this, IPCallView.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setAction(ipCallDao.getCallId());
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		ContactId contact = ipCallDao.getContact();
		String displayName = RcsDisplayName.getInstance(this).getDisplayName(contact);
		
		String notifTitle;
		if (ipCallDao.getVideoEncoding() != null) {
			notifTitle = getString(R.string.title_recv_ipcall_video, displayName);
		} else {
			notifTitle = getString(R.string.title_recv_ipcall, displayName);
		}
		
		// Create notification
		NotificationCompat.Builder notif = new NotificationCompat.Builder(this);
		notif.setContentIntent(contentIntent);
		notif.setSmallIcon(R.drawable.ri_notif_ipcall_icon);
		notif.setWhen(Calendar.getInstance().getTimeInMillis());
		notif.setAutoCancel(true);
		notif.setOnlyAlertOnce(true);
		notif.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
		notif.setDefaults(Notification.DEFAULT_VIBRATE);
		notif.setContentTitle(notifTitle);
		notif.setContentText(getString(R.string.label_from_args, displayName));

		// Send notification
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(ipCallDao.getCallId(), Utils.NOTIF_ID_IP_CALL, notif.build());
	}
}
