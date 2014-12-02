package com.orangelabs.rcs.ri.service;

import java.util.List;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.gsma.services.rcs.RcsUtils;
import com.orangelabs.rcs.ri.R;

/**
 * List others RCS services on the device
 * 
 * @author Jean-Marc AUFFRET
 */
public class OtherServices extends ListActivity {
	/**
	 * List of service detected
	 */
    private static List<ResolveInfo> clients = null;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	    // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.service_other);

		// Get the list of clients
        clients = RcsUtils.getRcsServices(this);

	    // Set list adapter
        String[] items = new String[clients.size()];
        for(int i=0; i < clients.size(); i++) {
        	items[i] = clients.get(i).activityInfo.packageName;
        }
        setListAdapter(new ArrayAdapter<String>(
        	      this,
        	      android.R.layout.simple_expandable_list_item_1,
        	      items)); 
    }
	
    @Override
	protected void onResume() {
    	super.onResume();
    
        // Request status of each stack
        for(int i=0; i < clients.size(); i++) {
        	RcsUtils.isRcsServiceActivated(this, clients.get(i), receiverResult);
        }
    }
    	
    @Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		RcsUtils.loadRcsServiceSettings(this, clients.get(position));
	}

	/**
	 * Receive client status
	 */
	private BroadcastReceiver receiverResult = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle bundle = getResultExtras(false);
			if (bundle == null) {
				return;
			}
			String client = bundle.getString(com.gsma.services.rcs.Intents.Service.EXTRA_PACKAGENAME);
			boolean status = bundle.getBoolean(com.gsma.services.rcs.Intents.Service.EXTRA_STATUS, false);
			
			for(int i=0; i < clients.size(); i++) {
				if (clients.get(i).activityInfo.packageName.equals(client)) {
					View v = OtherServices.this.getListView().getChildAt(i);
					if (v!= null) {
						if (status) {
							v.setBackgroundColor(Color.GREEN);
						} else {
							v.setBackgroundColor(Color.RED);
						}
					}
				}
			}
	    }		
	};
}