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
package com.orangelabs.rcs.ri.messaging.ft;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.util.Log;

import com.gsma.services.rcs.ft.FileTransfer;
import com.gsma.services.rcs.ft.FileTransferIntent;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * 
 * @author YPLO6403
 *
 */
public class FileTransferIntentService extends IntentService {

	/**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(FileTransferIntentService.class.getSimpleName());

	static final String BUNDLE_FTDAO_ID = "ftdao";

	public FileTransferIntentService(String name) {
		super(name);
	}

	public FileTransferIntentService() {
		super("FileTransferIntentService");
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
		if (intent.getAction().equalsIgnoreCase(FileTransferIntent.ACTION_NEW_INVITATION)
				|| intent.getAction().equalsIgnoreCase(FileTransferResumeReceiver.ACTION_FT_RESUME)) {
			//  Gets data from the incoming Intent
			String transferId = intent.getStringExtra(FileTransferIntent.EXTRA_TRANSFER_ID);
			if (transferId != null) {
				if (LogUtils.isActive) {
					Log.d(LOGTAG, "onHandleIntent file transfer with ID " + transferId);
				}
				try {
					// Get File Transfer from provider
					FileTransferDAO ftdao = new FileTransferDAO(this, transferId);
					// Save FileTransferDAO into intent
					Bundle bundle = new Bundle();
					bundle.putSerializable(BUNDLE_FTDAO_ID, ftdao);
					intent.putExtras(bundle);
					if (intent.getAction().equalsIgnoreCase(FileTransferIntent.ACTION_NEW_INVITATION)) {
						if (LogUtils.isActive) {
							Log.d(LOGTAG, "File Transfer invitation filename=" + ftdao.getFilename() + " size=" + ftdao.getSize());
						}
						// TODO check File Transfer state to know if rejected
						// TODO check validity of direction, etc ...
						addFileTransferInvitationNotification(this, intent);
					} else {
						if (LogUtils.isActive) {
							Log.d(LOGTAG, "onHandleIntent file transfer resume with ID " + transferId);
						}
						Intent intentLocal = new Intent(intent);
						if (ftdao.getDirection() == FileTransfer.Direction.INCOMING) {
							intentLocal.setClass(this, ReceiveFileTransfer.class);
						} else {
							intentLocal.setClass(this, InitiateFileTransfer.class);
						}
						intentLocal.addFlags(Intent.FLAG_FROM_BACKGROUND);
						intentLocal.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
						intentLocal.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						intentLocal.setAction(FileTransferResumeReceiver.ACTION_FT_RESUME);
						startActivity(intentLocal);
					}
				} catch (Exception e) {
					if (LogUtils.isActive) {
						Log.e(LOGTAG, "Cannot read FT data from provider", e);
					}
				}
			}
		}
	}

	/**
	 * Add file transfer notification
	 * 
	 * @param context
	 *            Context
	 * @param invitation
	 *            Intent invitation
	 */
	public static void addFileTransferInvitationNotification(Context context, Intent invitation) {
		// Get remote contact
		String contact = invitation.getStringExtra(FileTransferIntent.EXTRA_CONTACT);

		// Get filename
		String filename = invitation.getStringExtra(FileTransferIntent.EXTRA_FILENAME);

		// Create notification
		Intent intent = new Intent(invitation);
		intent.setClass(context, ReceiveFileTransfer.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		String notifTitle = context.getString(R.string.title_recv_file_transfer, contact);
		Notification notif = new Notification(R.drawable.ri_notif_file_transfer_icon, notifTitle, System.currentTimeMillis());
		notif.flags = Notification.FLAG_AUTO_CANCEL;
		notif.setLatestEventInfo(context, notifTitle, filename, contentIntent);
		notif.sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		notif.defaults |= Notification.DEFAULT_VIBRATE;

		// Send notification
		String transferId = invitation.getStringExtra(FileTransferIntent.EXTRA_TRANSFER_ID);
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(transferId, Utils.NOTIF_ID_FT, notif);
	}

	/**
	 * Remove file transfer notification
	 * 
	 * @param context
	 *            Context
	 * @param transferId
	 *            Transfer ID
	 */
	public static void removeFileTransferNotification(Context context, String transferId) {
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(transferId, Utils.NOTIF_ID_FT);
	}

}
