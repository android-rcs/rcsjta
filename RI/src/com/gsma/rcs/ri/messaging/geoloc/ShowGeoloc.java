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

import com.gsma.rcs.ri.R;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.contact.ContactId;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

/**
 * Activity to show Geoloc
 */
public class ShowGeoloc extends Activity {
    /**
     * Intent parameters
     */
    public final static String EXTRA_GEOLOC = "geoloc";
    public final static String EXTRA_CONTACT = "contact";

    private Geoloc mGeoloc;

    private ContactId mContact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.geoloc_show);

        mGeoloc = getIntent().getParcelableExtra(EXTRA_GEOLOC);
        mContact = getIntent().getParcelableExtra(EXTRA_CONTACT);

        TextView contactText = (TextView) findViewById(R.id.contact);
        contactText.setText(mContact.toString());

        TextView locationText = (TextView) findViewById(R.id.location);
        locationText.setText(mGeoloc.getLabel());

        TextView latitudeText = (TextView) findViewById(R.id.latitude);
        latitudeText.setText(Double.toString(mGeoloc.getLatitude()));

        TextView longitudeText = (TextView) findViewById(R.id.longitude);
        longitudeText.setText(Double.toString(mGeoloc.getLongitude()));

        TextView accuracyText = (TextView) findViewById(R.id.accuracy);
        accuracyText.setText(Float.toString(mGeoloc.getAccuracy()));

        Button displayBtn = (Button) findViewById(R.id.display_geoloc_btn);
        displayBtn.setOnClickListener(mBtnDisplayListener);
    }

    private OnClickListener mBtnDisplayListener = new OnClickListener() {
        public void onClick(View v) {
            DisplayGeoloc.showContactOnMap(ShowGeoloc.this, mContact, mGeoloc);
        }
    };

    public static void ShowGeolocForContact(Context ctx, ContactId contact, Geoloc geoloc) {
        Intent intent = new Intent(ctx, ShowGeoloc.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_GEOLOC, (Parcelable) geoloc);
        intent.putExtra(EXTRA_CONTACT, (Parcelable) contact);
        ctx.startActivity(intent);
    }
}
