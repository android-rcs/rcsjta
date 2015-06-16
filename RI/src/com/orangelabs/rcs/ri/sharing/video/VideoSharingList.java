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

import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.video.VideoSharing.ReasonCode;
import com.gsma.services.rcs.sharing.video.VideoSharing.State;
import com.gsma.services.rcs.sharing.video.VideoSharingListener;
import com.gsma.services.rcs.sharing.video.VideoSharingLog;
import com.gsma.services.rcs.sharing.video.VideoSharingService;

import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;
import com.orangelabs.rcs.ri.utils.Utils;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;

/**
 * List video sharing from the content provider
 * 
 * @author Jean-Marc AUFFRET
 * @author YPLO6403
 */
public class VideoSharingList extends FragmentActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    // @formatter:off
    private static final String[] PROJECTION = new String[] {
        VideoSharingLog.BASECOLUMN_ID,
        VideoSharingLog.SHARING_ID,
        VideoSharingLog.CONTACT,
        VideoSharingLog.DURATION,
        VideoSharingLog.STATE,
        VideoSharingLog.REASON_CODE,
        VideoSharingLog.DIRECTION,
        VideoSharingLog.TIMESTAMP
    };
    // @formatter:on

    private static final String SORT_ORDER = new StringBuilder(VideoSharingLog.TIMESTAMP).append(
            " DESC").toString();

    private ListView mListView;

    private ConnectionManager mCnxManager;

    private VideoSharingService mVideoSharingService;

    private boolean mVideoSharingListenerSet = false;

    private VideoSharingListAdapter mAdapter;

    private LockAccess mExitOnce = new LockAccess();

    /**
     * List of items for contextual menu
     */
    private static final int MENU_ITEM_DELETE = 0;

    private Handler mHandler = new Handler();

    private static final String LOGTAG = LogUtils.getTag(VideoSharingList.class.getSimpleName());

    /**
     * The loader's unique ID. Loader IDs are specific to the Activity in which they reside.
     */
    private static final int LOADER_ID = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Set layout */
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.video_sharing_list);

        mCnxManager = ConnectionManager.getInstance();
        mVideoSharingService = mCnxManager.getVideoSharingApi();

        mListView = (ListView) findViewById(android.R.id.list);
        TextView emptyView = (TextView) findViewById(android.R.id.empty);
        mListView.setEmptyView(emptyView);
        registerForContextMenu(mListView);

        mAdapter = new VideoSharingListAdapter(this);
        mListView.setAdapter(mAdapter);
        /*
         * Initialize the Loader with id '1' and callbacks 'mCallbacks'.
         */
        getSupportLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mVideoSharingService == null || !mVideoSharingListenerSet) {
            return;
        }
        try {
            mVideoSharingService.removeEventListener(mVideoSharingListener);
        } catch (Exception e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "removeEventListener failed", e);
            }
        }
    }

    /**
     * List adapter
     */
    private class VideoSharingListAdapter extends CursorAdapter {

        private LayoutInflater mInflater;

        /**
         * Constructor
         * 
         * @param context Context
         */
        public VideoSharingListAdapter(Context context) {
            super(context, null, 0);
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = mInflater.inflate(R.layout.video_sharing_list_item, parent, false);
            view.setTag(new VideoSharingItemCache(view, cursor));
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final VideoSharingItemCache holder = (VideoSharingItemCache) view.getTag();
            String number = cursor.getString(holder.columnNumber);
            String displayName = RcsDisplayName.getInstance(context).getDisplayName(number);
            holder.numberText.setText(getString(R.string.label_contact, displayName));

            Long duration = cursor.getLong(holder.columnDuration);
            holder.durationText.setText(getString(R.string.label_video_duration,
                    DateUtils.formatElapsedTime(duration / 1000)));

            State state = State.valueOf(cursor.getInt(holder.columnState));
            holder.stateText.setText(getString(R.string.label_session_state,
                    RiApplication.sVideoSharingStates[state.toInt()]));

            ReasonCode reason = ReasonCode.valueOf(cursor.getInt(holder.columnReason));
            if (ReasonCode.UNSPECIFIED == reason) {
                holder.reasonText.setVisibility(View.GONE);
            } else {
                holder.reasonText.setVisibility(View.VISIBLE);
                holder.reasonText.setText(getString(R.string.label_session_reason,
                        RiApplication.sVideoReasonCodes[reason.toInt()]));
            }

            Direction direction = Direction.valueOf(cursor.getInt(holder.columnDirection));
            holder.directionText.setText(getString(R.string.label_direction,
                    RiApplication.getDirection(direction)));

            Long timestamp = cursor.getLong(holder.columnTimestamp);
            holder.timestamptext.setText(getString(R.string.label_session_date,
                    decodeDate(timestamp)));
        }
    }

    /**
     * Video sharing item in cache
     */
    private class VideoSharingItemCache {
        int columnDuration;

        int columnDirection;

        int columnState;

        int columnReason;

        int columnTimestamp;

        int columnNumber;

        TextView numberText;

        TextView durationText;

        TextView stateText;

        TextView reasonText;

        TextView directionText;

        TextView timestamptext;

        public VideoSharingItemCache(View view, Cursor cursor) {
            columnNumber = cursor.getColumnIndexOrThrow(VideoSharingLog.CONTACT);
            columnDuration = cursor.getColumnIndexOrThrow(VideoSharingLog.DURATION);
            columnState = cursor.getColumnIndexOrThrow(VideoSharingLog.STATE);
            columnReason = cursor.getColumnIndexOrThrow(VideoSharingLog.REASON_CODE);
            columnDirection = cursor.getColumnIndexOrThrow(VideoSharingLog.DIRECTION);
            columnTimestamp = cursor.getColumnIndexOrThrow(VideoSharingLog.TIMESTAMP);
            numberText = (TextView) view.findViewById(R.id.number);
            durationText = (TextView) view.findViewById(R.id.duration);
            stateText = (TextView) view.findViewById(R.id.state);
            reasonText = (TextView) view.findViewById(R.id.reason);
            directionText = (TextView) view.findViewById(R.id.direction);
            timestamptext = (TextView) view.findViewById(R.id.date);
        }
    }

    /**
     * Decode date
     * 
     * @param date Date
     * @return String
     */
    private String decodeDate(long date) {
        return DateFormat.getInstance().format(new Date(date));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(getApplicationContext());
        inflater.inflate(R.menu.menu_log, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_clear_log:
                /* Delete all video sharings */
                if (!mCnxManager.isServiceConnected(RcsServiceName.VIDEO_SHARING)) {
                    Utils.showMessage(this, getString(R.string.label_api_unavailable));
                    break;
                }
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "delete all video sharing sessions");
                }
                try {
                    if (!mVideoSharingListenerSet) {
                        mVideoSharingService.addEventListener(mVideoSharingListener);
                        mVideoSharingListenerSet = true;
                    }
                    mVideoSharingService.deleteVideoSharings();
                } catch (Exception e) {
                    Utils.showMessageAndExit(this, getString(R.string.label_delete_sharing_failed),
                            mExitOnce, e);
                }
                break;
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (!mCnxManager.isServiceConnected(RcsServiceName.IMAGE_SHARING)) {
            Utils.showMessage(this, getString(R.string.label_api_unavailable));
            return;
        }
        menu.add(0, MENU_ITEM_DELETE, MENU_ITEM_DELETE, R.string.menu_sharing_delete);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        /* Get selected item */
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        Cursor cursor = (Cursor) (mListView.getAdapter()).getItem(info.position);
        String sharingId = cursor.getString(cursor
                .getColumnIndexOrThrow(VideoSharingLog.SHARING_ID));
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onContextItemSelected sharing ID=".concat(sharingId));
        }
        switch (item.getItemId()) {
            case MENU_ITEM_DELETE:
                if (!mCnxManager.isServiceConnected(RcsServiceName.VIDEO_SHARING)) {
                    Utils.showMessage(this, getString(R.string.label_api_unavailable));
                    return true;
                }
                /* Delete messages for contact */
                try {
                    if (!mVideoSharingListenerSet) {
                        mVideoSharingService.addEventListener(mVideoSharingListener);
                        mVideoSharingListenerSet = true;
                    }
                    if (LogUtils.isActive) {
                        Log.d(LOGTAG, "Delete video sharing ID=".concat(sharingId));
                    }
                    mVideoSharingService.deleteVideoSharing(sharingId);
                } catch (Exception e) {
                    Utils.showMessageAndExit(this, getString(R.string.label_delete_sharing_failed),
                            mExitOnce, e);
                }
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private VideoSharingListener mVideoSharingListener = new VideoSharingListener() {

        @Override
        public void onDeleted(ContactId contact, Set<String> sharingIds) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG,
                        "onDeleted contact=" + contact + " for sharing IDs="
                                + Arrays.toString(sharingIds.toArray()));
            }
            mHandler.post(new Runnable() {
                public void run() {
                    Utils.displayLongToast(VideoSharingList.this,
                            getString(R.string.label_delete_sharing_success));
                }
            });
        }

        @Override
        public void onStateChanged(ContactId arg0, String arg1, State arg2, ReasonCode arg3) {
        }

    };

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        /* Create a new CursorLoader with the following query parameters. */
        return new CursorLoader(this, VideoSharingLog.CONTENT_URI, PROJECTION, null, null,
                SORT_ORDER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        /* A switch-case is useful when dealing with multiple Loaders/IDs */
        switch (loader.getId()) {
            case LOADER_ID:
                /*
                 * The asynchronous load is complete and the data is now available for use. Only now
                 * can we associate the queried Cursor with the CursorAdapter.
                 */
                mAdapter.swapCursor(cursor);
                break;
        }
        /* The listview now displays the queried data. */
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        /*
         * For whatever reason, the Loader's data is now unavailable. Remove any references to the
         * old data by replacing it with a null Cursor.
         */
        mAdapter.swapCursor(null);
    }
}
