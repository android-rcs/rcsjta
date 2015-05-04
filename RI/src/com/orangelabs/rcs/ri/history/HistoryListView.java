
package com.orangelabs.rcs.ri.history;

import com.gsma.services.rcs.history.HistoryLog;
import com.gsma.services.rcs.history.HistoryUriBuilder;

import com.orangelabs.rcs.ri.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * History log
 */
public abstract class HistoryListView extends Activity {

    protected final static int MAX_LENGTH_DESCRIPTION = 25;
    protected final static int MAX_LENGTH_SUBJECT = 15;
    protected final static String SORT_BY = new StringBuilder(HistoryLog.TIMESTAMP).append(" DESC")
            .toString();

    /**
     * AlertDialog to show for selecting filters.
     */
    private AlertDialog mFilterAlertDialog;

    private List<String> mFilterMenuItems;

    private boolean[] mCheckedProviders;

    /**
     * Associate the providers position in filter menu with providerIds defined in HistoryLog
     */
    private List<Integer> mProviderIds;

    protected abstract void startQuery();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(getApplicationContext());
        inflater.inflate(R.menu.menu_historylog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_filter:
                AlertDialog.Builder builder = new AlertDialog.Builder(HistoryListView.this);
                builder.setTitle(R.string.title_history_log_dialog_filter_logs_title);
                builder.setMultiChoiceItems(
                        mFilterMenuItems.toArray(new CharSequence[mFilterMenuItems.size()]),
                        mCheckedProviders, new DialogInterface.OnMultiChoiceClickListener() {
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                mCheckedProviders[which] = isChecked;
                            }
                        });
                builder.setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mFilterAlertDialog.dismiss();
                        startQuery();
                    }
                });
                builder.setNegativeButton(R.string.label_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                mFilterAlertDialog.dismiss();
                            }
                        });
                mFilterAlertDialog = builder.show();
                break;
        }
        return true;
    }

    /**
     * Create History URI from list of provider IDs
     * 
     * @param providerIds list of provider IDs
     * @return Uri
     */
    protected Uri createHistoryUri(List<Integer> providerIds) {
        HistoryUriBuilder uriBuilder = new HistoryUriBuilder(HistoryLog.CONTENT_URI);
        for (int providerId : providerIds) {
            uriBuilder.appendProvider(providerId);
        }
        return uriBuilder.build();
    }

    protected String truncateString(String in, int maxLength) {
        if (in.length() > maxLength) {
            in = in.substring(0, maxLength).concat("...");
        }
        return new StringBuilder("\"").append(in).append("\"").toString();
    }

    /**
     * Sets the providers
     * 
     * @param providers ordered map of providers IDs associated with their name
     */
    protected void setProviders(TreeMap<Integer, String> providers) {
        mProviderIds = new ArrayList<Integer>();
        mFilterMenuItems = new ArrayList<String>();
        for (Entry<Integer, String> entry : providers.entrySet()) {
            mFilterMenuItems.add(entry.getValue());
            mProviderIds.add(entry.getKey());
        }
        /* Upon setting, all providers are checked True */
        mCheckedProviders = new boolean[providers.size()];
        for (int i = 0; i < mCheckedProviders.length; i++) {
            mCheckedProviders[i] = true;
        }
    }

    /**
     * Gets the list of selected providers IDs
     * 
     * @return the list of selected providers IDs
     */
    protected List<Integer> getSelectedProviderIds() {
        List<Integer> providers = new ArrayList<Integer>();
        for (int i = 0; i < mCheckedProviders.length; i++) {
            if (mCheckedProviders[i]) {
                providers.add(mProviderIds.get(i));
            }
        }
        return providers;
    }
}
