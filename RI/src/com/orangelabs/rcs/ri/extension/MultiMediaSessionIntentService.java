/******************************************
 * Software Name : VCOM
 * Copyright © 2011 France Telecom S.A.
 ******************************************/

package com.orangelabs.rcs.ri.extension;

import java.util.Calendar;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.extension.MultimediaMessagingSession;
import com.gsma.services.rcs.extension.MultimediaMessagingSessionIntent;
import com.gsma.services.rcs.extension.MultimediaStreamingSession;
import com.gsma.services.rcs.extension.MultimediaStreamingSessionIntent;
import com.orangelabs.rcs.ri.ApiConnectionManager;
import com.orangelabs.rcs.ri.ApiConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.extension.messaging.MessagingSessionView;
import com.orangelabs.rcs.ri.extension.streaming.StreamingSessionView;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Process the MultiMedia Session invitation<br>
 * Purpose is to retrieve the contactId from the service to build the notification.
 * 
 * @author YPLO6403
 * 
 */
public class MultiMediaSessionIntentService extends IntentService {

	public MultiMediaSessionIntentService(String name) {
		super(name);
	}

	public MultiMediaSessionIntentService() {
		super("MultiMediaSessionIntentService");
	}

	/**
	 * MM session API
	 */
	private String sessionId;
	private boolean actionMultimediaMessagingSession = false;

	/**
	 * API connection manager
	 */
	private ApiConnectionManager connectionManager;

	/**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(MultiMediaSessionIntentService.class.getSimpleName());

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

		if (intent.getAction().equalsIgnoreCase(MultimediaMessagingSessionIntent.ACTION_NEW_INVITATION)) {
			actionMultimediaMessagingSession = true;
		} else {
			if (!intent.getAction().equalsIgnoreCase(MultimediaStreamingSessionIntent.ACTION_NEW_INVITATION)) {
				if (LogUtils.isActive) {
					Log.d(LOGTAG, "onCreate invalid action=" + intent.getAction());
				}
				return;
			}
		}
		// Register to API connection manager
		connectionManager = ApiConnectionManager.getInstance(this);
		if (connectionManager == null || !connectionManager.isServiceConnected(RcsServiceName.MULTIMEDIA)) {
			return;
		}
		// Get invitation info
		sessionId = intent.getStringExtra(MultimediaMessagingSessionIntent.EXTRA_SESSION_ID);
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "onHandleIntent sessionId=" + sessionId);
		}
		initiateSession();
	}

	private void initiateSession() {
		try {
			if (actionMultimediaMessagingSession) {
				MultimediaMessagingSession mms = connectionManager.getMultimediaSessionApi().getMessagingSession(sessionId);
				if (mms != null) {
					ContactId contact = mms.getRemoteContact();
					addSessionInvitationNotification(sessionId, contact, true);
				} else {
					if (LogUtils.isActive) {
						Log.w(LOGTAG, "Cannot get messaging session for ID " + sessionId);
					}
				}
			} else {
				MultimediaStreamingSession mss = connectionManager.getMultimediaSessionApi().getStreamingSession(sessionId);
				if (mss != null) {
					ContactId contact = mss.getRemoteContact();
					addSessionInvitationNotification(sessionId, contact, false);
				} else {
					if (LogUtils.isActive) {
						Log.w(LOGTAG, "Cannot get streaming session for ID " + sessionId);
					}
				}
			}
		} catch (RcsServiceException e) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "Cannot get MM API", e);
			}
		}
	}

	/**
	 * Add session invitation notification
	 * 
	 * @param sessionId
	 * @param contact
	 * @param mms True is messaging session
	 */
	private void addSessionInvitationNotification(String sessionId, ContactId contact, boolean mms) {
		// Get remote contact and session
		if (contact == null) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "addSessionInvitationNotification failed");
			}
			return;
		}
		// Create notification
		Intent invitation = new Intent();
		String title;
		if (mms) {
			invitation.setClass(this, MessagingSessionView.class);
			title = getString(R.string.title_recv_messaging_session);
		} else {
			invitation.setClass(this, StreamingSessionView.class);
			title = getString(R.string.title_recv_streaming_session);
		}
		invitation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		invitation.setAction(sessionId);
		invitation.putExtra(MultimediaMessagingSessionIntent.EXTRA_SESSION_ID, sessionId);

		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, invitation, PendingIntent.FLAG_UPDATE_CURRENT);

		String from = RcsDisplayName.getInstance(this).getDisplayName(contact);

		// Create notification
		NotificationCompat.Builder notif = new NotificationCompat.Builder(this);
		notif.setContentIntent(contentIntent);
		notif.setSmallIcon(R.drawable.ri_notif_mm_session_icon);
		notif.setWhen(Calendar.getInstance().getTimeInMillis());
		notif.setAutoCancel(true);
		notif.setOnlyAlertOnce(true);
		notif.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
		notif.setDefaults(Notification.DEFAULT_VIBRATE);
		notif.setContentTitle(title);
		notif.setContentText(getString(R.string.label_from_args, from));

		// Send notification
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(sessionId, Utils.NOTIF_ID_MM_SESSION, notif.build());
	}

}