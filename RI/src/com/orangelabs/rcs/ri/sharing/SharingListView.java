/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.orangelabs.rcs.ri.sharing;

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
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

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
import com.orangelabs.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.api.connection.utils.ExceptionUtil;
import com.orangelabs.rcs.api.connection.utils.RcsActivity;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.sharing.geoloc.GeolocSharingLogView;
import com.orangelabs.rcs.ri.sharing.image.ImageSharingLogView;
import com.orangelabs.rcs.ri.sharing.video.VideoSharingLogView;
import com.orangelabs.rcs.ri.utils.ContactListAdapter;
import com.orangelabs.rcs.ri.utils.ContactUtil;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

public class SharingListView extends RcsActivity {

    private static final String LOGTAG = LogUtils.getTag(SharingListView.class.getName());

    private static final int MAX_LENGTH_DESCRIPTION = 20;
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
    private ResourceCursorAdapter mResourceCursorAdapter;
    private Spinner mSpinner;
    private VideoSharingListener mVideoSharingListener;
    private boolean mVideoSharingListenerSet;
    private VideoSharingService mVideoSharingService;

    private class SharingLogAdapter extends ResourceCursorAdapter {
        private Drawable mDrawableIncoming;
        private Drawable mDrawableIncomingFailed;
        private Drawable mDrawableOutgoing;
        private Drawable mDrawableOutgoingFailed;
        private Drawable mDrawableRichCall;

        public SharingLogAdapter(Context context) {
            super(context, R.layout.history_log_list, null);
            Resources resources = context.getResources();
            mDrawableIncomingFailed = resources.getDrawable(R.drawable.ri_incoming_call_failed);
            mDrawableOutgoingFailed = resources.getDrawable(R.drawable.ri_outgoing_call_failed);
            mDrawableIncoming = resources.getDrawable(R.drawable.ri_incoming_call);
            mDrawableOutgoing = resources.getDrawable(R.drawable.ri_outgoing_call);
            mDrawableRichCall = resources.getDrawable(R.drawable.ri_historylog_csh);
        }

