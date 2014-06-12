package com.orangelabs.rcs.ri.ipcall;

import java.text.DateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

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
    		IPCallLog.ID,
    		IPCallLog.CONTACT_NUMBER,
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
    		cache.number = cursor.getString(1);
    		cache.state = cursor.getInt(2);
    		cache.direction = cursor.getInt(3);
    		cache.date = cursor.getLong(4);
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
		if (state == IPCall.State.ABORTED) {
			return getString(R.string.label_state_aborted);
		} else
		if (state == IPCall.State.TERMINATED) {
			return getString(R.string.label_state_terminated);
		} else
		if (state == IPCall.State.FAILED) {
			return getString(R.string.label_state_failed);
		} else
		if (state == IPCall.State.INITIATED) {
			return getString(R.string.label_state_initiated);
		} else
		if (state == IPCall.State.INVITED) {
			return getString(R.string.label_state_invited);
		} else
		if (state == IPCall.State.STARTED) {
			return getString(R.string.label_state_started);
		} else
		if (state == IPCall.State.INACTIVE) {
			return getString(R.string.label_state_inactive);
		} else {
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
		if (direction == IPCall.Direction.INCOMING) {
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
