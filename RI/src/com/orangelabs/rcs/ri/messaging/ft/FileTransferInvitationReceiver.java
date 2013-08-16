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

import org.gsma.joyn.ft.FileTransferIntent;

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
 * File transfer invitation receiver
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileTransferInvitationReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		// Display invitation notification
		FileTransferInvitationReceiver.addFileTransferInvitationNotification(context, intent);
    }
	
    /**
     * Add file transfer notification
     * 
     * @param context Context
     * @param invitation Intent invitation
     */
    public static void addFileTransferInvitationNotification(Context context, Intent invitation) {
    	// Get remote contact
		String contact = invitation.getStringExtra(FileTransferIntent.EXTRA_CONTACT);
    	
    	// Create notification
		Intent intent = new Intent(invitation);
		intent.setClass(context, ReceiveFileTransfer.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notif = new Notification(R.drawable.ri_notif_file_transfer_icon,
        		context.getString(R.string.title_recv_file_transfer),
        		System.currentTimeMillis());
        notif.flags = Notification.FLAG_AUTO_CANCEL;
        notif.setLatestEventInfo(context,
        		context.getString(R.string.title_recv_file_transfer),
        		context.getString(R.string.label_from) + " " + contact,
        		contentIntent);
		notif.sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    	notif.defaults |= Notification.DEFAULT_VIBRATE;
        
        // Send notification
		String transferId = invitation.getStringExtra(FileTransferIntent.EXTRA_TRANSFER_ID);
		NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(transferId, Utils.NOTIF_ID_FT, notif);
    }
    
	/**
     * Remove file transfer notification
     * 
     * @param context Context
     * @param transferId Transfer ID
     */
    public static void removeFileTransferNotification(Context context, String transferId) {
		NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(transferId, Utils.NOTIF_ID_FT);
    }	
}