        public void bindView(View view, Context context, Cursor cursor) {
            int providerId = cursor.getInt(cursor.getColumnIndexOrThrow(HistoryLog.PROVIDER_ID));

            TextView sharingTypeView = (TextView) view.findViewById(R.id.conversation_type);
            TextView sharingLabelView = (TextView) view.findViewById(R.id.conversation_label);
            TextView descriptionView = (TextView) view.findViewById(R.id.description);
            TextView dateView = (TextView) view.findViewById(R.id.date);

            ImageView eventDirectionIconView = (ImageView) view.findViewById(R.id.call_type_icon);
            ImageView eventIconView = (ImageView) view.findViewById(R.id.call_icon);

            // Set contact number
            String phone = cursor.getString(cursor.getColumnIndexOrThrow(HistoryLog.CONTACT));
            sharingLabelView.setText(phone);

            // Set the date/time field by mixing relative and absolute times
            long date = cursor.getLong(cursor.getColumnIndex(HistoryLog.TIMESTAMP));
            dateView.setText(DateUtils.getRelativeTimeSpanString(date, System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE));

            // Set the status text and destination icon
            int status = cursor.getInt(cursor.getColumnIndexOrThrow(HistoryLog.STATUS));
            Direction dir = Direction.valueOf(cursor.getInt(cursor
                    .getColumnIndexOrThrow(HistoryLog.DIRECTION)));
            switch (dir) {
                case INCOMING:
                    if (status != State.FAILED.toInt()
                            && status != VideoSharing.State.FAILED.toInt()
                            && status != GeolocSharing.State.FAILED.toInt()) {
                        eventDirectionIconView.setImageDrawable(mDrawableIncoming);
                    } else {
                        eventDirectionIconView.setImageDrawable(mDrawableIncomingFailed);
                    }
                    break;

                case OUTGOING:
                    if (status != State.FAILED.toInt()
                            && status != VideoSharing.State.FAILED.toInt()
                            && status != GeolocSharing.State.FAILED.toInt()) {
                        eventDirectionIconView.setImageDrawable(mDrawableOutgoing);
                    } else {
                        eventDirectionIconView.setImageDrawable(mDrawableOutgoingFailed);
                    }
                    break;
            }
            eventIconView.setImageDrawable(mDrawableRichCall);
            switch (providerId) {
                case ImageSharingLog.HISTORYLOG_MEMBER_ID:
                    sharingTypeView.setText(R.string.label_sharing_log_menu_ish);
                    String filename = cursor.getString(cursor
                            .getColumnIndexOrThrow(HistoryLog.FILENAME));
                    descriptionView.setText(TextUtils.isEmpty(filename) ? "" : truncateString(
                            filename, MAX_LENGTH_DESCRIPTION));
                    break;

                case VideoSharingLog.HISTORYLOG_MEMBER_ID:
                    sharingTypeView.setText(R.string.label_sharing_log_menu_vsh);
                    long duration = cursor.getLong(cursor
                            .getColumnIndexOrThrow(HistoryLog.DURATION));
                    descriptionView.setText(getString(R.string.value_log_duration,
                            DateUtils.formatElapsedTime(duration / 1000L)));
                    break;

                case GeolocSharingLog.HISTORYLOG_MEMBER_ID:
                    sharingTypeView.setText(R.string.label_sharing_log_menu_gsh);
                    String content = cursor.getString(cursor
                            .getColumnIndexOrThrow(HistoryLog.CONTENT));
                    descriptionView.setText(TextUtils.isEmpty(content) ? "" : truncateString(
                            content, MAX_LENGTH_DESCRIPTION));
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
        mResourceCursorAdapter = new SharingLogAdapter(this);
        ListView listView = (ListView) findViewById(android.R.id.list);
        listView.setEmptyView(findViewById(android.R.id.empty));
        listView.setAdapter(mResourceCursorAdapter);
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
        switch (item.getItemId()) {
            case R.id.menu_filter:
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
                break;

            case R.id.menu_delete :
                if (!isServiceConnected(RcsServiceName.IMAGE_SHARING, RcsServiceName.VIDEO_SHARING,
                        RcsServiceName.GEOLOC_SHARING)) {
                    showMessage(R.string.label_service_not_available);
                    break;
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
                break;
        }
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.menu_log_ish_item, menu);
        menu.findItem(R.id.menu_sharing_display).setVisible(false);
        // Get the list item position
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        Cursor cursor = (Cursor) mResourceCursorAdapter.getItem(info.position);
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
        Cursor cursor = (Cursor) mResourceCursorAdapter.getItem(info.position);
        int providerId = cursor.getInt(cursor.getColumnIndexOrThrow(HistoryLog.PROVIDER_ID));
        String sharingId = cursor.getString(cursor.getColumnIndexOrThrow(HistoryLog.ID));
        if (LogUtils.isActive) {
            Log.d(LOGTAG, "onContextItemSelected sharing ID=".concat(sharingId));
        }
        try {
            switch (item.getItemId()) {
                case R.id.menu_sharing_delete:
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
                case R.id.menu_sharing_display:
                    Utils.showPicture(this, Uri.parse(cursor.getString(cursor
                            .getColumnIndexOrThrow(HistoryLog.CONTENT))));
                    return true;

                default:
                    return super.onContextItemSelected(item);
            }
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
                Cursor cursor = (Cursor) mResourceCursorAdapter.getItem(pos);
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
                Uri uri = createHistoryUri(selectedProviderIds);
                cursor = getContentResolver().query(uri, null, null, null, SORT_BY);
            } else {
                Uri uri = createHistoryUri(selectedProviderIds);
                cursor = getContentResolver().query(uri, null, WHERE_CLAUSE_WITH_CONTACT,
                        new String[] {
                            contact
                        }, SORT_BY);
            }
        }
        mResourceCursorAdapter.changeCursor(cursor);
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
    private Uri createHistoryUri(List<Integer> providerIds) {
        HistoryUriBuilder uriBuilder = new HistoryUriBuilder(HistoryLog.CONTENT_URI);
        for (Integer providerId : providerIds) {
            uriBuilder.appendProvider(providerId);
        }
        return uriBuilder.build();
    }
}
