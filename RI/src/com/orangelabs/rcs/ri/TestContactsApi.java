package com.orangelabs.rcs.ri;

import android.app.ListActivity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Contacts API
 * 
 * @author Jean-Marc AUFFRET
 */
public class TestContactsApi extends ListActivity {
    
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Set items
        String[] items = {
    		getString(R.string.menu_list_rcs_contacts),
    		getString(R.string.menu_list_online_rcs_contacts),        		
    		getString(R.string.menu_list_rcs_contacts_per_service)
        };
        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        switch(position) {
	        case 0:
            	Utils.showMessage(this, getString(R.string.label_not_implemented));
                break;
                
	        case 1:
            	Utils.showMessage(this, getString(R.string.label_not_implemented));
                break;
                
	        case 2:
            	Utils.showMessage(this, getString(R.string.label_not_implemented));
                break;
        }
    }
}
