package com.orangelabs.rcs.ri.service;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.TextView;

import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.JoynServiceListener;
import com.gsma.services.rcs.capability.CapabilityService;
import com.orangelabs.rcs.ri.R;

/**
 * Display and monitor the service status
 *  
 * @author Jean-Marc AUFFRET
 */
public class ServiceStatus extends Activity implements JoynServiceListener {
	/**
	 * Service API
	 */
    private JoynService serviceApi;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.service_status);
        
        // Set title
        setTitle(R.string.menu_service);
        
    	// Display service status by default
    	displayServiceStatus(false);

        // Register service up event listener
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(JoynService.ACTION_SERVICE_UP);
        registerReceiver(serviceUpListener, intentFilter);
    	
    	// Instanciate API
        serviceApi = new CapabilityService(getApplicationContext(), this);
                
        // Connect API
        serviceApi.connect();
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();
    	
        // Unregister service up event listener
    	try {
            unregisterReceiver(serviceUpListener);
        } catch (IllegalArgumentException e) {
        	// Nothing to do
        }    	
    	
        // Disconnect API
        serviceApi.disconnect();
    }

    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successfull):
     * this means the methods of the API may be used.
     */
    public void onServiceConnected() {
    	// Display service status
    	displayServiceStatus(true);
    }
    
    /**
     * Callback called when service has been disconnected. This method is called when
     * the service is disconnected from the RCS service (e.g. service deactivated).
     * 
     * @param error Error
     * @see JoynService.Error
     */
    public void onServiceDisconnected(int error) {
    	// Display service status
    	displayServiceStatus(false);
    }    
    
    /**
     * Display service status
     * 
     * @param status Status
     */
    private void displayServiceStatus(boolean status) {
    	TextView statusTxt = (TextView)findViewById(R.id.service_status);			
    	statusTxt.setText("" + status);
    }
    
    /**
     * Joyn service up event listener
     */
    private BroadcastReceiver serviceUpListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
        	// Retry a connection to the service
            serviceApi.connect();
        }
    };    
}
