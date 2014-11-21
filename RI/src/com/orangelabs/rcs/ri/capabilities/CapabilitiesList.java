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
package com.orangelabs.rcs.ri.capabilities;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.gsma.services.rcs.capability.CapabilitiesLog;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * List capabilities from the content provider
 * 
 * @author Jean-Marc AUFFRET
 * @author YPLO6403
 *
 */
public class CapabilitiesList extends Activity {

	/**
	 * Contact is the ID since there is a single contact occurrence per capabilities
	 */
	private static final String CONTACT_AS_ID = new StringBuilder(CapabilitiesLog.CONTACT).append(" AS ").append(BaseColumns._ID)
			.toString();

	// @formatter:off
	private static final String[] PROJECTION = new String[] {
			CONTACT_AS_ID,
        	CapabilitiesLog.CAPABILITY_IM_SESSION,
        	CapabilitiesLog.CAPABILITY_FILE_TRANSFER,
        	CapabilitiesLog.CAPABILITY_IMAGE_SHARE,
        	CapabilitiesLog.CAPABILITY_VIDEO_SHARE,
        	CapabilitiesLog.CAPABILITY_GEOLOC_PUSH,
        	CapabilitiesLog.CAPABILITY_IP_VOICE_CALL,
        	CapabilitiesLog.CAPABILITY_IP_VIDEO_CALL,
        	CapabilitiesLog.CAPABILITY_EXTENSIONS,
        	CapabilitiesLog.AUTOMATA,
        	CapabilitiesLog.TIMESTAMP
    		};
	// @formatter:on

