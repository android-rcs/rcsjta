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
package com.orangelabs.rcs.ri.messaging;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.widget.ListView;

import com.gsma.services.rcs.GroupDeliveryInfoLog;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.LogUtils;

/**
 * A list view of group chat delivery information where the data comes from a cursor.
 */
public class GroupDeliveryInfoList extends FragmentActivity implements LoaderManager.LoaderCallbacks<Cursor> {
	/**
	 * Intent parameters
	 */
	private static final String EXTRA_MESSAGE_ID = "message_id";
	
	/**
	 * the message ID for which we view the delivery information
	 */
	private String mMessageId;

	/**
	 * The loader's unique ID. Loader IDs are specific to the Activity in which they reside.
	 */
	private static final int LOADER_ID = 1;

	/** 
	 * Contact is the ID since there is a single contact occurrence per message ID
	 */
	private static final String CONTACT_AS_ID = new StringBuilder(GroupDeliveryInfoLog.CONTACT).append(" AS ").append(BaseColumns._ID)
			.toString();

	// @formatter:off
	private static final String[] PROJECTION = new String[] {
		CONTACT_AS_ID,
		GroupDeliveryInfoLog.TIMESTAMP_DELIVERED,
		GroupDeliveryInfoLog.TIMESTAMP_DISPLAYED,
		GroupDeliveryInfoLog.STATUS,
		GroupDeliveryInfoLog.REASON_CODE };
	// @formatter:on
	
	private static final String WHERE_CLAUSE = new StringBuilder(GroupDeliveryInfoLog.ID).append("=?").toString();

	/**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(GroupDeliveryInfoList.class.getSimpleName());

	/**
	 * The adapter that binds data to the ListView
	 */
	private GroupDeliveryInfoCursorAdapter mAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Set layout
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.delivery_info_list);
		mMessageId = getIntent().getStringExtra(EXTRA_MESSAGE_ID);
		// Initialize the adapter.
		mAdapter = new GroupDeliveryInfoCursorAdapter(this, null, 0);

		// Associate the list adapter with the ListView.
		ListView listView = (ListView) findViewById(android.R.id.list);
		listView.setAdapter(mAdapter);
		// Initialize the Loader with id and callbacks 'mCallbacks'.
		getSupportLoaderManager().initLoader(LOADER_ID, null, this);
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "onCreate");
		}
	}

	/**
	 * Start GroupDeliveryInfoList activity
	 * 
	 * @param context
	 * @param messageId
	 */
	public static void startActivity(Context context, String messageId) {
		Intent intent = new Intent(context, GroupDeliveryInfoList.class);
		intent.putExtra(EXTRA_MESSAGE_ID, messageId);
		context.startActivity(intent);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle arg) {
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "onCreateLoader " + id);
		}
		// Create a new CursorLoader with the following query parameters.
		CursorLoader loader = new CursorLoader(this, GroupDeliveryInfoLog.CONTENT_URI, PROJECTION, WHERE_CLAUSE,
				new String[] { mMessageId }, null);
		return loader;
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
		// Remove any references to the old data by replacing it with a null Cursor.
		mAdapter.swapCursor(null);
	}
}