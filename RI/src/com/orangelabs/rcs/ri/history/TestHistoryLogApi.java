
package com.orangelabs.rcs.ri.history;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.history.messaging.MessagingListView;
import com.orangelabs.rcs.ri.history.sharing.SharingListView;

import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * History log
 */
public class TestHistoryLogApi extends ListActivity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Set items
        String[] items = {
                getString(R.string.menu_history_log_messaging),
                getString(R.string.menu_history_log_sharing)
        };
        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        switch (position) {
            case 0:
                startActivity(new Intent(this, MessagingListView.class));
                break;
            case 1:
                startActivity(new Intent(this, SharingListView.class));
                break;
        }
    }

}
