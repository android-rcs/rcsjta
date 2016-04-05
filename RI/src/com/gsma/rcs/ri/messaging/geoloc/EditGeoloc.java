/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
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

package com.gsma.rcs.ri.messaging.geoloc;

import com.gsma.rcs.api.connection.ConnectionManager.RcsServiceName;
import com.gsma.rcs.api.connection.utils.ExceptionUtil;
import com.gsma.rcs.api.connection.utils.RcsActivity;
import com.gsma.rcs.ri.R;
import com.gsma.rcs.ri.utils.LogUtils;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.chat.ChatServiceConfiguration;

import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.InputFilter;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

/**
 * Geoloc info editor
 */
public class EditGeoloc extends RcsActivity {
    /**
     * Intent parameters
     */
    public final static String EXTRA_GEOLOC = "geoloc";

    private EditText mLocationEdit;

    private EditText mLatitudeEdit;

    private EditText mLongitudeEdit;

    private EditText mAccuracyEdit;

    private final static int REQUEST_CODE_SELECT_GEOLOC = 0;

    private long geolocExpirationTime = 0;

    private int geolocLabelMaxLength = 0;

    private static final String LOGTAG = LogUtils.getTag(EditGeoloc.class.getSimpleName());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.geoloc_edit);
        /* Create editors */
        mLocationEdit = (EditText) findViewById(R.id.location);
        mLatitudeEdit = (EditText) findViewById(R.id.latitude);
        mLongitudeEdit = (EditText) findViewById(R.id.longitude);
        mAccuracyEdit = (EditText) findViewById(R.id.accuracy);

        /* Set button callback */
        Button validateBtn = (Button) findViewById(R.id.validate_btn);
        validateBtn.setOnClickListener(btnValidateListener);

        Button selectBtn = (Button) findViewById(R.id.select_geoloc_btn);
        selectBtn.setOnClickListener(btnSelectListener);

        // Display my current location
        setMyLocation();

        // Register to API connection manager
        if (isServiceConnected(RcsServiceName.CHAT)) {
            try {
                ChatServiceConfiguration configuration = getChatApi().getConfiguration();
                geolocExpirationTime = configuration.getGeolocExpirationTime();
                geolocLabelMaxLength = configuration.getGeolocLabelMaxLength();

            } catch (RcsServiceException e) {
                Log.w(LOGTAG, ExceptionUtil.getFullStackTrace(e));
            }
        }
        if (geolocLabelMaxLength > 0) {
            InputFilter maxLengthFilter = new InputFilter.LengthFilter(geolocLabelMaxLength);
            mLocationEdit.setFilters(new InputFilter[] {
                maxLengthFilter
            });
        }
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
            mLatitudeEdit.setText(String.valueOf(lastKnownLoc.getLatitude()));
            mLongitudeEdit.setText(String.valueOf(lastKnownLoc.getLongitude()));
            mAccuracyEdit.setText(String.valueOf(lastKnownLoc.getAccuracy()));
        }
        super.onResume();
    }

    /**
     * Validate button listener
     */
    private OnClickListener btnValidateListener = new OnClickListener() {
        public void onClick(View v) {
            String lat = mLatitudeEdit.getText().toString().trim();
            if (lat.length() == 0) {
                mLatitudeEdit.setText("0.0");
            }
            String lon = mLongitudeEdit.getText().toString().trim();
            if (lon.length() == 0) {
                mLongitudeEdit.setText("0.0");
            }
            String acc = mAccuracyEdit.getText().toString().trim();
            if (acc.length() == 0) {
                mAccuracyEdit.setText("0");
            }
            long expiration = System.currentTimeMillis() + geolocExpirationTime;
            Geoloc geoloc = new Geoloc(mLocationEdit.getText().toString(), Double.parseDouble(lat),
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
            startActivityForResult(geolocSelectIntent, REQUEST_CODE_SELECT_GEOLOC);
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case REQUEST_CODE_SELECT_GEOLOC:
                Geoloc geoloc = data.getParcelableExtra(EXTRA_GEOLOC);
                mLatitudeEdit.setText(String.valueOf(geoloc.getLatitude()));
                mLongitudeEdit.setText(String.valueOf(geoloc.getLongitude()));
                break;
        }
    }
}
