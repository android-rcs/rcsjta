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

package com.orangelabs.rcs.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.orangelabs.rcs.R;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.service.LauncherUtils;
import com.orangelabs.rcs.service.ServiceUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Settings display
 * 
 * @author Jean-Marc AUFFRET
 */
public class SettingsDisplay extends PreferenceActivity implements Preference.OnPreferenceChangeListener {
    // Dialog IDs
    private final static int SERVICE_DEACTIVATION_CONFIRMATION_DIALOG = 1;
    
    /**
     * Service flag
     */
    private CheckBoxPreference rcsCheckbox;
    
    /**
     * Battery level
     */
    private ListPreference batteryLevel;

    /**
     * Minimum storage capacity
     */
    private ListPreference minStorage;

    /**
     * User profile preference
     */
    private static Preference presencePref = null;
    
    /**
     * UI handler
     */
    private Handler handler = new Handler();
    
    /**
     * Id of the current dialog shown, 0 if none.
     */
    private int currentDialog = 0;
    
	/**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		// Set title
        setTitle(R.string.rcs_settings_title_settings);
        addPreferencesFromResource(R.xml.rcs_settings_preferences);

        // Instantiate the settings provider
        RcsSettings.createInstance(getApplicationContext());
        RcsSettings rcsSettings = RcsSettings.getInstance();
        
        // Save user profile preference the first time
		presencePref = (Preference)getPreferenceScreen().findPreference("presence_settings");

        // Battery level
        batteryLevel = (ListPreference) findPreference("min_battery_level");
        batteryLevel.setPersistent(false);
        batteryLevel.setOnPreferenceChangeListener(this);
        batteryLevel.setValue("" + rcsSettings.getMinBatteryLevel());

        // Minimum storage
        minStorage = (ListPreference) findPreference("min_storage");
        minStorage.setPersistent(false);
        minStorage.setOnPreferenceChangeListener(this);
		minStorage.setValue("" + rcsSettings.getMinStorageCapacity() / 1024L);

    	// Modify the intents so the activities can be launched even if not defined in this application (i.e. RCS apps)
    	int totalNumberOfPreferences = getPreferenceScreen().getPreferenceCount();
    	for (int i=4; i<totalNumberOfPreferences; i++) {
        	Preference preference = getPreferenceScreen().getPreference(i);
        	Intent preferenceIntent = preference.getIntent();
        	String className = preferenceIntent.getComponent().getClassName();
        	String packageName = getApplicationContext().getPackageName();
        	preferenceIntent.setClassName(packageName, className);
        	preferenceIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        	preference.setIntent(preferenceIntent);
    	}
    }

    @Override
    protected void onResume() {
    	super.onResume();
    	
        // Set default value
    	rcsCheckbox = (CheckBoxPreference)getPreferenceScreen().findPreference("rcs_activation");
		rcsCheckbox.setChecked(RcsSettings.getInstance().isServiceActivated());
		
		// Update dynamic menu
    	if (RcsSettings.getInstance().isSocialPresenceSupported()) {
    		getPreferenceScreen().addPreference(presencePref);
    	} else {
    		getPreferenceScreen().removePreference(presencePref);
    	}		
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		new MenuInflater(getApplication()).inflate(R.menu.rcs_settings_main_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int i = item.getItemId();
		if (i == R.id.menu_about) {
	    	Intent intent = new Intent(this, AboutDisplay.class);
	    	startActivity(intent);
			return true;
		} else
		if (i == R.id.menu_help) {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse("https://code.google.com/p/rcsjta/"));
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			return true;
		}		
		return super.onOptionsItemSelected(item);
	}
    
    /**
     * Start RCS service in background
     */
    private void startRcsService() {
    	new StartServiceTask().execute();
    }
    
    /**
     * Stop RCS service in background
     */
    private void stopRcsService() {
    	new StopServiceTask().execute();
    }
    
    /**
     * Stop service thread
     */
    private class StopServiceTask extends AsyncTask<Void, Void, Void> {
    	protected void onPreExecute() {
    		super.onPreExecute();
    		handler.post(new Runnable() {
    			public void run() {
					rcsCheckbox.setEnabled(false);
                    if (currentDialog != 0) {
                        dismissDialog(currentDialog);
                        currentDialog = 0;
                    }
    			}
    		});
    	}
    	
