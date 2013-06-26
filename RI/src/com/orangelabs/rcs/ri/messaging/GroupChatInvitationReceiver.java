package com.orangelabs.rcs.ri.messaging;

import org.gsma.joyn.chat.GroupChatIntent;

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
 * Group chat invitation receiver
 * 
 * @author jexa7410
 */
public class GroupChatInvitationReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		// Display invitation notification
		GroupChatInvitationReceiver.addGroupChatInvitationNotification(context, intent);
    }
	
    /**
     * Add chat notification
     * 
     * @param context Context
     * @param invitation Intent invitation
     */
    public static void addGroupChatInvitationNotification(Context context, Intent invitation) {
    	// Get remote contact
		String contact = invitation.getStringExtra(GroupChatIntent.EXTRA_CONTACT);

		// Get chat ID
		String chatId = invitation.getStringExtra(GroupChatIntent.EXTRA_CHAT_ID);

		// Get chat subject
		String subject = invitation.getStringExtra(GroupChatIntent.EXTRA_SUBJECT);

		// Create notification
		Intent intent = new Intent(invitation);
		intent.setClass(context, GroupChatView.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(chatId);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        String notifTitle = context.getString(R.string.title_recv_group_chat);
        Notification notif = new Notification(R.drawable.ri_notif_chat_icon,
        		notifTitle,
        		System.currentTimeMillis());
        notif.flags = Notification.FLAG_AUTO_CANCEL;
        String msg = contact;
        if (subject != null) {
        	msg = msg + ": " + subject;
        }
        notif.setLatestEventInfo(context, notifTitle, msg, contentIntent);
		notif.sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    	notif.defaults |= Notification.DEFAULT_VIBRATE;
        
        // Send notification
		NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(chatId, Utils.NOTIF_ID_GROUP_CHAT, notif);
    }
    
    /**
     * Remove chat notification
     * 
     * @param context Context
     * @param sessionId Session ID
     */
    public static void removeGroupChatNotification(Context context, String sessionId) {
		NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(sessionId, Utils.NOTIF_ID_GROUP_CHAT);
    }
	
}