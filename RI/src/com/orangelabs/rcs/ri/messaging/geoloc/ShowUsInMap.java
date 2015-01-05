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

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.gsma.services.rcs.RcsCommon;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.Geoloc;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Show us in a map
 */
public class ShowUsInMap extends MapActivity {
	/**
	 * Intent parameters
	 */
	public final static String EXTRA_CONTACTS = "contacts";
	
	private final static String QUERY_SORT_ORDER = new StringBuilder(ChatLog.Message.TIMESTAMP).append(" DESC").toString();
	private final static String QUERY_WHERE_CLAUSE = new StringBuilder(ChatLog.Message.MIME_TYPE).append("='")
			.append(ChatLog.Message.MimeType.GEOLOC_MESSAGE).append("' AND ").append(ChatLog.Message.DIRECTION).append(" = ")
			.append(RcsCommon.Direction.OUTGOING).toString();
	private final static String[] QUERY_PROJECTION = new String[] { ChatLog.Message.CONTENT };

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
		
		// Create an overlay
		Drawable drawable = getResources().getDrawable(R.drawable.ri_map_icon);
		GeolocOverlay overlay = new GeolocOverlay(this, drawable);
		
		// Add an overlay item for each contact having a geoloc info		
		Geoloc lastGeoloc = null;
		ArrayList<String> contacts = getIntent().getStringArrayListExtra(EXTRA_CONTACTS);
		for (int i=0; i < contacts.size(); i++) {
			// Get geoloc of a contact
			String contact = contacts.get(i);
			
			//Get the last incoming geoloc for a contact
			Geoloc geoloc = getLastGeoloc(contact);
			if (geoloc != null) {
				// Add an overlay item
				overlay.addOverlayItem(contact, geoloc.getLabel(), geoloc.getLatitude(), geoloc.getLongitude(), geoloc.getAccuracy());
				lastGeoloc = geoloc;
			}
		}
		
		// Get my last geoloc
		Geoloc lastOutgoingGeoloc = getMyLastGeoloc();
		if (lastOutgoingGeoloc != null ) {
			// Add an overlay item
			overlay.addOverlayItem(getString(R.string.label_me), lastOutgoingGeoloc.getLabel(), lastOutgoingGeoloc.getLatitude(), lastOutgoingGeoloc.getLongitude(), lastOutgoingGeoloc.getAccuracy());				
		}
		
		if (overlay.size() == 0) {
			Utils.displayLongToast(this, getString(R.string.label_geoloc_not_found));
			return;
		}		
		
		// Add overlays to the map
		mapView.getOverlays().add(overlay);

		// Center the map
		if (lastGeoloc != null ) {
			mapView.getController().setCenter(new GeoPoint((int)(lastGeoloc.getLatitude() * 1E6), (int)(lastGeoloc.getLongitude() * 1E6)));
		}
	}
	
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	/**
	 * Get the last incoming geoloc for a given contact
	 * 
	 * @param contact Contact
	 * @return Geoloc info
	 */
	public Geoloc getLastGeoloc(String contact) {
		Cursor cursor = null;
		String where = new StringBuilder(ChatLog.Message.CONTACT).append("='").append(PhoneNumberUtils.formatNumber(contact))
				.append("' AND ").append(ChatLog.Message.MIME_TYPE).append("='").append(ChatLog.Message.MimeType.GEOLOC_MESSAGE).append("' AND ")
				.append(ChatLog.Message.DIRECTION).append(" = ").append(RcsCommon.Direction.INCOMING).toString();
		try {
			// TODO CR025 Geoloc sharing provider
			cursor = getApplicationContext().getContentResolver().query(ChatLog.Message.CONTENT_URI, QUERY_PROJECTION, where, null,
					QUERY_SORT_ORDER);
			if (cursor.moveToFirst()) {
				String content = cursor.getString(cursor.getColumnIndex(ChatLog.Message.CONTENT));
				return new Geoloc(content);
			}
		} catch (Exception e) {
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return null;
	}

	/**
	 * Get my last geoloc
	 * 
	 * @return Geoloc info
	 */
	private Geoloc getMyLastGeoloc() {
		Cursor cursor = null;
		try {
			// TODO CR025 Geoloc sharing provider
			cursor = getApplicationContext().getContentResolver().query(ChatLog.Message.CONTENT_URI, QUERY_PROJECTION,
					QUERY_WHERE_CLAUSE, null, QUERY_SORT_ORDER);
			if (cursor.moveToFirst()) {
				String content = cursor.getString(cursor.getColumnIndex(ChatLog.Message.CONTENT));
				return new Geoloc(content);
			}
		} catch (Exception e) {
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
		return null;
	}
	
	/**
	 * Start activity
	 * @param context
	 * @param contacts list of contacts
	 */
	public static void startShowUsInMap(Context context, List<String> contacts) {
		Intent intent = new Intent(context, ShowUsInMap.class);
		intent.putStringArrayListExtra(ShowUsInMap.EXTRA_CONTACTS, (ArrayList<String>) contacts);
		context.startActivity(intent);
	}
}