		protected Void doInBackground(Void... params) {
            LauncherUtils.stopRcsService(getApplicationContext());
			return null;
		}
    	
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			handler.post(new Runnable() {
				public void run() {
					rcsCheckbox.setEnabled(true);
				}
			});
		}
    }
    
    /**
     * Start service thread
     */
    private class StartServiceTask extends AsyncTask<Void, Void, Void> {
    	@Override
    	protected void onPreExecute() {
    		super.onPreExecute();
    		handler.post(new Runnable() {
    			public void run() {
					rcsCheckbox.setEnabled(false);
    			}
    		});
    	}
    	
		@Override
		protected Void doInBackground(Void... params) {
			LauncherUtils.launchRcsService(getApplicationContext(), false, true);
			return null;
		}
    	
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			handler.post(new Runnable() {
				public void run() {
					rcsCheckbox.setEnabled(true);
				}
			});
		}
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == rcsCheckbox) {
        	if (rcsCheckbox.isChecked()) {
        		// Deactivate service
        		if (logger.isActivated()) {
        			logger.debug("Start the service");
        		}
	        	RcsSettings.getInstance().setServiceActivationState(true);
	        	startRcsService();
        	} else {
        		// Activate service. If service is running, ask a confirmation 
        		if (ServiceUtils.isServiceStarted(getApplicationContext())) {
        			showDialog(SERVICE_DEACTIVATION_CONFIRMATION_DIALOG);
				}
        	}
        	return true;
        } else {
        	return super.onPreferenceTreeClick(preferenceScreen, preference);
    	}
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
        currentDialog = id;
        switch (id) {
            case SERVICE_DEACTIVATION_CONFIRMATION_DIALOG:
                return new AlertDialog.Builder(this)
                		.setIcon(android.R.drawable.ic_dialog_info)
                		.setTitle(R.string.rcs_settings_label_confirm)
                        .setMessage(R.string.rcs_settings_label_rcs_service_shutdown)
                        .setNegativeButton(R.string.rcs_settings_label_cancel, new DialogInterface.OnClickListener() {
                        	public void onClick(DialogInterface dialog, int button) {
                        		rcsCheckbox.setChecked(!rcsCheckbox.isChecked());
						    }
						})                        
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
							public void onCancel(DialogInterface dialog) {
								rcsCheckbox.setChecked(!rcsCheckbox.isChecked());
							}
						})
                        .setPositiveButton(R.string.rcs_settings_label_ok, new DialogInterface.OnClickListener() {
						    public void onClick(DialogInterface dialog, int button) {
						    	// Stop running service
				        		if (logger.isActivated()) {
				        			logger.debug("Stop the service");
				        		}
				        		stopRcsService();
					        	RcsSettings.getInstance().setServiceActivationState(false);
						    }
						})
                        .setCancelable(true)
                        .create();
        }
        return super.onCreateDialog(id);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference.getKey().equals("min_battery_level")) {
            // Set the min battery level
            int level = 0;
            try {
                level = Integer.parseInt((String)objValue);
            } catch (Exception e) {
                // Nothing to do
            }
            RcsSettings.getInstance().setMinBatteryLevel(level);
        } else
        if (preference.getKey().equals("min_storage")) {
            // Set the min storage capacity
            int value = 0;
            try {
                value = Integer.parseInt((String)objValue);
            } catch (Exception e) {
                // Nothing to do
            }
            RcsSettings.getInstance().setMinStorageCapacity(value);
        }
        return true;
    }

	/**
	 * Is mobile connected in roaming
	 * 
	 * @return Boolean
	 */
	private boolean isMobileRoaming() {
		boolean result = false;
		try {
			ConnectivityManager connectivityMgr = (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo netInfo = connectivityMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            if (netInfo != null) {
                result = netInfo.isRoaming();
            }
		} catch(Exception e) {
            // Nothing to do
		}
		return result;
	}
}
