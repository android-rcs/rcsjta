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
import com.gsma.services.rcs.ish.ImageSharing;
import com.gsma.services.rcs.ish.ImageSharingLog;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * List image sharings from the content provider 
 *   
 * @author Jean-Marc AUFFRET
 */
public class ImageSharingList extends Activity {
	
	/**
	 * SHARING_ID is the ID since it is a primary key
	 */
	private static final String SHARING_ID_AS_ID = new StringBuilder(ImageSharingLog.SHARING_ID).append(" AS ")
			.append(BaseColumns._ID).toString();
	
	// @formatter:off
	 private static final String[] PROJECTION = new String[] {
		SHARING_ID_AS_ID,
		ImageSharingLog.CONTACT,
 		ImageSharingLog.FILENAME,
 		ImageSharingLog.FILESIZE,
 		ImageSharingLog.STATE,
 		ImageSharingLog.DIRECTION,
 		ImageSharingLog.TIMESTAMP
	 };
	 // @formatter:on
	 
	private static final String SORT_ORDER = new StringBuilder(ImageSharingLog.TIMESTAMP).append(" DESC").toString();

	/**
	 * List view
	 */
    private ListView listView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.image_sharing_list);
        
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
	private ImageSharingListAdapter createListAdapter() {
		Cursor cursor = getContentResolver().query(ImageSharingLog.CONTENT_URI, PROJECTION, null, null, SORT_ORDER);
		if (cursor == null) {
			Utils.showMessageAndExit(this, getString(R.string.label_load_log_failed));
			return null;
		}
		return new ImageSharingListAdapter(this, cursor);
	}
	
    /**
     * List adapter
     */
    private class ImageSharingListAdapter extends CursorAdapter {
    	/**
    	 * Constructor
    	 * 
    	 * @param context Context
    	 * @param c Cursor
    	 */
		public ImageSharingListAdapter(Context context, Cursor c) {
            super(context, c);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.image_sharing_list_item, parent, false);
            
            ImageSharingItemCache cache = new ImageSharingItemCache(view, cursor);
            view.setTag(cache);
            
            return view;
        }
        
    	@Override
    	public void bindView(View view, Context context, Cursor cursor) {
    		ImageSharingItemCache holder = (ImageSharingItemCache)view.getTag();
    		
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
     * Image sharing item in cache
     */
	private class ImageSharingItemCache {
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
		
		public ImageSharingItemCache(View view, Cursor cursor) {
			columnNumber = cursor.getColumnIndex(ImageSharingLog.CONTACT);
			columnFilename = cursor.getColumnIndex(ImageSharingLog.FILENAME);
			columnFilesize = cursor.getColumnIndex(ImageSharingLog.FILESIZE);
			columnState = cursor.getColumnIndex(ImageSharingLog.STATE);
			columnDirection = cursor.getColumnIndex(ImageSharingLog.DIRECTION);
			columnTimestamp = cursor.getColumnIndex(ImageSharingLog.TIMESTAMP);
			numberText = (TextView) view.findViewById(R.id.number);
			filenameText = (TextView) view.findViewById(R.id.filename);
			filesizeText = (TextView) view.findViewById(R.id.filesize);
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
		case ImageSharing.State.INVITED:
			return getString(R.string.label_state_invited);
		case ImageSharing.State.INITIATED:
			return getString(R.string.label_state_initiated);
		case ImageSharing.State.STARTED:
			return getString(R.string.label_state_started);
		case ImageSharing.State.ABORTED:
			return getString(R.string.label_state_aborted);
		case ImageSharing.State.FAILED:
			return getString(R.string.label_state_failed);
		case ImageSharing.State.TRANSFERRED:
			return getString(R.string.label_state_transferred);
		case ImageSharing.State.REJECTED:
			return getString(R.string.label_state_rejected);
		case ImageSharing.State.RINGING:
			return getString(R.string.label_state_ringing);
		case ImageSharing.State.ACCEPTING:
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
				getContentResolver().delete(ImageSharingLog.CONTENT_URI, null, null);
				
				// Refresh view
		        listView.setAdapter(createListAdapter());	
		        break;
		}
		return true;
	}
}
