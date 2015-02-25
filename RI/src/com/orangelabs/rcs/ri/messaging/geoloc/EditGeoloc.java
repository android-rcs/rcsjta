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
import android.os.Parcelable;
import android.text.InputFilter;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.chat.ChatServiceConfiguration;
import com.gsma.services.rcs.Geoloc;
import com.orangelabs.rcs.ri.ConnectionManager;
import com.orangelabs.rcs.ri.ConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;

/**
 * Geoloc info editor
 */
public class EditGeoloc extends Activity {
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
     * Accuracy editor
     */
    private EditText accuracyEdit;

    /**
     * Activity result constant
     */
    public final static int SELECT_GEOLOCATION = 0;

    private int geolocExpirationTime = 0;

    private int geolocLabelMaxLength = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setContentView(R.layout.geoloc_edit);

        // Create editors
        locationEdit = (EditText) findViewById(R.id.location);
        latitudeEdit = (EditText) findViewById(R.id.latitude);
        longitudeEdit = (EditText) findViewById(R.id.longitude);
        accuracyEdit = (EditText) findViewById(R.id.accuracy);

        // Set button callback
        Button validateBtn = (Button) findViewById(R.id.validate_btn);
        validateBtn.setOnClickListener(btnValidateListener);

        // Set button callback
        Button selectBtn = (Button) findViewById(R.id.select_geoloc_btn);
        selectBtn.setOnClickListener(btnSelectListener);

        // Display my current location
        setMyLocation();

        // Register to API connection manager
        ConnectionManager connectionManager = ConnectionManager.getInstance(this);
        if (connectionManager != null && connectionManager.isServiceConnected(RcsServiceName.CHAT)) {
            try {
                ChatServiceConfiguration configuration = connectionManager.getChatApi()
                        .getConfiguration();
                geolocExpirationTime = configuration.getGeolocExpirationTime();
                geolocLabelMaxLength = configuration.getGeolocLabelMaxLength();
            } catch (RcsServiceException e) {
                // Ignore exception
            }
        }
        if (geolocLabelMaxLength > 0) {
            InputFilter maxLengthFilter = new InputFilter.LengthFilter(geolocLabelMaxLength);
            locationEdit.setFilters(new InputFilter[] {
                    maxLengthFilter
            });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * Set the location of the device
     */
    protected void setMyLocation() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String bestProvider = lm.getBestProvider(criteria, false);

        Location lastKnownLoc = lm.getLastKnownLocation(bestProvider);
        if (lastKnownLoc != null) {
            latitudeEdit.setText(String.valueOf(lastKnownLoc.getLatitude()));
            longitudeEdit.setText(String.valueOf(lastKnownLoc.getLongitude()));
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

            String acc = accuracyEdit.getText().toString().trim();
            if (acc.length() == 0) {
                accuracyEdit.setText("0");
            }

            long expiration = System.currentTimeMillis() + geolocExpirationTime;
            Geoloc geoloc = new Geoloc(locationEdit.getText().toString(), Double.parseDouble(lat),
                    Double.parseDouble(lon), expiration, Float.parseFloat(acc));
            Intent in = new Intent();
            in.putExtra(EXTRA_GEOLOC, (Parcelable) geoloc);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }

        switch (requestCode) {
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
