package com.orangelabs.rcs.ri.intents;

import java.util.List;

import org.gsma.joyn.Intents;
import org.gsma.joyn.JoynUtils;

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

import com.orangelabs.rcs.ri.R;

/**
 * List existing joyn clients
 * 
 * @author Jean-Marc AUFFRET
 */
public class Clients extends ListActivity {
	/**
	 * List of clients detected
	 */
    private List<ResolveInfo> clients;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	    // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.intents_clients);

		// Set UI title
        setTitle(getString(R.string.menu_clients));
        
		// Get the list of clients
	    clients = JoynUtils.getJoynClients(this);

	    // Set list adapter
        String[] items = new String[clients.size()];
        for(int i=0; i < clients.size(); i++) {
        	items[i] = clients.get(i).activityInfo.packageName;
        	JoynUtils.isJoynClientActivated(this, clients.get(i), receiverResult);
        }
        setListAdapter(new ArrayAdapter<String>(
        	      this,
        	      android.R.layout.simple_expandable_list_item_1,
        	      items)); 
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		JoynUtils.loadJoynClientSettings(this, clients.get(position));
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
			String client = bundle.getString(Intents.Client.EXTRA_CLIENT);
			boolean status = bundle.getBoolean(Intents.Client.EXTRA_STATUS, false);
			
			for(int i=0; i < clients.size(); i++) {
				if (clients.get(i).activityInfo.packageName.equals(client)) {
					View v = Clients.this.getListView().getChildAt(i);
					if (status) {
						v.setBackgroundColor(Color.GREEN);
					} else {
						v.setBackgroundColor(Color.RED);
					}
				}
			}
	    }		
	};
}