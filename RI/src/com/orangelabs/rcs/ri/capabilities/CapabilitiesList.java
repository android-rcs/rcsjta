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
import android.net.Uri;
import android.os.Bundle;
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
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * List capabilities from the content provider 
 *   
 * @author Jean-Marc AUFFRET
 */
public class CapabilitiesList extends Activity {
	
	private static final String[] PROJECTION = new String[] {
        	CapabilitiesLog.ID,
        	CapabilitiesLog.CONTACT,
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
	
	private static final String SORT_ORDER = new StringBuilder(CapabilitiesLog.CONTACT).append(" DESC").toString();
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.capabilities_list);
        
        // Set title
        setTitle(R.string.menu_capabilities_log);

        // Set list adapter
        ListView view = (ListView)findViewById(android.R.id.list);
        TextView emptyView = (TextView)findViewById(android.R.id.empty);
        view.setEmptyView(emptyView);
        CapabilitiesListAdapter adapter = createListAdapter();
        view.setAdapter(adapter);		
    }
    
	/**
	 * Create list adapter
	 */
	private CapabilitiesListAdapter createListAdapter() {
		Uri uri = CapabilitiesLog.CONTENT_URI;
		Cursor cursor = getContentResolver().query(uri, PROJECTION, null, null, SORT_ORDER);
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
    	/**
    	 * Constructor
    	 * 
    	 * @param context Context
    	 * @param c Cursor
    	 */
		public CapabilitiesListAdapter(Context context, Cursor c) {
            super(context, c);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.capabilities_list_item, parent, false);
            
            CapabilitiesItemCache cache = new CapabilitiesItemCache();
    		cache.number = cursor.getString(cursor.getColumnIndex(CapabilitiesLog.CONTACT));
    		cache.im = cursor.getInt(cursor.getColumnIndex(CapabilitiesLog.CAPABILITY_IM_SESSION));
    		cache.ft = cursor.getInt(cursor.getColumnIndex(CapabilitiesLog.CAPABILITY_FILE_TRANSFER));
    		cache.ish = cursor.getInt(cursor.getColumnIndex(CapabilitiesLog.CAPABILITY_IMAGE_SHARE));
    		cache.vsh = cursor.getInt(cursor.getColumnIndex(CapabilitiesLog.CAPABILITY_VIDEO_SHARE));
    		cache.geoloc = cursor.getInt(cursor.getColumnIndex(CapabilitiesLog.CAPABILITY_GEOLOC_PUSH));
    		cache.ipVoiceCall = cursor.getInt(cursor.getColumnIndex(CapabilitiesLog.CAPABILITY_IP_VOICE_CALL));
    		cache.ipVideoCall = cursor.getInt(cursor.getColumnIndex(CapabilitiesLog.CAPABILITY_IP_VIDEO_CALL));

    		String exts = cursor.getString(cursor.getColumnIndex(CapabilitiesLog.CAPABILITY_EXTENSIONS));
    		if (exts != null) {
    			exts = exts.replace(';', '\n');
    		}
    		cache.exts = exts;
    		cache.automata = cursor.getInt(cursor.getColumnIndex(CapabilitiesLog.AUTOMATA));
    		cache.lastRefresh = cursor.getLong(cursor.getColumnIndex(CapabilitiesLog.TIMESTAMP));
            view.setTag(cache);
            
            return view;
        }
        
    	@Override
    	public void bindView(View view, Context context, Cursor cursor) {
    		CapabilitiesItemCache cache = (CapabilitiesItemCache)view.getTag();
    		TextView numberView = (TextView)view.findViewById(R.id.number);
    		numberView.setText(getString(R.string.label_contact, cache.number));
	        CheckBox im = (CheckBox)view.findViewById(R.id.im);
	        im.setChecked(cache.im == CapabilitiesLog.SUPPORTED);
	        CheckBox ft = (CheckBox)view.findViewById(R.id.file_transfer);
	        ft.setChecked(cache.ft == CapabilitiesLog.SUPPORTED);
	        CheckBox imageCSh = (CheckBox)view.findViewById(R.id.image_sharing);
	        imageCSh.setChecked(cache.ish == CapabilitiesLog.SUPPORTED);
	        CheckBox videoCSh = (CheckBox)view.findViewById(R.id.video_sharing);
	        videoCSh.setChecked(cache.vsh == CapabilitiesLog.SUPPORTED);
	        CheckBox geoloc = (CheckBox)view.findViewById(R.id.geoloc_push);
	        geoloc.setChecked(cache.geoloc == CapabilitiesLog.SUPPORTED);
	        CheckBox ipVoiceCall = (CheckBox)view.findViewById(R.id.ip_voice_call);
	        ipVoiceCall.setChecked(cache.ipVoiceCall == CapabilitiesLog.SUPPORTED);
	        CheckBox ipVideoCall = (CheckBox)view.findViewById(R.id.ip_video_call);
	        ipVideoCall.setChecked(cache.ipVideoCall == CapabilitiesLog.SUPPORTED);
    		TextView extsView = (TextView)view.findViewById(R.id.extensions);
    		extsView.setText(cache.exts);
    		CheckBox automata = (CheckBox)view.findViewById(R.id.automata);
	        automata.setChecked(cache.automata == CapabilitiesLog.SUPPORTED);
    		TextView lastRefresh = (TextView)view.findViewById(R.id.last_refresh);
			lastRefresh.setText( DateUtils.getRelativeTimeSpanString(cache.lastRefresh, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE));
    	}
    }

    /**
     * Capabilities item in cache
     */
	private class CapabilitiesItemCache {
		public String number;
		public int im;
		public int ft;
		public int ish;
		public int vsh;
		public int geoloc;
		public int ipVoiceCall;
		public int ipVideoCall;		
		public String exts;
		public int automata;
		public long lastRefresh;
	}    
 }
