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

package com.orangelabs.rcs.ri.history;

import com.gsma.services.rcs.history.HistoryLog;
import com.gsma.services.rcs.history.HistoryUriBuilder;

import com.orangelabs.rcs.api.connection.utils.RcsActivity;
import com.orangelabs.rcs.ri.R;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
public abstract class HistoryListView extends RcsActivity {

    protected final static int MAX_LENGTH_DESCRIPTION = 25;
    protected final static int MAX_LENGTH_SUBJECT = 15;
    protected final static String SORT_BY = HistoryLog.TIMESTAMP + " DESC";

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

    /**
     * UI handler
     */
    protected Handler mHandler = new Handler();

    /**
     * A method called to query the history log and refresh view
     */
    protected abstract void queryHistoryLogAndRefreshView();

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
                        queryHistoryLogAndRefreshView();
                    }
                });
                builder.setNegativeButton(R.string.label_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                mFilterAlertDialog.dismiss();
                            }
                        });
                mFilterAlertDialog = builder.show();
                registerDialog(mFilterAlertDialog);
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

    /**
     * Truncate a string
     * 
     * @param in string to truncate
     * @param maxLength maximum length
     * @return truncated string
     */
    protected String truncateString(String in, int maxLength) {
        if (in.length() > maxLength) {
            in = in.substring(0, maxLength).concat("...");
        }
        return "\"" + in + "\"";
    }

    /**
     * Sets the providers
     * 
     * @param providers ordered map of providers IDs associated with their name
     */
    protected void setProviders(TreeMap<Integer, String> providers) {
        mProviderIds = new ArrayList<>();
        mFilterMenuItems = new ArrayList<>();
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
        List<Integer> providers = new ArrayList<>();
        for (int i = 0; i < mCheckedProviders.length; i++) {
            if (mCheckedProviders[i]) {
                providers.add(mProviderIds.get(i));
            }
        }
        return providers;
    }
}
