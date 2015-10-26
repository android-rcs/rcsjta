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

package com.orangelabs.rcs.ri.messaging.filetransfer;

import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransfer.ReasonCode;
import com.gsma.services.rcs.filetransfer.FileTransfer.State;
import com.gsma.services.rcs.filetransfer.FileTransferLog;
import com.gsma.services.rcs.filetransfer.FileTransferService;

import com.orangelabs.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.api.connection.utils.RcsFragmentActivity;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.RiApplication;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsContactUtil;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.Bundle;
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
import java.util.Date;

/**
 * List file transfers from the content provider
 * 
 * @author Jean-Marc AUFFRET
 * @author Philippe LEMORDANT
 */
public class FileTransferList extends RcsFragmentActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    // @formatter:off
    private static final String[] PROJECTION = new String[] {
            FileTransferLog.BASECOLUMN_ID,
            FileTransferLog.CHAT_ID,
            FileTransferLog.FT_ID,
            FileTransferLog.CONTACT,
            FileTransferLog.FILENAME,
            FileTransferLog.FILESIZE,
            FileTransferLog.STATE,
            FileTransferLog.REASON_CODE,
            FileTransferLog.DIRECTION,
            FileTransferLog.TIMESTAMP
    };
    // @formatter:on

    private static final String SORT_ORDER = FileTransferLog.TIMESTAMP + " DESC";

    private FtCursorAdapter mAdapter;

    private FileTransferService mFileTransferService;

    private static final String LOGTAG = LogUtils.getTag(FileTransferList.class.getSimpleName());

    /**
     * List of items for contextual menu
     */
    private final static int MENU_ITEM_DELETE = 0;
    private final static int MENU_ITEM_RESEND = 1;

    /**
     * The loader's unique ID. Loader IDs are specific to the Activity in which they reside.
     */
    private static final int LOADER_ID = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.filetransfer_list);

        mFileTransferService = getFileTransferApi();

        /* Set list adapter */
        ListView listView = (ListView) findViewById(android.R.id.list);
        TextView emptyView = (TextView) findViewById(android.R.id.empty);
        listView.setEmptyView(emptyView);
        registerForContextMenu(listView);

        mAdapter = new FtCursorAdapter(this);
        listView.setAdapter(mAdapter);
        /*
         * Initialize the Loader with id '1' and callbacks 'mCallbacks'.
         */
        getSupportLoaderManager().initLoader(LOADER_ID, null, this);
    }

    private class FtCursorAdapter extends CursorAdapter {

        private LayoutInflater mInflater;

        /**
         * Constructor
         * 
         * @param context Context
         */
        public FtCursorAdapter(Context context) {
            super(context, null, 0);
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            final View view = mInflater.inflate(R.layout.filetransfer_list_item, parent, false);
            view.setTag(new FileTransferItemViewHolder(view, cursor));
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final FileTransferItemViewHolder holder = (FileTransferItemViewHolder) view.getTag();
            String number = cursor.getString(holder.columnNumber);
            String displayName = RcsContactUtil.getInstance(context).getDisplayName(number);
            holder.numberText.setText(getString(R.string.label_contact, displayName));

            String filename = cursor.getString(holder.columnFilename);
            holder.filenameText.setText(getString(R.string.label_filename, filename));

            Long filesize = cursor.getLong(holder.columnFilesize);
            holder.filesizeText.setText(getString(R.string.label_filesize, filesize));

            State state = State.valueOf(cursor.getInt(holder.columnState));
            holder.stateText.setText(getString(R.string.label_session_state,
                    RiApplication.sFileTransferStates[state.toInt()]));

            ReasonCode reason = ReasonCode.valueOf(cursor.getInt(holder.columnReason));
            if (ReasonCode.UNSPECIFIED == reason) {
                holder.reasonText.setVisibility(View.GONE);
            } else {
                holder.reasonText.setVisibility(View.VISIBLE);
                holder.reasonText.setText(getString(R.string.label_session_reason,
                        RiApplication.sFileTransferReasonCodes[reason.toInt()]));
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
     * A ViewHolder class keeps references to children views to avoid unnecessary calls to
     * findViewById() or getColumnIndex() on each row.
     */
    private class FileTransferItemViewHolder {
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

        public FileTransferItemViewHolder(View base, Cursor cursor) {
            columnNumber = cursor.getColumnIndexOrThrow(FileTransferLog.CONTACT);
            columnFilename = cursor.getColumnIndexOrThrow(FileTransferLog.FILENAME);
            columnFilesize = cursor.getColumnIndexOrThrow(FileTransferLog.FILESIZE);
            columnState = cursor.getColumnIndexOrThrow(FileTransferLog.STATE);
            columnDirection = cursor.getColumnIndexOrThrow(FileTransferLog.DIRECTION);
            columnTimestamp = cursor.getColumnIndexOrThrow(FileTransferLog.TIMESTAMP);
            columnReason = cursor.getColumnIndexOrThrow(FileTransferLog.REASON_CODE);
            numberText = (TextView) base.findViewById(R.id.number);
            filenameText = (TextView) base.findViewById(R.id.filename);
            filesizeText = (TextView) base.findViewById(R.id.filesize);
            stateText = (TextView) base.findViewById(R.id.state);
            directionText = (TextView) base.findViewById(R.id.direction);
            timestamptext = (TextView) base.findViewById(R.id.date);
            reasonText = (TextView) base.findViewById(R.id.reason);
        }

    }

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
                /* Delete all file transfers */
                if (!isServiceConnected(RcsServiceName.FILE_TRANSFER)) {
                    showMessage(R.string.label_service_not_available);
                    break;
                }
                try {
                    if (LogUtils.isActive) {
                        Log.d(LOGTAG, "onOptionsItemSelected: delete OneToOne File Transfers");
                    }
                    mFileTransferService.deleteOneToOneFileTransfers();

                    if (LogUtils.isActive) {
                        Log.d(LOGTAG, "onOptionsItemSelected: delete Group File Transfers");
                    }
                    mFileTransferService.deleteGroupFileTransfers();
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
        /* Check file transfer API is connected */
        if (!isServiceConnected(RcsServiceName.FILE_TRANSFER)) {
            showMessage(R.string.label_service_not_available);
            return;
        }
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        Cursor cursor = (Cursor) mAdapter.getItem(info.position);
        menu.add(0, MENU_ITEM_DELETE, 0, R.string.menu_delete_message);

        /* Check if message can be resent */
        String transferId = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.FT_ID));
        try {
            FileTransfer transfer = mFileTransferService.getFileTransfer(transferId);
            if (transfer != null && transfer.isAllowedToResendTransfer()) {
                menu.add(0, MENU_ITEM_RESEND, 1, R.string.menu_resend_message);
            }
        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        Cursor cursor = (Cursor) (mAdapter.getItem(info.position));
        String transferId = cursor.getString(cursor.getColumnIndexOrThrow(FileTransferLog.FT_ID));
        try {
            switch (item.getItemId()) {
                case MENU_ITEM_RESEND:
                    if (LogUtils.isActive) {
                        Log.d(LOGTAG, "onContextItemSelected resend ftId=".concat(transferId));
                    }
                    if (!isServiceConnected(RcsServiceName.FILE_TRANSFER)) {
                        showMessage(R.string.label_service_not_available);
                        return true;
                    }
                    FileTransfer transfer = mFileTransferService.getFileTransfer(transferId);
                    if (transfer != null) {
                        transfer.resendTransfer();
                    }
                    return true;

                case MENU_ITEM_DELETE:
                    if (LogUtils.isActive) {
                        Log.d(LOGTAG, "onContextItemSelected delete ftId=".concat(transferId));
                    }
                    if (!isServiceConnected(RcsServiceName.FILE_TRANSFER)) {
                        showMessage(R.string.label_service_not_available);
                        return true;
                    }
                    mFileTransferService.deleteFileTransfer(transferId);
                    return true;
            }
        } catch (RcsServiceException e) {
            showExceptionThenExit(e);
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        /* Create a new CursorLoader with the following query parameters. */
        return new CursorLoader(this, FileTransferLog.CONTENT_URI, PROJECTION, null, null,
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