	private static final String SORT_ORDER = new StringBuilder(CapabilitiesLog.CONTACT).append(" DESC").toString();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set layout
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.capabilities_list);

		// Set list adapter
		ListView view = (ListView) findViewById(android.R.id.list);
		TextView emptyView = (TextView) findViewById(android.R.id.empty);
		view.setEmptyView(emptyView);
		CapabilitiesListAdapter adapter = createListAdapter();
		view.setAdapter(adapter);
	}

	/**
	 * Create list adapter
	 */
	private CapabilitiesListAdapter createListAdapter() {
		Cursor cursor = getContentResolver().query(CapabilitiesLog.CONTENT_URI, PROJECTION, null, null, SORT_ORDER);
		if (cursor == null) {
			Utils.showMessageAndExit(this, getString(R.string.label_load_log_failed));
			return null;
		}
		return new CapabilitiesListAdapter(this, cursor);
	}

	/**
	 * List adapter
	 */
	private class CapabilitiesListAdapter extends CursorAdapter {
		private RcsDisplayName rcsDisplayName;
		
		/**
		 * Constructor
		 * 
		 * @param context
		 *            Context
		 * @param c
		 *            Cursor
		 */
		public CapabilitiesListAdapter(Context context, Cursor c) {
			super(context, c);
			rcsDisplayName = RcsDisplayName.getInstance(context);
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			LayoutInflater inflater = LayoutInflater.from(context);
			View view = inflater.inflate(R.layout.capabilities_list_item, parent, false);

			CapabilitiesItemViewHolder holder = new CapabilitiesItemViewHolder(view, cursor);
			view.setTag(holder);
			return view;
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			CapabilitiesItemViewHolder holder = (CapabilitiesItemViewHolder) view.getTag();
			
			// Set display name from number
			String number = cursor.getString(holder.columnContact);
			String displayName = rcsDisplayName.getDisplayName(number);
			holder.numberText.setText(getString(R.string.label_contact, displayName));
			
			holder.imBox.setChecked(cursor.getInt(holder.columnCapabilityIm) == CapabilitiesLog.SUPPORTED);
			holder.ftBox.setChecked(cursor.getInt(holder.columnCapabilityFileTransfer) == CapabilitiesLog.SUPPORTED);
			holder.ishBox.setChecked(cursor.getInt(holder.columnCapabilityImageSharing) == CapabilitiesLog.SUPPORTED);
			holder.vshBox.setChecked(cursor.getInt(holder.columnCapabilityVideoSharing) == CapabilitiesLog.SUPPORTED);
			holder.geolocBox.setChecked(cursor.getInt(holder.columnCapabilityGeolocPush) == CapabilitiesLog.SUPPORTED);
			holder.ipVoiceCallBox.setChecked(cursor.getInt(holder.columnCapabilityIpVoiceCall) == CapabilitiesLog.SUPPORTED);
			holder.ipVideoCallBox.setChecked(cursor.getInt(holder.columnCapabilityIpVideoCall) == CapabilitiesLog.SUPPORTED);
			String exts = cursor.getString(holder.columnCapabilityExtensions);
			if (exts != null) {
				exts = exts.replace(';', '\n');
			}
			holder.extsText.setText(exts);
			holder.automataBox.setChecked(cursor.getInt(holder.columnAutomata) == CapabilitiesLog.SUPPORTED);
			long lastRefresh = cursor.getLong(holder.columnTimestamp);
			holder.lastRefreshText.setText(DateUtils.getRelativeTimeSpanString(lastRefresh, System.currentTimeMillis(),
					DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE));
		}
	}

	/**
	 * A ViewHolder class keeps references to children views to avoid unnecessary calls to findViewById() or getColumnIndex() on each
	 * row.
	 */
	private class CapabilitiesItemViewHolder {
		public TextView numberText;
		public CheckBox imBox;
		public CheckBox ftBox;
		public CheckBox ishBox;
		public CheckBox vshBox;
		public CheckBox geolocBox;
		public CheckBox ipVoiceCallBox;
		public CheckBox ipVideoCallBox;
		public TextView extsText;
		public CheckBox automataBox;
		public TextView lastRefreshText;

		public int columnContact;
		public int columnCapabilityIm;
		public int columnCapabilityImageSharing;
		public int columnCapabilityFileTransfer;
		public int columnCapabilityVideoSharing;
		public int columnCapabilityIpVoiceCall;
		public int columnCapabilityIpVideoCall;
		public int columnCapabilityGeolocPush;
		public int columnCapabilityExtensions;
		public int columnAutomata;
		public int columnTimestamp;

		CapabilitiesItemViewHolder(View base, Cursor cursor) {
			columnContact = cursor.getColumnIndex(BaseColumns._ID);
			columnCapabilityIm = cursor.getColumnIndex(CapabilitiesLog.CAPABILITY_IM_SESSION);
			columnCapabilityFileTransfer = cursor.getColumnIndex(CapabilitiesLog.CAPABILITY_FILE_TRANSFER);
			columnCapabilityImageSharing = cursor.getColumnIndex(CapabilitiesLog.CAPABILITY_IMAGE_SHARE);
			columnCapabilityVideoSharing = cursor.getColumnIndex(CapabilitiesLog.CAPABILITY_VIDEO_SHARE);
			columnCapabilityGeolocPush = cursor.getColumnIndex(CapabilitiesLog.CAPABILITY_GEOLOC_PUSH);
			columnCapabilityIpVoiceCall = cursor.getColumnIndex(CapabilitiesLog.CAPABILITY_IP_VOICE_CALL);
			columnCapabilityIpVideoCall = cursor.getColumnIndex(CapabilitiesLog.CAPABILITY_IP_VIDEO_CALL);
			columnCapabilityExtensions = cursor.getColumnIndex(CapabilitiesLog.CAPABILITY_EXTENSIONS);
			columnAutomata = cursor.getColumnIndex(CapabilitiesLog.AUTOMATA);
			columnTimestamp = cursor.getColumnIndex(CapabilitiesLog.TIMESTAMP);

			numberText = (TextView) base.findViewById(R.id.number);
			imBox = (CheckBox) base.findViewById(R.id.im);
			ftBox = (CheckBox) base.findViewById(R.id.file_transfer);
			ishBox = (CheckBox) base.findViewById(R.id.image_sharing);
			vshBox = (CheckBox) base.findViewById(R.id.video_sharing);
			geolocBox = (CheckBox) base.findViewById(R.id.geoloc_push);
			ipVoiceCallBox = (CheckBox) base.findViewById(R.id.ip_voice_call);
			ipVideoCallBox = (CheckBox) base.findViewById(R.id.ip_video_call);
			extsText = (TextView) base.findViewById(R.id.extensions);
			automataBox = (CheckBox) base.findViewById(R.id.automata);
			lastRefreshText = (TextView) base.findViewById(R.id.last_refresh);
		}

	}
}
