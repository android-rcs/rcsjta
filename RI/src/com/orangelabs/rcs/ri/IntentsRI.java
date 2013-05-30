package com.orangelabs.rcs.ri;

import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.orangelabs.rcs.ri.intents.Clients;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Intents RI
 * 
 * @author Jean-Marc AUFFRET
 */
public class IntentsRI extends ListActivity {
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Set items
        String[] items = {
    		getString(R.string.menu_clients),
    		getString(R.string.menu_apps)
        };
        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        switch(position) {
	        case 0:
            	startActivity(new Intent(this, Clients.class));
                break;
                
	        case 1:
            	Utils.showMessage(this, getString(R.string.label_not_implemented));
            	// TODO startActivity(new Intent(this, Intents.class));
                break;
        }
    }
}
