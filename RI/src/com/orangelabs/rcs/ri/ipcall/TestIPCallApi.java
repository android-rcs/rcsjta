package com.orangelabs.rcs.ri.ipcall;

import android.app.ListActivity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * IP call API
 * 
 * @author Jean-Marc AUFFRET
 */
public class TestIPCallApi extends ListActivity {

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Set items
        String[] items = {
    		getString(R.string.menu_initiate_ipcall),
    		getString(R.string.menu_ipcall_sessions)
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
        }
    }
}
