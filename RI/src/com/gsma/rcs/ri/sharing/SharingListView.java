/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010-2016 Orange.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.ri.sharing;

import com.gsma.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.gsma.rcs.api.connection.utils.ExceptionUtil;
import com.gsma.rcs.api.connection.utils.RcsFragmentActivity;
import com.gsma.rcs.ri.R;
import com.gsma.rcs.ri.sharing.geoloc.GeolocSharingLogView;
import com.gsma.rcs.ri.sharing.image.ImageSharingLogView;
import com.gsma.rcs.ri.sharing.video.VideoSharingLogView;
import com.gsma.rcs.ri.utils.ContactListAdapter;
import com.gsma.rcs.ri.utils.ContactUtil;
import com.gsma.rcs.ri.utils.LogUtils;
import com.gsma.rcs.ri.utils.RcsContactUtil;
import com.gsma.rcs.ri.utils.Utils;
import com.gsma.services.rcs.RcsGenericException;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.history.HistoryLog;
import com.gsma.services.rcs.history.HistoryUriBuilder;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharing;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharingListener;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharingLog;
import com.gsma.services.rcs.sharing.geoloc.GeolocSharingService;
import com.gsma.services.rcs.sharing.image.ImageSharing.ReasonCode;
import com.gsma.services.rcs.sharing.image.ImageSharing.State;
import com.gsma.services.rcs.sharing.image.ImageSharingListener;
import com.gsma.services.rcs.sharing.image.ImageSharingLog;
import com.gsma.services.rcs.sharing.image.ImageSharingService;
import com.gsma.services.rcs.sharing.video.VideoSharing;
import com.gsma.services.rcs.sharing.video.VideoSharingListener;
import com.gsma.services.rcs.sharing.video.VideoSharingLog;
import com.gsma.services.rcs.sharing.video.VideoSharingService;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
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
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 * A class to view the sharing logs
 */
