package com.orangelabs.rcs.ri.capabilities;

import org.gsma.joyn.JoynService;
import org.gsma.joyn.JoynServiceListener;
import org.gsma.joyn.capability.CapabilityService;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Request capabilities of all contacts
 * 
 * @author Jean-Marc AUFFRET
 */
public class RequestAllCapabilities extends Activity implements JoynServiceListener {
    /**
     * UI handler
     */
    private Handler handler = new Handler();    
    
    /**
	 * Capability API
	 */
    private CapabilityService capabilityApi;
	
    /**
     * Progress dialog
     */
    private Dialog progressDialog = null;    
   
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.capabilities_refresh);
        
        // Set title
        setTitle(R.string.menu_refresh_capabilities);
        
		// Set buttons callback
        Button refreshBtn = (Button)findViewById(R.id.refresh_btn);
        refreshBtn.setOnClickListener(btnSyncListener);        
        refreshBtn.setEnabled(false);
        
        // Instanciate API
        capabilityApi = new CapabilityService(getApplicationContext(), this);
                
        // Connect API
        capabilityApi.connect();
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();
    	
        // Disconnect API
    	capabilityApi.disconnect();
    }
    
    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successfull):
     * this means the methods of the API may be used.
     */
    public void onServiceConnected() {
        Button refreshBtn = (Button)findViewById(R.id.refresh_btn);
        refreshBtn.setEnabled(true);
    }
    
    /**
     * Callback called when service has been disconnected. This method is called when
     * the service is disconnected from the RCS service (e.g. service deactivated).
     * 
     * @param error Error
     * @see JoynService.Error
     */
    public void onServiceDisconnected(int error) {
		Utils.showMessageAndExit(RequestAllCapabilities.this, getString(R.string.label_api_disconnected));
	}    

    /**
     * Callback called when service is registered to the RCS/IMS platform
     */
    public void onServiceRegistered() {
    	// Not used here
    }
    
    /**
     * Callback called when service is unregistered from the RCS/IMS platform
     */
    public void onServiceUnregistered() {
    	// Not used here
    }

    /**
     * Publish button listener
     */
    private OnClickListener btnSyncListener = new OnClickListener() {
        public void onClick(View v) {
        	// Execute in background
        	final SyncTask tsk = new SyncTask(capabilityApi);
        	tsk.execute();

        	// Display a progress dialog
            handler.post(new Runnable() { 
                public void run() {
                    progressDialog = Utils.showProgressDialog(RequestAllCapabilities.this,
                            getString(R.string.label_refresh_in_progress));
                    progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                        public void onCancel(DialogInterface dialog) {
                            try {
                                tsk.cancel(true);
                            } catch (Exception e) {
                            }
                        }
                    });
                }
            });
        }
    };
    
    /**
     * Background task
     */
    private class SyncTask extends AsyncTask<Void, Void, Void> {
    	private CapabilityService api; 
    	
    	public SyncTask(CapabilityService api) {
    		this.api = api;
    	}
    	
        protected Void doInBackground(Void... unused) {        	
        	try {
    			// Refresh all contacts
        		api.requestAllContactsCapabilities();
        	} catch(Exception e) {
    	    	e.printStackTrace();
        		Utils.showMessage(RequestAllCapabilities.this, getString(R.string.label_refresh_failed));
        	}
        	return null;
        }

        protected void onPostExecute(Void unused) {
			// Hide progress dialog
    		if (progressDialog != null && progressDialog.isShowing()) {
    			progressDialog.dismiss();
    			progressDialog = null;
    		}
    		
    		// Display message
			Utils.displayLongToast(RequestAllCapabilities.this, getString(R.string.label_refresh_success));
        }
    }    
}
