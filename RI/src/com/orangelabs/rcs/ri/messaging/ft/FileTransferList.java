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

import java.text.DateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
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
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.gsma.services.rcs.RcsCommon;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.ft.FileTransfer;
import com.gsma.services.rcs.ft.FileTransferLog;
import com.orangelabs.rcs.ri.ApiConnectionManager;
import com.orangelabs.rcs.ri.ApiConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * List file transfers from the content provider
 * 
 * @author Jean-Marc AUFFRET
 * @author YPLO6403
 *
 */
public class FileTransferList extends Activity {

	/**
	 * FT_ID is the ID since it is a primary key
	 */
	private static final String FILE_TRANSFER_ID_AS_ID = new StringBuilder(FileTransferLog.FT_ID).append(" AS ")
			.append(BaseColumns._ID).toString();

	// @formatter:off
 	private static final String[] PROJECTION = new String[] {
 			FILE_TRANSFER_ID_AS_ID,
 			FileTransferLog.CONTACT,
 			FileTransferLog.FILENAME,
 			FileTransferLog.FILESIZE,
 			FileTransferLog.STATE,
 			FileTransferLog.DIRECTION,
 			FileTransferLog.TIMESTAMP
     		};
 	// @formatter:on

	private static final String SORT_ORDER = new StringBuilder(FileTransferLog.TIMESTAMP).append(" DESC").toString();

	private ListView mListView;
	
	private ListAdapter mAdapter;
	
	private ApiConnectionManager mCnxManager;
	
	private LockAccess mExitOnce = new LockAccess();
	
	private static final String LOGTAG = LogUtils.getTag(FileTransferList.class.getSimpleName());
	
