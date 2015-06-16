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

package com.orangelabs.rcs.ri.sharing.image;

import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.sharing.image.ImageSharing.ReasonCode;
import com.gsma.services.rcs.sharing.image.ImageSharing.State;
import com.gsma.services.rcs.sharing.image.ImageSharingListener;
import com.gsma.services.rcs.sharing.image.ImageSharingLog;
import com.gsma.services.rcs.sharing.image.ImageSharingService;

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
 * List image sharings from the content provider
 * 
 * @author Jean-Marc AUFFRET
 * @author YPLO6403
 */
public class ImageSharingList extends FragmentActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    // @formatter:off
    private static final String[] PROJECTION = new String[] {
        ImageSharingLog.BASECOLUMN_ID,
        ImageSharingLog.SHARING_ID, 
        ImageSharingLog.CONTACT, 
        ImageSharingLog.FILENAME,
        ImageSharingLog.FILESIZE, 
        ImageSharingLog.STATE,
        ImageSharingLog.REASON_CODE,
        ImageSharingLog.DIRECTION,
        ImageSharingLog.TIMESTAMP
    };
    // @formatter:on

    private static final String SORT_ORDER = new StringBuilder(ImageSharingLog.TIMESTAMP).append(
            " DESC").toString();

    private ListView mListView;

    private ConnectionManager mCnxManager;

    private ImageSharingService mImageSharingService;

    private ImageSharingListAdapter mAdapter;

    private boolean mImageSharingListenerSet = false;

    private LockAccess mExitOnce = new LockAccess();

    /**
     * List of items for contextual menu
     */
    private static final int MENU_ITEM_DELETE = 0;

    private Handler mHandler = new Handler();

    private static final String LOGTAG = LogUtils.getTag(ImageSharingList.class.getSimpleName());

    /**
     * The loader's unique ID. Loader IDs are specific to the Activity in which they reside.
     */
    private static final int LOADER_ID = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Set layout */
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.image_sharing_list);

        mCnxManager = ConnectionManager.getInstance();
        mImageSharingService = mCnxManager.getImageSharingApi();

        mListView = (ListView) findViewById(android.R.id.list);
        TextView emptyView = (TextView) findViewById(android.R.id.empty);
        mListView.setEmptyView(emptyView);
        registerForContextMenu(mListView);

        mAdapter = new ImageSharingListAdapter(this);
        mListView.setAdapter(mAdapter);
        /*
         * Initialize the Loader with id '1' and callbacks 'mCallbacks'.
         */
        getSupportLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mImageSharingService == null || !mImageSharingListenerSet) {
            return;
        }
        try {
            mImageSharingService.removeEventListener(mImageSharingListener);
        } catch (Exception e) {
            if (LogUtils.isActive) {
                Log.e(LOGTAG, "removeEventListener failed", e);
            }
        }
    }

    /**
     * List adapter
     */
    private class ImageSharingListAdapter extends CursorAdapter {

        private LayoutInflater mInflater;

        /**
         * Constructor
         * 
         * @param context Context
         */
        public ImageSharingListAdapter(Context context) {
            super(context, null, 0);
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            final View view = mInflater.inflate(R.layout.image_sharing_list_item, parent, false);
            view.setTag(new ImageSharingItemCache(view, cursor));
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final ImageSharingItemCache holder = (ImageSharingItemCache) view.getTag();
            String number = cursor.getString(holder.columnNumber);

            String displayName = RcsDisplayName.getInstance(context).getDisplayName(number);
            holder.numberText.setText(getString(R.string.label_contact, displayName));

            String filename = cursor.getString(holder.columnFilename);
            holder.filenameText.setText(getString(R.string.label_filename, filename));

            Long filesize = cursor.getLong(holder.columnFilesize);
            holder.filesizeText.setText(getString(R.string.label_filesize, filesize));

            State state = State.valueOf(cursor.getInt(holder.columnState));
            holder.stateText.setText(getString(R.string.label_session_state,
                    RiApplication.sImageSharingStates[state.toInt()]));

            ReasonCode reason = ReasonCode.valueOf(cursor.getInt(holder.columnReason));
            if (ReasonCode.UNSPECIFIED == reason) {
                holder.reasonText.setVisibility(View.GONE);
            } else {
                holder.reasonText.setVisibility(View.VISIBLE);
                holder.reasonText.setText(getString(R.string.label_session_reason,
                        RiApplication.sImageSharingReasonCodes[reason.toInt()]));
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
     * Image sharing item in cache
     */
    private class ImageSharingItemCache {
        int columnFilename;

        int columnFilesize;

        int columnDirection;

        int columnState;

        int columnReason;

        int columnTimestamp;

        int columnNumber;

        TextView numberText;

        TextView filenameText;

        TextView filesizeText;

        TextView stateText;

        TextView reasonText;

        TextView directionText;

        TextView timestamptext;

        public ImageSharingItemCache(View view, Cursor cursor) {
            columnNumber = cursor.getColumnIndexOrThrow(ImageSharingLog.CONTACT);
            columnFilename = cursor.getColumnIndexOrThrow(ImageSharingLog.FILENAME);
            columnFilesize = cursor.getColumnIndexOrThrow(ImageSharingLog.FILESIZE);
            columnState = cursor.getColumnIndexOrThrow(ImageSharingLog.STATE);
            columnReason = cursor.getColumnIndexOrThrow(ImageSharingLog.REASON_CODE);
            columnDirection = cursor.getColumnIndexOrThrow(ImageSharingLog.DIRECTION);
            columnTimestamp = cursor.getColumnIndexOrThrow(ImageSharingLog.TIMESTAMP);
            numberText = (TextView) view.findViewById(R.id.number);
            filenameText = (TextView) view.findViewById(R.id.filename);
            filesizeText = (TextView) view.findViewById(R.id.filesize);
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
                /* Delete all image sharings */
                if (!mCnxManager.isServiceConnected(RcsServiceName.IMAGE_SHARING)) {
                    Utils.showMessage(this, getString(R.string.label_api_unavailable));
                    break;
                }
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "delete all image sharing sessions");
                }
                try {
                    if (!mImageSharingListenerSet) {
                        mImageSharingService.addEventListener(mImageSharingListener);
                        mImageSharingListenerSet = true;
                    }
                    mImageSharingService.deleteImageSharings();
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
                .getColumnIndexOrThrow(ImageSharingLog.SHARING_ID));
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onContextItemSelected sharing ID=".concat(sharingId));
        }
        switch (item.getItemId()) {
            case MENU_ITEM_DELETE:
                if (!mCnxManager.isServiceConnected(RcsServiceName.IMAGE_SHARING)) {
                    Utils.showMessage(this, getString(R.string.label_api_unavailable));
                    return true;
                }
                /* Delete messages for contact */
                if (LogUtils.isActive) {
                    Log.d(LOGTAG, "Delete image sharing ID=".concat(sharingId));
                }
                try {
                    if (!mImageSharingListenerSet) {
                        mImageSharingService.addEventListener(mImageSharingListener);
                        mImageSharingListenerSet = true;
                    }
                    mImageSharingService.deleteImageSharing(sharingId);
                } catch (Exception e) {
                    Utils.showMessageAndExit(this, getString(R.string.label_delete_sharing_failed),
                            mExitOnce, e);
                }
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private ImageSharingListener mImageSharingListener = new ImageSharingListener() {

        @Override
        public void onStateChanged(ContactId contact, String sharingId, State state,
                ReasonCode reasonCode) {
        }

        @Override
        public void onProgressUpdate(ContactId contact, String sharingId, long currentSize,
                long totalSize) {
        }

        @Override
        public void onDeleted(ContactId contact, Set<String> sharingIds) {
            if (LogUtils.isActive) {
                Log.d(LOGTAG,
                        "onDeleted contact=" + contact + " for sharing IDs="
                                + Arrays.toString(sharingIds.toArray()));
            }
            mHandler.post(new Runnable() {
                public void run() {
                    Utils.displayLongToast(ImageSharingList.this,
                            getString(R.string.label_delete_sharing_success));
                }
            });
        }
    };

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        /* Create a new CursorLoader with the following query parameters. */
        return new CursorLoader(this, ImageSharingLog.CONTENT_URI, PROJECTION, null, null,
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
