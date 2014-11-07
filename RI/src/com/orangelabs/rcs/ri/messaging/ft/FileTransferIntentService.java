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

import com.gsma.services.rcs.RcsCommon;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.ft.FileTransferIntent;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.messaging.chat.group.GroupChatDAO;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * File transfer intent service
 * 
 * @author YPLO6403
 * 
 */
public class FileTransferIntentService extends IntentService {

	/**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(FileTransferIntentService.class.getSimpleName());

	/* package private */static final String BUNDLE_FTDAO_ID = "ftdao";
	/* package private */static final String EXTRA_GROUP_FILE = "group_file";

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
		if (!intent.getAction().equalsIgnoreCase(FileTransferIntent.ACTION_NEW_INVITATION)
				&& !intent.getAction().equalsIgnoreCase(FileTransferResumeReceiver.ACTION_FT_RESUME)) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Unknown action " + intent.getAction());
			}
			return;
		}
		// Gets data from the incoming Intent
		String transferId = intent.getStringExtra(FileTransferIntent.EXTRA_TRANSFER_ID);
		if (transferId == null) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Cannot read transfer ID");
			}
			return;
		}
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "onHandleIntent file transfer with ID " + transferId);
		}
		try {
			// Get File Transfer from provider
			FileTransferDAO ftDao = new FileTransferDAO(this, transferId);
			try {
				// Check if a Group CHAT session exists for this file transfer
				new GroupChatDAO(this, ftDao.getChatId());
				intent.putExtra(EXTRA_GROUP_FILE, true);
			} catch (Exception e) {
				// Purposely left blank
			}
			
			// Save FileTransferDAO into intent
			Bundle bundle = new Bundle();
			bundle.putParcelable(BUNDLE_FTDAO_ID, ftDao);
			intent.putExtras(bundle);
			if (intent.getAction().equalsIgnoreCase(FileTransferIntent.ACTION_NEW_INVITATION)) {
				if (LogUtils.isActive) {
					Log.d(LOGTAG, "File Transfer invitation filename=" + ftDao.getFilename() + " size=" + ftDao.getSize());
				}
				// TODO check File Transfer state to know if rejected
				// TODO check validity of direction, etc ...
				addFileTransferInvitationNotification(intent, ftDao);
			} else {
				if (LogUtils.isActive) {
					Log.d(LOGTAG, "onHandleIntent file transfer resume with ID " + transferId);
				}
				Intent intentLocal = new Intent(intent);
				if (ftDao.getDirection() == RcsCommon.Direction.INCOMING) {
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

	/**
	 * Add file transfer notification
	 * 
	 * @param invitation
	 *            Intent invitation
	 * @param ftDao
	 * 				the file transfer data object
	 */
	private void addFileTransferInvitationNotification(Intent invitation, FileTransferDAO ftDao) {
		if (ftDao.getContact() == null) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "addFileTransferInvitationNotification failed: cannot parse contact");
			}
			return;
		}
		// Create notification
		Intent intent = new Intent(invitation);
		intent.setClass(this, ReceiveFileTransfer.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		
		ContactId contact = ftDao.getContact();
		String displayName = RcsDisplayName.getInstance(this).getDisplayName(contact);
		
		String title = getString(R.string.title_recv_file_transfer, displayName);

		// Create notification
		NotificationCompat.Builder notif = new NotificationCompat.Builder(this);
		notif.setContentIntent(contentIntent);
		notif.setSmallIcon(R.drawable.ri_notif_file_transfer_icon);
		notif.setWhen(Calendar.getInstance().getTimeInMillis());
		notif.setAutoCancel(true);
		notif.setOnlyAlertOnce(true);
		notif.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
		notif.setDefaults(Notification.DEFAULT_VIBRATE);
		notif.setContentTitle(title);
		notif.setContentText(ftDao.getFilename());
				
		// Send notification
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(ftDao.getTransferId(), Utils.NOTIF_ID_FT, notif.build());
	}

}
