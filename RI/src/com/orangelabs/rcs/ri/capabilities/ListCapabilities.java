package com.orangelabs.rcs.ri.capabilities;

import java.util.Vector;

import org.gsma.joyn.capability.CapabilitiesLog;
import org.gsma.joyn.capability.CapabilityService;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * List capabilities from the content provider 
 *   
 * @author Jean-Marc AUFFRET
 */
public class ListCapabilities extends Activity {
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
        String[] projection = new String[] {
    		"_id",
    		CapabilitiesLog.CONTACT_NUMBER,
    		CapabilitiesLog.CAPABILITY_IM_SESSION,
    		CapabilitiesLog.CAPABILITY_FILE_TRANSFER,
    		CapabilitiesLog.CAPABILITY_IMAGE_SHARE,
    		CapabilitiesLog.CAPABILITY_VIDEO_SHARE,
    		CapabilitiesLog.CAPABILITY_EXTENSIONS
    		};
        String sortOrder = CapabilitiesLog.CONTACT_NUMBER + " DESC ";
		Cursor cursor = getContentResolver().query(uri, projection, null, null, sortOrder);
		if (cursor == null) {
			Utils.showMessageAndExit(this, getString(R.string.label_load_log_failed));
			return null;
		}
			
		Vector<String> items = new Vector<String>();
		MatrixCursor matrix = new MatrixCursor(projection);
		while (cursor.moveToNext()) {
    		String id = cursor.getString(0);
			if (!items.contains(id)) {
				matrix.addRow(new Object[]{
						cursor.getInt(0), 
						cursor.getString(1), 
						cursor.getInt(2), 
						cursor.getInt(3),
						cursor.getInt(4),
						cursor.getInt(5),
						cursor.getString(6)});
				items.add(id);
			}
		}
		cursor.close();

		return new CapabilitiesListAdapter(this, matrix);
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
    		cache.number = cursor.getString(1);
    		cache.im = cursor.getInt(2);
    		cache.ft = cursor.getInt(3);
    		cache.ish = cursor.getInt(4);
    		cache.vsh = cursor.getInt(5);

    		String exts = "";
			String[] extensionList = cursor.getString(6).split(";");
	        for(String value : extensionList) {
	        	if (value.length() > 0) {
	        		exts += "-" + value.substring(CapabilityService.EXTENSION_PREFIX_NAME.length()+1) + "\n";
	        	}
            }
    		cache.exts = exts;
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
    		TextView extsView = (TextView)view.findViewById(R.id.extensions);
    		extsView.setText(getString(R.string.label_extensions, cache.exts));
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
		public String exts;
	}    
 }
