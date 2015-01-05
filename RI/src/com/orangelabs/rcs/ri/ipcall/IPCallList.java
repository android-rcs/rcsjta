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
package com.orangelabs.rcs.ri.ipcall;

import java.text.DateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
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
import com.gsma.services.rcs.ipcall.IPCall;
import com.gsma.services.rcs.ipcall.IPCallLog;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * List calls from the content provider 
 *   
 * @author Jean-Marc AUFFRET
 */
public class IPCallList extends Activity {
	
	/**
	 * Contact is the ID since there is a single contact occurrence in the query result
	 */
	private static final String CONTACT_AS_ID = new StringBuilder(IPCallLog.CONTACT).append(" AS ").append(BaseColumns._ID)
			.toString();

	
	/**
	 * List view
	 */
    private ListView listView;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.ipcall_list);
        
        // Set title
        setTitle(R.string.menu_ipcall_list);

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
	private CallListAdapter createListAdapter() {
		Uri uri = IPCallLog.CONTENT_URI;
        String[] projection = new String[] {
        	CONTACT_AS_ID,
    		IPCallLog.STATE,
    		IPCallLog.DIRECTION,
    		IPCallLog.TIMESTAMP
    		};
        String sortOrder = IPCallLog.TIMESTAMP + " DESC ";
		Cursor cursor = getContentResolver().query(uri, projection, null, null, sortOrder);
		if (cursor == null) {
			Utils.showMessageAndExit(this, getString(R.string.label_load_log_failed));
			return null;
		}
		return new CallListAdapter(this, cursor);
	}
	
    /**
     * List adapter
     */
    private class CallListAdapter extends CursorAdapter {
    	/**
    	 * Constructor
    	 * 
    	 * @param context Context
    	 * @param c Cursor
    	 */
		public CallListAdapter(Context context, Cursor c) {
            super(context, c);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.ipcall_list_item, parent, false);
            
            CallItemCache cache = new CallItemCache();
    		cache.number = cursor.getString(cursor.getColumnIndex(BaseColumns._ID));
    		cache.state = cursor.getInt(cursor.getColumnIndex(IPCallLog.STATE));
    		cache.direction = cursor.getInt(cursor.getColumnIndex(IPCallLog.DIRECTION));
    		cache.date = cursor.getLong(cursor.getColumnIndex(IPCallLog.TIMESTAMP));
            view.setTag(cache);
            
            return view;
        }
        
    	@Override
    	public void bindView(View view, Context context, Cursor cursor) {
    		CallItemCache cache = (CallItemCache)view.getTag();
    		TextView numberView = (TextView)view.findViewById(R.id.number);
    		numberView.setText(getString(R.string.label_contact, cache.number));
    		TextView stateView = (TextView)view.findViewById(R.id.state);
    		stateView.setText(getString(R.string.label_session_state, decodeState(cache.state)));
    		TextView directionView = (TextView)view.findViewById(R.id.direction);
    		directionView.setText(getString(R.string.label_direction, decodeDirection(cache.direction)));
    		TextView dateView = (TextView)view.findViewById(R.id.date);
    		dateView.setText(getString(R.string.label_session_date, decodeDate(cache.date)));
    	}
    }

    /**
     * Call item in cache
     */
	private class CallItemCache {
		public String number;
		public int direction;
		public int state;
		public long date;
	}
	
	/**
	 * Decode state
	 * 
	 * @param state State
	 * @return String
	 */
	private String decodeState(int state) {
		switch (state) {
		case IPCall.State.INVITED:
			return getString(R.string.label_state_invited);
		case IPCall.State.INITIATED:
			return getString(R.string.label_state_initiating);
		case IPCall.State.STARTED:
			return getString(R.string.label_state_started);
		case IPCall.State.ABORTED:
			return getString(R.string.label_state_aborted);
		case IPCall.State.FAILED:
			return getString(R.string.label_state_failed);
		case IPCall.State.REJECTED:
			return getString(R.string.label_state_rejected);
		case IPCall.State.HOLD:
			return getString(R.string.label_state_hold);
		case IPCall.State.ACCEPTING:
			return getString(R.string.label_state_accepting);
		case IPCall.State.RINGING:
			return getString(R.string.label_state_ringing);
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
				getContentResolver().delete(IPCallLog.CONTENT_URI, null, null);
				
				// Refresh view
		        listView.setAdapter(createListAdapter());		
				break;
		}
		return true;
	}	
}
