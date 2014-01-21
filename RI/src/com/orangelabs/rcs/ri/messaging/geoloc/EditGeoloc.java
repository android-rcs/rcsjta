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
package com.orangelabs.rcs.ri.messaging.geoloc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.JoynServiceException;
import com.gsma.services.rcs.JoynServiceListener;
import com.gsma.services.rcs.JoynServiceNotAvailableException;
import com.gsma.services.rcs.chat.ChatService;
import com.gsma.services.rcs.chat.Geoloc;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Geoloc info editor 
 */
public class EditGeoloc extends Activity implements JoynServiceListener {
	/**
	 * Intent parameters
	 */
	public final static String EXTRA_GEOLOC = "geoloc";
	
	/**
	 * Location label editor
	 */
	private EditText locationEdit;
	
	/**
	 * Latitude editor
	 */
	private EditText latitudeEdit;
	
	/**
	 * Longitude editor
	 */
	private EditText longitudeEdit;
	
	/**
	 * Altitude editor
	 */
	private EditText altitudeEdit;

	/**
	 * Accuracy editor
	 */
	private EditText accuracyEdit;

	/**
	 * Activity result constant
	 */
	public final static int SELECT_GEOLOCATION = 0;
	
    /**
	 * Chat API
	 */
    private ChatService chatApi;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Set title
		setTitle(R.string.menu_send_geoloc);
		
		// Set layout
		setContentView(R.layout.geoloc_edit);

		// Create editors
		locationEdit = (EditText)findViewById(R.id.location);
		latitudeEdit = (EditText)findViewById(R.id.latitude);
		longitudeEdit = (EditText)findViewById(R.id.longitude);
		altitudeEdit = (EditText)findViewById(R.id.altitude);
		accuracyEdit = (EditText)findViewById(R.id.accuracy);
		
        // Set button callback
        Button validateBtn = (Button)findViewById(R.id.validate_btn);
        validateBtn.setOnClickListener(btnValidateListener);
        validateBtn.setEnabled(false);
        
        // Set button callback
        Button selectBtn = (Button)findViewById(R.id.select_geoloc_btn);
        selectBtn.setOnClickListener(btnSelectListener);
        
        // Display my current location
        setMyLocation();
        
        // Instanciate API
        chatApi = new ChatService(getApplicationContext(), this);
        
        // Connect API
        chatApi.connect();
	}
    
    @Override
    public void onDestroy() {
    	super.onDestroy();

        // Disconnect API
        chatApi.disconnect();
    }

    /**
     * Callback called when service is connected. This method is called when the
     * service is well connected to the RCS service (binding procedure successfull):
     * this means the methods of the API may be used.
     */
    public void onServiceConnected() {
		try {
			// Set max label length
			int maxLabelLength = chatApi.getConfiguration().getGeolocLabelMaxLength();
			if (maxLabelLength > 0) {
				InputFilter maxLengthFilter = new InputFilter.LengthFilter(maxLabelLength);
				locationEdit.setFilters(new InputFilter[]{ maxLengthFilter });
			}

			// Enable button
	        Button validateBtn = (Button)findViewById(R.id.validate_btn);
	        validateBtn.setEnabled(true);
	    } catch(JoynServiceNotAvailableException e) {
	    	e.printStackTrace();
			Utils.showMessageAndExit(EditGeoloc.this, getString(R.string.label_api_disabled));
	    } catch(JoynServiceException e) {
	    	e.printStackTrace();
			Utils.showMessageAndExit(EditGeoloc.this, getString(R.string.label_api_failed));
		}
    }
    
    /**
     * Callback called when service has been disconnected. This method is called when
     * the service is disconnected from the RCS service (e.g. service deactivated).
     * 
     * @param error Error
     * @see JoynService.Error
     */
    public void onServiceDisconnected(int error) {
		Utils.showMessageAndExit(EditGeoloc.this, getString(R.string.label_api_disabled));
    }    
    
	/**
	 * Set the location of the device
	 */
	protected void setMyLocation() {
		LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		String bestProvider = lm.getBestProvider(criteria, false);
		
		Location lastKnownLoc = lm.getLastKnownLocation(bestProvider);
		if (lastKnownLoc!= null) {
			latitudeEdit.setText(String.valueOf(lastKnownLoc.getLatitude()));
			longitudeEdit.setText(String.valueOf(lastKnownLoc.getLongitude()));
			altitudeEdit.setText(String.valueOf(lastKnownLoc.getAltitude()));
			accuracyEdit.setText(String.valueOf(lastKnownLoc.getAccuracy()));
		}
		super.onResume();
	}
	
    /**
     * Validate button listener
     */
    private OnClickListener btnValidateListener = new OnClickListener() {
        public void onClick(View v) {
        	String lat = latitudeEdit.getText().toString().trim();
    		if (lat.length() == 0) { 	
    			latitudeEdit.setText("0.0");
    		}
    		
        	String lon = longitudeEdit.getText().toString().trim();
    		if (lon.length() == 0) { 	
    			longitudeEdit.setText("0.0");
    		}
    		
        	String alt = altitudeEdit.getText().toString().trim();
    		if (alt.length() == 0) { 	
    			altitudeEdit.setText("0.0");
    		}
    		
        	String acc = accuracyEdit.getText().toString().trim();
    		if (acc.length() == 0) { 	
    			accuracyEdit.setText("0");
    		}

			long expiration = 0L;
			try {
				expiration = System.currentTimeMillis() + chatApi.getConfiguration().getGeolocExpirationTime();
			} catch(Exception e) {
				e.printStackTrace();
			}
    		Geoloc geoloc = new Geoloc(locationEdit.getText().toString(),
    				Double.parseDouble(lat), Double.parseDouble(lon), Double.parseDouble(alt),
    				expiration,
    				Float.parseFloat(acc));
    		Intent in = new Intent();
    		in.putExtra(EXTRA_GEOLOC, geoloc);
    		setResult(-1, in);
    		finish();
        }
    };
    
    /**
     * Select geolocation button listener
     */
    private OnClickListener btnSelectListener = new OnClickListener() {
        public void onClick(View v) {
        	// Start a new activity to send a geolocation
        	Intent geolocSelectIntent = new Intent(EditGeoloc.this, SelectGeoloc.class);        	
        	startActivityForResult(geolocSelectIntent, SELECT_GEOLOCATION);    		
        }
    };
    
    /**
     * On activity result
     * 
     * @param requestCode Request code
     * @param resultCode Result code
     * @param data Data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (resultCode != RESULT_OK) {
    		return;
    	}

    	switch(requestCode) {
	    	case SELECT_GEOLOCATION: {
				// Get selected geoloc
	    		double latitude = data.getDoubleExtra("latitude", 0.0);
	    		double longitude = data.getDoubleExtra("longitude", 0.0);

				// Display geoloc
	    		latitudeEdit.setText(String.valueOf(latitude));
	    		longitudeEdit.setText(String.valueOf(longitude));
	    	}
	    	break;
    	}
    }
}