	/**
	 * List of items for contextual menu
	 */
	private final static int MENU_ITEM_DELETE = 0;
	private final static int MENU_ITEM_RESEND = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set layout
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.filetransfer_list);

		// Set list adapter
		mListView = (ListView) findViewById(android.R.id.list);
		TextView emptyView = (TextView) findViewById(android.R.id.empty);
		mListView.setEmptyView(emptyView);
		registerForContextMenu(mListView);
		
		mCnxManager = ApiConnectionManager.getInstance(FileTransferList.this);
		if (mCnxManager == null || !mCnxManager.isServiceConnected(RcsServiceName.FILE_TRANSFER)) {
			Utils.showMessage(FileTransferList.this, getString(R.string.label_api_disabled));
			return;
			
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		mAdapter = createListAdapter();
		// Refresh view
		mListView.setAdapter(mAdapter);
	}

	/**
	 * Create list adapter
	 */
	private FtListAdapter createListAdapter() {
		Cursor cursor = getContentResolver().query(FileTransferLog.CONTENT_URI, PROJECTION, null, null, SORT_ORDER);
		if (cursor == null) {
			Utils.showMessageAndExit(this, getString(R.string.label_load_log_failed));
			return null;
		}
		return new FtListAdapter(this, cursor);
	}

	/**
	 * List adapter
	 */
	private class FtListAdapter extends CursorAdapter {
		/**
		 * Constructor
		 * 
		 * @param context
		 *            Context
		 * @param c
		 *            Cursor
		 */
		public FtListAdapter(Context context, Cursor c) {
			super(context, c);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			LayoutInflater inflater = LayoutInflater.from(context);
			View view = inflater.inflate(R.layout.filetransfer_list_item, parent, false);

			FileTransferItemViewHolder holder = new FileTransferItemViewHolder(view, cursor);
			view.setTag(holder);
			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			FileTransferItemViewHolder holder = (FileTransferItemViewHolder) view.getTag();
			String number = cursor.getString(holder.columnNumber);
			String displayName = RcsDisplayName.getInstance(context).getDisplayName(number);
			holder.numberText.setText(getString(R.string.label_contact, displayName));

			String filename = cursor.getString(holder.columnFilename);
			holder.filenameText.setText(getString(R.string.label_filename, filename));

			Long filesize = cursor.getLong(holder.columnFilesize);
			holder.filesizeText.setText(getString(R.string.label_filesize, filesize));

			int state = cursor.getInt(holder.columnState);
			holder.stateText.setText(getString(R.string.label_session_state, decodeState(state)));

			int direction = cursor.getInt(holder.columnDirection);
			holder.directionText.setText(getString(R.string.label_direction, decodeDirection(direction)));

			Long timestamp = cursor.getLong(holder.columnTimestamp);
			holder.timestamptext.setText(getString(R.string.label_session_date, decodeDate(timestamp)));
		}

	}

	/**
	 * A ViewHolder class keeps references to children views to avoid unnecessary calls to findViewById() or getColumnIndex() on
	 * each row.
	 */
	private class FileTransferItemViewHolder {
		int columnFilename;
		int columnFilesize;
		int columnDirection;
		int columnState;
		int columnTimestamp;
		int columnNumber;

		TextView numberText;
		TextView filenameText;
		TextView filesizeText;
		TextView stateText;
		TextView directionText;
		TextView timestamptext;

		public FileTransferItemViewHolder(View base, Cursor cursor) {
			columnNumber = cursor.getColumnIndex(FileTransferLog.CONTACT);
			columnFilename = cursor.getColumnIndex(FileTransferLog.FILENAME);
			columnFilesize = cursor.getColumnIndex(FileTransferLog.FILESIZE);
			columnState = cursor.getColumnIndex(FileTransferLog.STATE);
			columnDirection = cursor.getColumnIndex(FileTransferLog.DIRECTION);
			columnTimestamp = cursor.getColumnIndex(FileTransferLog.TIMESTAMP);
			numberText = (TextView) base.findViewById(R.id.number);
			filenameText = (TextView) base.findViewById(R.id.filename);
			filesizeText = (TextView) base.findViewById(R.id.filesize);
			stateText = (TextView) base.findViewById(R.id.state);
			directionText = (TextView) base.findViewById(R.id.direction);
			timestamptext = (TextView) base.findViewById(R.id.date);
		}
	}

	/**
	 * Decode state
	 * 
	 * @param state
	 *            State
	 * @return String
	 */
	private String decodeState(int state) {
		switch (state) {
		case FileTransfer.State.INVITED:
			return getString(R.string.label_state_invited);
		case FileTransfer.State.INITIATING:
			return getString(R.string.label_state_initiating);
		case FileTransfer.State.STARTED:
			return getString(R.string.label_state_started);
		case FileTransfer.State.TRANSFERRED:
			return getString(R.string.label_state_transferred);
		case FileTransfer.State.ABORTED:
			return getString(R.string.label_state_aborted);
		case FileTransfer.State.FAILED:
			return getString(R.string.label_state_failed);
		case FileTransfer.State.PAUSED:
			return getString(R.string.label_state_paused);
		case FileTransfer.State.REJECTED:
			return getString(R.string.label_state_rejected);
		case FileTransfer.State.ACCEPTING:
			return getString(R.string.label_state_accepting);
		case FileTransfer.State.DELIVERED:
			return getString(R.string.label_state_delivered);
		case FileTransfer.State.DISPLAYED:
			return getString(R.string.label_state_displayed);
		case FileTransfer.State.QUEUED:
			return getString(R.string.label_state_queued);
		default:
			return getString(R.string.label_state_unknown);
		}
	}

	/**
	 * Decode direction
	 * 
	 * @param direction
	 *            Direction
	 * @return String
	 */
	private String decodeDirection(int direction) {
		if (direction == RcsCommon.Direction.INCOMING) {
			return getString(R.string.label_incoming);
		} else {
			return getString(R.string.label_outgoing);
		}
	}

	/**
	 * Decode date
	 * 
	 * @param date
	 *            Date
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
			// Delete all TODO CR005 delete method
			getContentResolver().delete(FileTransferLog.CONTENT_URI, null, null);

			// Refresh view
			mListView.setAdapter(createListAdapter());
			break;
		}
		return true;
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		// Get the list item position
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		Cursor cursor = (Cursor) mAdapter.getItem(info.position);
		menu.add(0, MENU_ITEM_DELETE, 0, R.string.menu_delete_message);
		String transferId = cursor.getString(cursor.getColumnIndexOrThrow(BaseColumns._ID));
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "onCreateContextMenu ftId=".concat(transferId));
		}
		
		// Check if resend is allowed
		try {
			FileTransfer transfer = mCnxManager.getFileTransferApi().getFileTransfer(transferId);
			if (transfer.canResendTransfer()) {
				menu.add(0, MENU_ITEM_RESEND, 1, R.string.menu_resend_message);
			}
		} catch (RcsServiceException e) {
			Utils.showMessageAndExit(FileTransferList.this, getString(R.string.label_api_disabled),
					mExitOnce, e);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		Cursor cursor = (Cursor) (mAdapter.getItem(info.position));
		String transferId = cursor.getString(cursor.getColumnIndexOrThrow(BaseColumns._ID));
		switch (item.getItemId()) {
		case MENU_ITEM_RESEND:
			if (LogUtils.isActive) {
				Log.d(LOGTAG, "onContextItemSelected resend ftId=".concat(transferId));
			}
			try {
				FileTransfer transfer = mCnxManager.getFileTransferApi().getFileTransfer(transferId);
				transfer.resendTransfer();
			} catch (RcsServiceException e) {
				Utils.showMessageAndExit(FileTransferList.this, getString(R.string.label_resend_failed),
						mExitOnce, e);
			}
			return true;
			
		case MENU_ITEM_DELETE:
			// TODO CR005 delete methods
			return true;
			
		default:
			return super.onContextItemSelected(item);
		}
	}
}
