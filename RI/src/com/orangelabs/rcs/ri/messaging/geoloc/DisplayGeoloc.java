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

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Display a geoloc on a Google map
 */
public class DisplayGeoloc extends MapActivity {
	/**
	 * Intent parameters
	 */
	public final static String EXTRA_GEOLOC = "geoloc";
	public final static String EXTRA_CONTACT = "contact";

	/**
	 * Map view
	 */
	private MapView mapView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// Set layout
		setContentView(R.layout.geoloc_display);

		// Set map
		mapView = (MapView)findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);
		mapView.getController().setZoom(4);
		
		// Clear the list of overlay
		mapView.getOverlays().clear();
		mapView.invalidate();		
		
		// Get geoloc value
		ContactId contact = getIntent().getParcelableExtra(EXTRA_CONTACT);
		Geoloc geoloc = getIntent().getParcelableExtra(EXTRA_GEOLOC);
		if ((contact == null) || (geoloc == null)) {
			Utils.showMessageAndExit(this, getString(R.string.label_geoloc_not_found));
			return;
		}
		
		// Create an overlay
		Drawable drawable = getResources().getDrawable(R.drawable.ri_map_icon);
		GeolocOverlay overlay = new GeolocOverlay(this, drawable);

		// Add an overlay item
		overlay.addOverlayItem(contact.toString(), geoloc.getLabel(), geoloc.getLatitude(), geoloc.getLongitude(), geoloc.getAccuracy());

		// Add overlay to the map
		mapView.getOverlays().add(overlay);
		
		// Center the map
		mapView.getController().setCenter(new GeoPoint((int)(geoloc.getLatitude() * 1E6), (int)(geoloc.getLongitude() * 1E6)));
	}
	
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
}
