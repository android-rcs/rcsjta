/******************************************
 * Software Name : VCOM
 * Copyright 
 ******************************************/

package com.orangelabs.rcs.ri.extension;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.extension.MultimediaMessagingSession;
import com.gsma.services.rcs.extension.MultimediaMessagingSessionIntent;
import com.gsma.services.rcs.extension.MultimediaStreamingSession;
import com.gsma.services.rcs.extension.MultimediaStreamingSessionIntent;

import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.extension.messaging.MessagingSessionView;
import com.orangelabs.rcs.ri.extension.streaming.StreamingSessionView;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;
import com.orangelabs.rcs.ri.utils.Utils;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * Process the MultiMedia Session invitation<br>
 * Purpose is to retrieve the contactId from the service to build the notification.
 * 
 * @author YPLO6403
 */
public class MultiMediaSessionIntentService extends IntentService {

    private String mSessionId;

    private boolean mMultimediaMessagingSession = false;

    private ConnectionManager mCnxManager;

    private static final String LOGTAG = LogUtils.getTag(MultiMediaSessionIntentService.class
            .getSimpleName());

    /**
     * Constructor
     */
    public MultiMediaSessionIntentService() {
        super("MultiMediaSessionIntentService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        /*
         * We want this service to stop running if forced stop so return not sticky.
         */
        return START_NOT_STICKY;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action;
        if ((action = intent.getAction()) == null) {
            return;
        }

        if (MultimediaMessagingSessionIntent.ACTION_NEW_INVITATION.equals(action)) {
            mMultimediaMessagingSession = true;
        } else {
            if (!MultimediaStreamingSessionIntent.ACTION_NEW_INVITATION.equals(action)) {
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "Unknown action=".concat(action));
                }
                return;
            }
        }
        /* Since there is no provider associated to multimedia sessions, we must connect to the API */
        mCnxManager = ConnectionManager.getInstance();
        if (!mCnxManager.isServiceConnected(RcsServiceName.MULTIMEDIA)) {
            return;
        }
        // Get invitation info
        mSessionId = intent.getStringExtra(MultimediaMessagingSessionIntent.EXTRA_SESSION_ID);
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onHandleIntent sessionId=".concat(mSessionId));
        }
        initiateSession(intent);
    }

    private void initiateSession(Intent intent) {
        try {
            if (mMultimediaMessagingSession) {
                MultimediaMessagingSession mms = mCnxManager.getMultimediaSessionApi()
                        .getMessagingSession(mSessionId);
                if (mms != null) {
                    addSessionInvitationNotification(intent, mms.getRemoteContact());
                } else {
                    if (LogUtils.isActive) {
                        Log.w(LOGTAG, "Cannot get messaging session for ID ".concat(mSessionId));
                    }
                }
            } else {
                MultimediaStreamingSession mss = mCnxManager.getMultimediaSessionApi()
                        .getStreamingSession(mSessionId);
                if (mss != null) {
                    addSessionInvitationNotification(intent, mss.getRemoteContact());
                } else {
                    if (LogUtils.isActive) {
                        Log.w(LOGTAG, "Cannot get streaming session for ID ".concat(mSessionId));
                    }
                }
            }
        } catch (RcsServiceException e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "Cannot get MM API", e);
            }
        }
    }

    private void addSessionInvitationNotification(Intent intent, ContactId contact) {
        /* Create pending intent */
        Intent invitation = new Intent(intent);
        String title;
        if (mMultimediaMessagingSession) {
            invitation.setClass(this, MessagingSessionView.class);
            title = getString(R.string.title_recv_messaging_session);
        } else {
            invitation.setClass(this, StreamingSessionView.class);
            title = getString(R.string.title_recv_streaming_session);
        }
        invitation.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        /*
         * If the PendingIntent has the same operation, action, data, categories, components, and
         * flags it will be replaced. Invitation should be notified individually so we use a random
         * generator to provide a unique request code and reuse it for the notification.
         */
        int uniqueId = Utils.getUniqueIdForPendingIntent();
        PendingIntent contentIntent = PendingIntent.getActivity(this, uniqueId, invitation,
                PendingIntent.FLAG_ONE_SHOT);

        String displayName = RcsDisplayName.getInstance(this).getDisplayName(contact);

        /* Create notification */
        NotificationCompat.Builder notif = new NotificationCompat.Builder(this);
        notif.setContentIntent(contentIntent);
        notif.setSmallIcon(R.drawable.ri_notif_mm_session_icon);
        notif.setWhen(System.currentTimeMillis());
        notif.setAutoCancel(true);
        notif.setOnlyAlertOnce(true);
        notif.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        notif.setDefaults(Notification.DEFAULT_VIBRATE);
        notif.setContentTitle(title);
        notif.setContentText(getString(R.string.label_from_args, displayName));

        /* Send notification */
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(uniqueId, notif.build());
    }

}
