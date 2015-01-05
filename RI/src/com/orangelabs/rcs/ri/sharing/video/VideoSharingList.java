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

import java.text.DateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.gsma.services.rcs.RcsCommon;
import com.gsma.services.rcs.vsh.VideoSharing;
import com.gsma.services.rcs.vsh.VideoSharingLog;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * List video sharing from the content provider 
 *   
 * @author Jean-Marc AUFFRET
 * @author YPLO6403
 *
 */
public class VideoSharingList extends Activity {
	/**
	 * SHARING_ID is the ID since it is a primary key
	 */
	private static final String SHARING_ID_AS_ID = new StringBuilder(VideoSharingLog.SHARING_ID).append(" AS ")
			.append(BaseColumns._ID).toString();
	
	// @formatter:off
	 private static final String[] PROJECTION = new String[] {
		SHARING_ID_AS_ID,
		VideoSharingLog.CONTACT,
		VideoSharingLog.DURATION,
		VideoSharingLog.STATE,
		VideoSharingLog.DIRECTION,
		VideoSharingLog.TIMESTAMP
	 };
	 // @formatter:on
	 
	private static final String SORT_ORDER = new StringBuilder(VideoSharingLog.TIMESTAMP).append(" DESC").toString();
	
	/**
	 * List view
	 */
    private ListView listView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.video_sharing_list);
        
        // Set list adapter
        listView = (ListView)findViewById(android.R.id.list);
        TextView emptyView = (TextView)findViewById(android.R.id.empty);
        listView.setEmptyView(emptyView);
    }
    
	@Override
	protected void onResume() {
		super.onResume();

		// Refresh view
		listView.setAdapter(createListAdapter());
	}    
    
	/**
	 * Create list adapter
	 */
	private VideoSharingListAdapter createListAdapter() {
		Cursor cursor = getContentResolver().query(VideoSharingLog.CONTENT_URI, PROJECTION, null, null, SORT_ORDER);
		if (cursor == null) {
			Utils.showMessageAndExit(this, getString(R.string.label_load_log_failed));
			return null;
		}
		return new VideoSharingListAdapter(this, cursor);
	}
	
    /**
     * List adapter
     */
    private class VideoSharingListAdapter extends CursorAdapter {
    	/**
    	 * Constructor
    	 * 
    	 * @param context Context
    	 * @param c Cursor
    	 */
		public VideoSharingListAdapter(Context context, Cursor c) {
            super(context, c);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.video_sharing_list_item, parent, false);
            VideoSharingItemCache cache = new VideoSharingItemCache(view, cursor);
            view.setTag(cache);
            return view;
        }
        
    	@Override
    	public void bindView(View view, Context context, Cursor cursor) {
    		VideoSharingItemCache holder = (VideoSharingItemCache)view.getTag();
    		
    		String number = cursor.getString(holder.columnNumber);
			String displayName = RcsDisplayName.getInstance(context).getDisplayName(number);
			holder.numberText.setText(getString(R.string.label_contact, displayName));
			
			Long duration = cursor.getLong(holder.columnDuration);
    		holder.durationText.setText(getString(R.string.label_video_duration, duration));
    		
    		int state = cursor.getInt(holder.columnState);
    		holder.stateText.setText(getString(R.string.label_session_state, decodeState(state)));
    		
    		int direction = cursor.getInt(holder.columnDirection);
			holder.directionText.setText(getString(R.string.label_direction, decodeDirection(direction)));

			Long timestamp = cursor.getLong(holder.columnTimestamp);
			holder.timestamptext.setText(getString(R.string.label_session_date, decodeDate(timestamp)));
    	}
    }

    /**
     * Video sharing item in cache
     */
	private class VideoSharingItemCache {
		int columnDuration;
		int columnDirection;
		int columnState;
		int columnTimestamp;
		int columnNumber;

		TextView numberText;
		TextView durationText;
		TextView stateText;
		TextView directionText;
		TextView timestamptext;
		
		public VideoSharingItemCache(View view, Cursor cursor) {
			columnNumber = cursor.getColumnIndex(VideoSharingLog.CONTACT);
			columnDuration = cursor.getColumnIndex(VideoSharingLog.DURATION);
			columnState = cursor.getColumnIndex(VideoSharingLog.STATE);
			columnDirection = cursor.getColumnIndex(VideoSharingLog.DIRECTION);
			columnTimestamp = cursor.getColumnIndex(VideoSharingLog.TIMESTAMP);
			numberText = (TextView) view.findViewById(R.id.number);
			durationText = (TextView) view.findViewById(R.id.duration);
			stateText = (TextView) view.findViewById(R.id.state);
			directionText = (TextView) view.findViewById(R.id.direction);
			timestamptext = (TextView) view.findViewById(R.id.date);
		}
	}    

	/**
	 * Decode state
	 * 
	 * @param state State
	 * @return String
	 */
	private String decodeState(int state) {
		switch (state) {
		case VideoSharing.State.INVITED:
			return getString(R.string.label_state_invited);
		case VideoSharing.State.INITIATING:
			return getString(R.string.label_state_initiating);
		case VideoSharing.State.STARTED:
			return getString(R.string.label_state_started);
		case VideoSharing.State.ABORTED:
			return getString(R.string.label_state_aborted);
		case VideoSharing.State.FAILED:
			return getString(R.string.label_state_failed);
		case VideoSharing.State.REJECTED:
			return getString(R.string.label_state_rejected);
		case VideoSharing.State.RINGING:
			return getString(R.string.label_state_ringing);
		case VideoSharing.State.ACCEPTING:
			return getString(R.string.label_state_accepting);
		default:
			return getString(R.string.label_state_unknown);
		}
	}

	/**
	 * Decode direction
	 * 
	 * @param direction Direction
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
	 * @param date Date
	 * @return String
	 */
	private String decodeDate(long date) {
		return DateFormat.getInstance().format(new Date(date));
	}

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater=new MenuInflater(getApplicationContext());
		inflater.inflate(R.menu.menu_log, menu);

		return true;
	}
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_clear_log:
				// Delete all
				getContentResolver().delete(VideoSharingLog.CONTENT_URI, null, null);
				
				// Refresh view
		        listView.setAdapter(createListAdapter());		
				break;
		}
		return true;
	}
}
