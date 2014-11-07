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

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.orangelabs.rcs.ri.R;

/**
 * Select a geoloc from a map
 * 
 * @author vfml3370
 */
public class SelectGeoloc extends MapActivity implements OnTouchListener {
	/**
	 * Intent parameters
	 */
	public final static String EXTRA_LATITUDE = "latitude";
	public final static String EXTRA_LONGITUDE = "longitude";

	/**
	 * MapView
	 */
	private MapView mapView;

	/**
	 * GestureDetector
	 */
	private GestureDetector gestureDectector;
	
	/**
	 * Geo point
	 */
	private GeoPoint geoPoint;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set layout
		setContentView(R.layout.geoloc_select);

		// Set map
		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);
		mapView.setOnTouchListener(this);
		mapView.getController().setZoom(4);
		
		// Set gesture detector
		gestureDectector = new GestureDetector(this, new LearnGestureListener());

		// Clear the list of overlay
		mapView.getOverlays().clear();
		mapView.invalidate();
		
		// Set button callback
		Button selectBtn = (Button)findViewById(R.id.select_btn);
		selectBtn.setOnClickListener(btnSelectListener);	
	}

	/**
	 * Select button listener
	 */
	private OnClickListener btnSelectListener = new OnClickListener() {
		public void onClick(View v) {
			Intent intent = new Intent();    		
			intent.putExtra(EXTRA_LATITUDE, (geoPoint.getLatitudeE6() / 1E6));
			intent.putExtra(EXTRA_LONGITUDE, (geoPoint.getLongitudeE6() / 1E6));
			setResult(RESULT_OK, intent);
			finish();
		}
	};
	
	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	public boolean onTouch(View arg0, MotionEvent arg1) {
		gestureDectector.onTouchEvent(arg1);
		return false;
	}

	/**
	 * Gesture event listener
	 */
	private class LearnGestureListener extends GestureDetector.SimpleOnGestureListener{ 
		private Drawable drawable = getResources().getDrawable(R.drawable.ri_map_icon);

		@Override 
		public boolean onDoubleTap(MotionEvent ev) { 	     
			mapView.getController().zoomIn();
			return true; 
		} 

		@Override 
		public void onLongPress(MotionEvent e) {
			// Get the latitude and the longitude
			geoPoint = mapView.getProjection().fromPixels((int) e.getX(),(int) e.getY());
			
			// Remove all overlay from the list, only one marker on the map
			mapView.getOverlays().removeAll(mapView.getOverlays());		
			
			// Create an overlay
			OverlayItem overlay = new OverlayItem(geoPoint, "", "");
			MyItemizedOverlay itemizedoverlay = new MyItemizedOverlay(drawable);
			itemizedoverlay.addOverlay(overlay);
			itemizedoverlay.setGestureDetector(gestureDectector);
			
			// Add the overlays to the map
			mapView.getOverlays().add(itemizedoverlay);			
		} 
	}
	
	/**
	 * My overlay item
	 */
	private class MyItemizedOverlay extends ItemizedOverlay<OverlayItem>{

		private ArrayList<OverlayItem> overlays = new ArrayList<OverlayItem>();
		
		private GestureDetector gestureDetector;

		public MyItemizedOverlay(Drawable defaultMarker) {
			super(boundCenterBottom(defaultMarker));
		}

		@Override
		protected OverlayItem createItem(int i) {
			return overlays.get(i);
		}

		@Override
		public int size() {
			return overlays.size();
		}

		public void addOverlay(OverlayItem overlay) {
			overlays.add(overlay);
			populate();
		}
		
		public void setGestureDetector(GestureDetector gestureDetector) {
			this.gestureDetector = gestureDetector;
		}
				
		public boolean onTouchEvent(MotionEvent e, MapView mapView) {
			if (gestureDetector.onTouchEvent(e)) {
				return true;
			}
			return false;
		}
	}	
}
