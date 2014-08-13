/******************************************
 * Software Name : VCOM
 * Copyright © 2011 France Telecom S.A.
 ******************************************/

package com.orangelabs.rcs.ri.extension;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import com.gsma.services.rcs.JoynServiceException;
import com.gsma.services.rcs.JoynServiceListener;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.extension.MultimediaMessagingSession;
import com.gsma.services.rcs.extension.MultimediaMessagingSessionIntent;
import com.gsma.services.rcs.extension.MultimediaSessionService;
import com.gsma.services.rcs.extension.MultimediaStreamingSession;
import com.gsma.services.rcs.extension.MultimediaStreamingSessionIntent;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.extension.messaging.MessagingSessionView;
import com.orangelabs.rcs.ri.extension.streaming.StreamingSessionView;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Receive MultiMedia Session invitation<br>
 * Purpose is to retrieve the contactId from the service to build the notification.
 * 
 * @author YPLO6403
 * 
 */
public class ReceiveMultiMediaSession extends Activity implements JoynServiceListener {

	/**
	 * MM session API
	 */
	private MultimediaSessionService sessionApi;
	private boolean serviceConnected = false;
	private String sessionId;
	private boolean actionMultimediaMessagingSession = false;

	/**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(ReceiveMultiMediaSession.class.getSimpleName());

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Get invitation info
		sessionId = getIntent().getStringExtra(MultimediaMessagingSessionIntent.EXTRA_SESSION_ID);
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "onCreate sessionId=" + sessionId);
		}
		if (getIntent() == null || getIntent().getAction() == null) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onCreate invalid intent=" + getIntent());
			}
			finish();
			return;
		}
		if (getIntent().getAction().equalsIgnoreCase(MultimediaMessagingSessionIntent.ACTION_NEW_INVITATION)) {
			actionMultimediaMessagingSession = true;
		} else {
			if (!getIntent().getAction().equalsIgnoreCase(MultimediaStreamingSessionIntent.ACTION_NEW_INVITATION)) {
				if (LogUtils.isActive) {
					Log.d(LOGTAG, "onCreate invalid action=" + getIntent().getAction());
				}
				finish();
				return;
			}
		}
		// Instantiate API
		sessionApi = new MultimediaSessionService(getApplicationContext(), this);
		// Connect API
		sessionApi.connect();
	}

	@Override
	public void onDestroy() {
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "onDestroy");
		}
		super.onDestroy();
		if (serviceConnected) {
			// Disconnect API
			sessionApi.disconnect();
		}
	}

	@Override
	public void onServiceDisconnected(int error) {
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "onServiceDisconnected");
		}
		serviceConnected = false;
		quitSession(R.string.label_api_disabled);
	}

	@Override
	public void onServiceConnected() {
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "onServiceConnected");
		}
		serviceConnected = true;
		try {
			if (actionMultimediaMessagingSession) {
				MultimediaMessagingSession mms = sessionApi.getMessagingSession(sessionId);
				if (mms != null) {
					ContactId contact = mms.getRemoteContact();
					addSessionInvitationNotification(this, sessionId, contact, true);
				} else {
					quitSession(R.string.label_session_not_found);
				}
			} else {
				MultimediaStreamingSession mss = sessionApi.getStreamingSession(sessionId);
				if (mss != null) {
					ContactId contact = mss.getRemoteContact();
					addSessionInvitationNotification(this, sessionId, contact, false);
				} else {
					quitSession(R.string.label_session_not_found);
				}
			}
		} catch (JoynServiceException e) {
			e.printStackTrace();
			quitSession(R.string.label_api_failed);
		}
	}

	private void quitSession(int msgId) {
		quitSession(getString(msgId));
	}

	/**
	 * Quit the session
	 */
	private void quitSession(String msg) {
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "quitSession: " + msg);
		}
		/*
		 * Show a pop-up or toast then exit
		 */
		Utils.displayLongToast(ReceiveMultiMediaSession.this, msg);
		// Exit activity
		finish();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onKeyDown: BACK");
			}
			// Quit the session
			quitSession(null);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Add session invitation notification
	 * 
	 * @param context
	 *            Context
	 * @param invitation
	 *            Intent invitation
	 */
	public static void addSessionInvitationNotification(Context context, String sessionId, ContactId contact, boolean mms) {
		// Get remote contact and session
		if (contact == null) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "addSessionInvitationNotification failed");
			}
			return;
		}
		// Create notification
		Intent intent = new Intent();
		String notifTitle;
		if (mms) {
			intent.setClass(context, MessagingSessionView.class);
			notifTitle = context.getString(R.string.title_recv_messaging_session);
		} else {
			intent.setClass(context, StreamingSessionView.class);
			notifTitle = context.getString(R.string.title_recv_streaming_session);
		}
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.setAction(sessionId);
		intent.putExtra(MultimediaMessagingSessionIntent.EXTRA_SESSION_ID, sessionId);

		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		Notification notif = new Notification(R.drawable.ri_notif_mm_session_icon, notifTitle, System.currentTimeMillis());
		notif.flags = Notification.FLAG_AUTO_CANCEL;
		notif.setLatestEventInfo(context, notifTitle, context.getString(R.string.label_session_from, contact.toString()),
				contentIntent);
		notif.sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		notif.defaults |= Notification.DEFAULT_VIBRATE;

		// Send notification
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(sessionId, Utils.NOTIF_ID_MM_SESSION, notif);
	}

}