public class SharingListView extends RcsFragmentActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * The loader's unique ID. Loader IDs are specific to the Activity in which they reside.
     */
    private static final int LOADER_ID = 1;

    // @formatter:off
    private static final String[] PROJECTION = {
            HistoryLog.BASECOLUMN_ID,
            HistoryLog.ID,
            HistoryLog.PROVIDER_ID,
            HistoryLog.CONTACT,
            HistoryLog.TIMESTAMP,
            HistoryLog.DIRECTION,
            HistoryLog.STATUS,
            HistoryLog.FILENAME,
            HistoryLog.DURATION,
            HistoryLog.CONTENT,
            HistoryLog.MIME_TYPE,
            HistoryLog.TRANSFERRED,
            HistoryLog.FILESIZE
    };
    // @formatter:on

    private static final String LOGTAG = LogUtils.getTag(SharingListView.class.getName());

    private static final int MAX_LENGTH_DESCRIPTION = 30;
    private static final String SORT_BY = "timestamp DESC";
    private static final String WHERE_CLAUSE_WITH_CONTACT = "contact=?";
    /**
     * Associate the providers name menu with providerIds defined in HistoryLog
     */
    private static final TreeMap<Integer, String> sProviders = new TreeMap<>();

    private boolean[] mCheckedProviders;
    private AlertDialog mFilterAlertDialog;
    private List<String> mFilterMenuItems;
    private GeolocSharingListener mGeolocSharingListener;
    private boolean mGeolocSharingListenerSet;
    private GeolocSharingService mGeolocSharingService;
    private final Handler mHandler = new Handler();
    private ImageSharingListener mImageSharingListener;
    private boolean mImageSharingListenerSet;
    private ImageSharingService mImageSharingService;
    private List<Integer> mProviderIds;
    private SharingLogAdapter mAdapter;
    private Spinner mSpinner;
    private VideoSharingListener mVideoSharingListener;
    private boolean mVideoSharingListenerSet;
    private VideoSharingService mVideoSharingService;

    private class ViewHolder {

        private final int mColumnContactIdx;
        private final int mColumnTimestampIdx;
        private final int mColumnStatusIdx;
        private final int mColumnDirectionIdx;
        private final TextView mTypeView;
        private final TextView mContactView;
        private final TextView mDescriptionView;
        private final TextView mDateView;
        private final ImageView mDirectionIconImageView;
        private final int mColumnProviderIdIdx;
        private final int mColumnFilenameIdx;
        private final int mColumnDurationIdx;
        private final int mColumnContentIdx;

        public ViewHolder(View view, Cursor cursor) {
            mColumnProviderIdIdx = cursor.getColumnIndexOrThrow(HistoryLog.PROVIDER_ID);
            mColumnContactIdx = cursor.getColumnIndexOrThrow(HistoryLog.CONTACT);
            mColumnTimestampIdx = cursor.getColumnIndexOrThrow(HistoryLog.TIMESTAMP);
            mColumnDirectionIdx = cursor.getColumnIndexOrThrow(HistoryLog.DIRECTION);
            mColumnStatusIdx = cursor.getColumnIndexOrThrow(HistoryLog.STATUS);
            mColumnFilenameIdx = cursor.getColumnIndexOrThrow(HistoryLog.FILENAME);
            mColumnDurationIdx = cursor.getColumnIndexOrThrow(HistoryLog.DURATION);
            mColumnContentIdx = cursor.getColumnIndexOrThrow(HistoryLog.CONTENT);

            mTypeView = (TextView) view.findViewById(R.id.conversation_type);
            mContactView = (TextView) view.findViewById(R.id.contact_label);
            mDescriptionView = (TextView) view.findViewById(R.id.description);
            mDateView = (TextView) view.findViewById(R.id.date);
            mDirectionIconImageView = (ImageView) view.findViewById(R.id.call_type_icon);
        }

        public int getColumnProviderIdIdx() {
            return mColumnProviderIdIdx;
        }

        public int getColumnContactIdx() {
            return mColumnContactIdx;
        }

        public int getColumnTimestampIdx() {
            return mColumnTimestampIdx;
        }

        public int getColumnStatusIdx() {
            return mColumnStatusIdx;
        }

        public int getColumnDirectionIdx() {
            return mColumnDirectionIdx;
        }

        public TextView getTypeView() {
            return mTypeView;
        }

        public TextView getContactView() {
            return mContactView;
        }

        public TextView getDescriptionView() {
            return mDescriptionView;
        }

        public TextView getDateView() {
            return mDateView;
        }

        public ImageView getDirectionIconImageView() {
            return mDirectionIconImageView;
        }

        public int getColumnFilenameIdx() {
            return mColumnFilenameIdx;
        }

        public int getColumnDurationIdx() {
            return mColumnDurationIdx;
        }

        public int getColumnContentIdx() {
            return mColumnContentIdx;
        }
    }

    private class SharingLogAdapter extends CursorAdapter {
        private final LayoutInflater mInflater;
        private final RcsContactUtil mRcsContactUtil;
        private Drawable mDrawableIncoming;
        private Drawable mDrawableIncomingFailed;
        private Drawable mDrawableOutgoing;
        private Drawable mDrawableOutgoingFailed;

        public SharingLogAdapter(Activity activity) {
            super(activity, null, 0);
            mRcsContactUtil = RcsContactUtil.getInstance(activity);
            mInflater = LayoutInflater.from(SharingListView.this);
            Resources resources = activity.getResources();
            mDrawableIncomingFailed = resources.getDrawable(R.drawable.ri_incoming_call_failed);
            mDrawableOutgoingFailed = resources.getDrawable(R.drawable.ri_outgoing_call_failed);
            mDrawableIncoming = resources.getDrawable(R.drawable.ri_incoming_call);
            mDrawableOutgoing = resources.getDrawable(R.drawable.ri_outgoing_call);
        }

        @Override
        public int getItemViewType(int position) {
            return 1;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = mInflater.inflate(R.layout.sharing_log_list, parent, false);
            view.setTag(new ViewHolder(view, cursor));
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder holder = (ViewHolder) view.getTag();

            // Set contact number
            String phone = cursor.getString(holder.getColumnContactIdx());
            ContactId contact = ContactUtil.formatContact(phone);
            holder.getContactView().setText(mRcsContactUtil.getDisplayName(contact));

            // Set the date/time field by mixing relative and absolute times
            long date = cursor.getLong(holder.getColumnTimestampIdx());
            holder.getDateView().setText(
                    DateUtils.getRelativeTimeSpanString(date, System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE));

            // Set the status text and destination icon
            int status = cursor.getInt(holder.getColumnStatusIdx());
            Direction dir = Direction.valueOf(cursor.getInt(holder.getColumnDirectionIdx()));
            switch (dir) {
                case INCOMING:
                    if (status != State.FAILED.toInt()
                            && status != VideoSharing.State.FAILED.toInt()
                            && status != GeolocSharing.State.FAILED.toInt()) {
                        holder.getDirectionIconImageView().setImageDrawable(mDrawableIncoming);
                    } else {
                        holder.getDirectionIconImageView()
                                .setImageDrawable(mDrawableIncomingFailed);
                    }
                    break;

                case OUTGOING:
                    if (status != State.FAILED.toInt()
                            && status != VideoSharing.State.FAILED.toInt()
                            && status != GeolocSharing.State.FAILED.toInt()) {
                        holder.getDirectionIconImageView().setImageDrawable(mDrawableOutgoing);
                    } else {
                        holder.getDirectionIconImageView()
                                .setImageDrawable(mDrawableOutgoingFailed);
                    }
                    break;
            }
            int providerId = cursor.getInt(holder.getColumnProviderIdIdx());
            switch (providerId) {
                case ImageSharingLog.HISTORYLOG_MEMBER_ID:
                    holder.getTypeView().setText(R.string.label_sharing_log_menu_ish);
                    String filename = cursor.getString(holder.getColumnFilenameIdx());
                    holder.getDescriptionView().setText(
                            TextUtils.isEmpty(filename) ? "" : truncateString(filename,
                                    MAX_LENGTH_DESCRIPTION));
                    break;

                case VideoSharingLog.HISTORYLOG_MEMBER_ID:
                    holder.getTypeView().setText(R.string.label_sharing_log_menu_vsh);
                    long duration = cursor.getLong(holder.getColumnDurationIdx());
                    holder.getDescriptionView().setText(
                            getString(R.string.value_log_duration,
                                    DateUtils.formatElapsedTime(duration / 1000L)));
                    break;

                case GeolocSharingLog.HISTORYLOG_MEMBER_ID:
                    holder.getTypeView().setText(R.string.label_sharing_log_menu_gsh);
                    String content = cursor.getString(holder.getColumnContentIdx());
                    holder.getDescriptionView().setText(
                            TextUtils.isEmpty(content) ? "" : truncateString(content,
                                    MAX_LENGTH_DESCRIPTION));
                default:
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.history_log_sharing);
        initialize();
    }

    @Override
    protected void onResume() {
        super.onResume();
        queryHistoryLogAndRefreshView();
    }

    private void initialize() {
        final Runnable updateUi = new Runnable() {
            @Override
            public void run() {
                queryHistoryLogAndRefreshView();
            }
        };
        mImageSharingService = getImageSharingApi();
        mImageSharingListener = new ImageSharingListener() {
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
                Log.d(SharingListView.LOGTAG, "onDeleted contact=" + contact + " for sharing IDs="
                        + Arrays.toString(sharingIds.toArray()));
                mHandler.post(updateUi);
            }
        };
        mVideoSharingService = getVideoSharingApi();
        mVideoSharingListener = new VideoSharingListener() {
            @Override
            public void onStateChanged(ContactId contact, String sharingId,
                    VideoSharing.State state, VideoSharing.ReasonCode reasonCode) {
            }

            @Override
            public void onDeleted(ContactId contact, Set<String> sharingIds) {
                Log.d(SharingListView.LOGTAG, "onDeleted contact=" + contact + " for sharing IDs="
                        + Arrays.toString(sharingIds.toArray()));
                mHandler.post(updateUi);
            }
        };
        mGeolocSharingService = getGeolocSharingApi();
        mGeolocSharingListener = new GeolocSharingListener() {
            @Override
            public void onStateChanged(ContactId contact, String sharingId,
                    GeolocSharing.State state, GeolocSharing.ReasonCode reasonCode) {
            }

            @Override
            public void onProgressUpdate(ContactId contact, String sharingId, long currentSize,
                    long totalSize) {
            }

            @Override
            public void onDeleted(ContactId contact, Set<String> sharingIds) {
                Log.d(SharingListView.LOGTAG, "onDeleted contact=" + contact + " for sharing IDs="
                        + Arrays.toString(sharingIds.toArray()));
                mHandler.post(updateUi);
            }
        };
        mAdapter = new SharingLogAdapter(this);
        ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setEmptyView(findViewById(android.R.id.empty));
        listView.setAdapter(mAdapter);
        registerForContextMenu(listView);

        mSpinner = (Spinner) findViewById(R.id.contact);
        mSpinner.setAdapter(ContactListAdapter.createContactListAdapter(this,
                getString(R.string.label_sharing_log_contact_spinner_default_value)));
        mSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                /* Call when an item is selected so also at the start of the activity to initialize */
                queryHistoryLogAndRefreshView();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        listView.setOnItemClickListener(getOnItemClickListener());

        sProviders.put(ImageSharingLog.HISTORYLOG_MEMBER_ID,
                getString(R.string.label_sharing_log_menu_ish));
        sProviders.put(VideoSharingLog.HISTORYLOG_MEMBER_ID,
                getString(R.string.label_sharing_log_menu_vsh));
        sProviders.put(GeolocSharingLog.HISTORYLOG_MEMBER_ID,
                getString(R.string.label_sharing_log_menu_gsh));
        setProviders(sProviders);

        setCursorLoader(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (mImageSharingService != null && mImageSharingListenerSet) {
                mImageSharingService.removeEventListener(mImageSharingListener);
            }
            if (mVideoSharingService != null && mVideoSharingListenerSet) {
                mVideoSharingService.removeEventListener(mVideoSharingListener);
            }
            if (mGeolocSharingService != null && mGeolocSharingListenerSet) {
                mGeolocSharingService.removeEventListener(mGeolocSharingListener);
            }
        } catch (RcsServiceException e) {
            Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        new MenuInflater(getApplicationContext()).inflate(R.menu.menu_historylog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_filter) {
            Builder builder = new Builder(this);
            builder.setTitle(R.string.title_sharing_log_dialog_filter_logs);
            builder.setMultiChoiceItems(
                    mFilterMenuItems.toArray(new CharSequence[mFilterMenuItems.size()]),
                    mCheckedProviders, new DialogInterface.OnMultiChoiceClickListener() {
                        public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                            mCheckedProviders[which] = isChecked;
                        }
                    });
            builder.setPositiveButton(R.string.label_ok, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    mFilterAlertDialog.dismiss();
                    queryHistoryLogAndRefreshView();
                }
            });
            builder.setNegativeButton(R.string.label_cancel, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mFilterAlertDialog.dismiss();
                }
            });
            mFilterAlertDialog = builder.show();
            registerDialog(mFilterAlertDialog);
        } else if (itemId == R.id.menu_delete) {
            if (!isServiceConnected(RcsServiceName.IMAGE_SHARING, RcsServiceName.VIDEO_SHARING,
                    RcsServiceName.GEOLOC_SHARING)) {
                showMessage(R.string.label_service_not_available);
                return true;
            }
            Log.d(LOGTAG, "delete all image sharing sessions");
            try {
                if (!mImageSharingListenerSet) {
                    mImageSharingService.addEventListener(mImageSharingListener);
                    mImageSharingListenerSet = true;
                }
                mImageSharingService.deleteImageSharings();
                if (!mVideoSharingListenerSet) {
                    mVideoSharingService.addEventListener(mVideoSharingListener);
                    mVideoSharingListenerSet = true;
                }
                mVideoSharingService.deleteVideoSharings();
                if (!mGeolocSharingListenerSet) {
                    mGeolocSharingService.addEventListener(mGeolocSharingListener);
                    mGeolocSharingListenerSet = true;
                }
                mGeolocSharingService.deleteGeolocSharings();
            } catch (RcsServiceException e) {
                showExceptionThenExit(e);

            }
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.menu_log_sharing_item, menu);
        menu.findItem(R.id.menu_sharing_display).setVisible(false);
        // Get the list item position
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        Cursor cursor = (Cursor) mAdapter.getItem(info.position);
        Direction dir = Direction.valueOf(cursor.getInt(cursor
                .getColumnIndexOrThrow(HistoryLog.DIRECTION)));
        String mimeType = cursor.getString(cursor.getColumnIndexOrThrow(HistoryLog.MIME_TYPE));
        if (mimeType != null && Utils.isImageType(mimeType)) {
            if (Direction.INCOMING == dir) {
                Long transferred = cursor.getLong(cursor
                        .getColumnIndexOrThrow(HistoryLog.TRANSFERRED));
                Long fileSize = cursor.getLong(cursor.getColumnIndexOrThrow(HistoryLog.FILESIZE));
                if (fileSize.equals(transferred)) {
                    menu.findItem(R.id.menu_sharing_display).setVisible(true);
                    return;
                }
                return;
            }
            menu.findItem(R.id.menu_sharing_display).setVisible(true);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        Cursor cursor = (Cursor) mAdapter.getItem(info.position);
        int providerId = cursor.getInt(cursor.getColumnIndexOrThrow(HistoryLog.PROVIDER_ID));
        String sharingId = cursor.getString(cursor.getColumnIndexOrThrow(HistoryLog.ID));
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onContextItemSelected sharing ID=".concat(sharingId));
        }
        try {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_sharing_delete) {
                Log.d(LOGTAG, "Delete sharing ID=".concat(sharingId));
                switch (providerId) {
                    case ImageSharingLog.HISTORYLOG_MEMBER_ID:
                        if (!mImageSharingListenerSet) {
                            mImageSharingService.addEventListener(mImageSharingListener);
                            mImageSharingListenerSet = true;
                        }
                        mImageSharingService.deleteImageSharing(sharingId);
                        return true;

                    case VideoSharingLog.HISTORYLOG_MEMBER_ID:
                        if (!mVideoSharingListenerSet) {
                            mVideoSharingService.addEventListener(mVideoSharingListener);
                            mVideoSharingListenerSet = true;
                        }
                        mVideoSharingService.deleteVideoSharing(sharingId);
                        return true;
                    case GeolocSharingLog.HISTORYLOG_MEMBER_ID:
                        if (!mGeolocSharingListenerSet) {
                            mGeolocSharingService.addEventListener(mGeolocSharingListener);
                            mGeolocSharingListenerSet = true;
                        }
                        mGeolocSharingService.deleteGeolocSharing(sharingId);
                        return true;
                    default:
                        return true;
                }
            } else if (itemId == R.id.menu_sharing_display) {
                Utils.showPicture(this, Uri.parse(cursor.getString(cursor
                        .getColumnIndexOrThrow(HistoryLog.CONTENT))));
                return true;
            }
            return super.onContextItemSelected(item);
        } catch (RcsServiceNotAvailableException e) {
            showMessage(R.string.label_service_not_available);
            return true;

        } catch (RcsGenericException e2) {
            showExceptionThenExit(e2);
            return true;
        }
    }

    private OnItemClickListener getOnItemClickListener() {
        return new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
                Cursor cursor = (Cursor) mAdapter.getItem(pos);
                int providerId = cursor
                        .getInt(cursor.getColumnIndexOrThrow(HistoryLog.PROVIDER_ID));
                String sharingId = cursor.getString(cursor.getColumnIndexOrThrow(HistoryLog.ID));
                switch (providerId) {
                    case ImageSharingLog.HISTORYLOG_MEMBER_ID:
                        ImageSharingLogView.startActivity(SharingListView.this, sharingId);
                        break;
                    case VideoSharingLog.HISTORYLOG_MEMBER_ID:
                        VideoSharingLogView.startActivity(SharingListView.this, sharingId);
                        break;
                    case GeolocSharingLog.HISTORYLOG_MEMBER_ID:
                        GeolocSharingLogView.startActivity(SharingListView.this, sharingId);
                        break;
                    default:
                }
            }
        };
    }

    private String getSelectedContact() {
        MatrixCursor cursor = (MatrixCursor) mSpinner.getSelectedItem();
        String contact = cursor.getString(1);
        if (getString(R.string.label_history_log_contact_spinner_default_value).equals(contact)) {
            return contact;
        }
        return ContactUtil.formatContact(contact).toString();
    }

    /**
     * A method called to query the history log and refresh view
     */
    private void queryHistoryLogAndRefreshView() {
        Cursor cursor = null;
        List<Integer> selectedProviderIds = getSelectedProviderIds();
        String contact = getSelectedContact();
        if (!selectedProviderIds.isEmpty()) {
            if (getString(R.string.label_sharing_log_contact_spinner_default_value).equals(contact)) {
                /*
                 * No contact is selected
                 */
                Uri uri = createSharingLogUri(selectedProviderIds);
                cursor = getContentResolver().query(uri, PROJECTION, null, null, SORT_BY);
            } else {
                Uri uri = createSharingLogUri(selectedProviderIds);
                cursor = getContentResolver().query(uri, PROJECTION, WHERE_CLAUSE_WITH_CONTACT,
                        new String[] {
                            contact
                        }, SORT_BY);
            }
        }
        mAdapter.changeCursor(cursor);
    }

    /**
     * Truncate a string
     *
     * @param in string to truncate
     * @param maxLength maximum length
     * @return truncated string
     */
    private String truncateString(String in, int maxLength) {
        if (in.length() > maxLength) {
            in = in.substring(0, maxLength).concat("...");
        }
        return "\"" + in + "\"";
    }

    /**
     * Sets the providers
     *
     * @param providers ordered map of providers IDs associated with their name
     */
    private void setProviders(TreeMap<Integer, String> providers) {
        mProviderIds = new ArrayList<>();
        mFilterMenuItems = new ArrayList<>();
        for (Entry<Integer, String> entry : providers.entrySet()) {
            mFilterMenuItems.add(entry.getValue());
            mProviderIds.add(entry.getKey());
        }
        /* Upon setting, all providers are checked True */
        mCheckedProviders = new boolean[providers.size()];
        for (int i = 0; i < mCheckedProviders.length; i++) {
            mCheckedProviders[i] = true;
        }
    }

    /**
     * Gets the list of selected providers IDs
     *
     * @return the list of selected providers IDs
     */
    private List<Integer> getSelectedProviderIds() {
        List<Integer> providers = new ArrayList<>();
        for (int i = 0; i < mCheckedProviders.length; i++) {
            if (mCheckedProviders[i]) {
                providers.add(mProviderIds.get(i));
            }
        }
        return providers;
    }

    /**
     * Create History URI from list of provider IDs
     *
     * @param providerIds list of provider IDs
     * @return Uri
     */
    private Uri createSharingLogUri(List<Integer> providerIds) {
        HistoryUriBuilder uriBuilder = new HistoryUriBuilder(HistoryLog.CONTENT_URI);
        for (Integer providerId : providerIds) {
            uriBuilder.appendProvider(providerId);
        }
        return uriBuilder.build();
    }

    private void setCursorLoader(boolean firstLoad) {
        if (firstLoad) {
            // Initialize the Loader with id '1' and callbacks 'mCallbacks'.
            getSupportLoaderManager().initLoader(LOADER_ID, null, this);
        } else {
            // We switched from one contact to another: reload history since.
            getSupportLoaderManager().restartLoader(LOADER_ID, null, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onCreateLoader " + id);
        }
        List<Integer> selectedProviderIds = getSelectedProviderIds();
        String contact = getSelectedContact();
        if (!selectedProviderIds.isEmpty()) {
            Uri uri = createSharingLogUri(selectedProviderIds);
            // Create a new CursorLoader with the following query parameters.
            if (getString(R.string.label_sharing_log_contact_spinner_default_value).equals(contact)) {
                // No contact is selected
                return new CursorLoader(this, uri, PROJECTION, null, null, SORT_BY);
            }
            return new CursorLoader(this, uri, PROJECTION, WHERE_CLAUSE_WITH_CONTACT, new String[] {
                contact
            }, SORT_BY);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onLoadFinished " + loader.getId());
        }
        // A switch-case is useful when dealing with multiple Loaders/IDs
        switch (loader.getId()) {
            case LOADER_ID:
                // The asynchronous load is complete and the data
                // is now available for use. Only now can we associate
                // the queried Cursor with the CursorAdapter.
                mAdapter.swapCursor(cursor);
                break;
        }
        // The listview now displays the queried data.
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onLoaderReset " + loader.getId());
        }
        // For whatever reason, the Loader's data is now unavailable.
        // Remove any references to the old data by replacing it with a null
        // Cursor.
        mAdapter.swapCursor(null);
    }

}
