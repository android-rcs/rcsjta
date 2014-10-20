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

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.google.android.maps.Projection;
import com.orangelabs.rcs.ri.R;

/**
 * Geoloc overlay
 */
public class GeolocOverlay extends ItemizedOverlay<OverlayItem> {
	/**
	 * Context
	 */
	private Context context;

	/**
	 * Painter
	 */
    private Paint paint;
    
	/**
	 * List of overlay items
	 */
	private ArrayList<AccuracyOverlayItem> overlays = new ArrayList<AccuracyOverlayItem>();
	
	/**
	 * Contsructor
	 * 
	 * @param context Context
	 * @param defaultMarker Marker
	 */
	public GeolocOverlay(Context context, Drawable defaultMarker) {
		super(boundCenterBottom(defaultMarker));
		
		this.context = context;
		this.paint = new Paint();
		this.paint.setColor(Color.BLUE);
		this.paint.setAlpha(25);
		this.paint.setAntiAlias(true);
		this.paint.setStyle(Paint.Style.FILL);
	}

    @Override
	protected OverlayItem createItem(int i) {
		return overlays.get(i);
	}

	@Override
	public int size() {
		return overlays.size();
	}

	public void addOverlayItem(String contact, String label, double latitude, double longitude, float accuracy) {
		if (label == null) {
			label = "";
		}
		String snippet = context.getString(R.string.label_location) + " " + label + "\n" +
				context.getString(R.string.label_latitude) + " "  + latitude + "\n" +
				context.getString(R.string.label_longitude) + " "  + longitude + "\n" +
				context.getString(R.string.label_accuracy) + " " + accuracy;
		GeoPoint geoPoint = new GeoPoint((int)(latitude * 1E6),	(int)(longitude * 1E6));
		AccuracyOverlayItem overlayitem = new AccuracyOverlayItem(geoPoint, contact, snippet, accuracy);
		overlays.add(overlayitem);
		populate();
	}
	
	@Override
	protected boolean onTap(int index) {
	  OverlayItem item = overlays.get(index);
	  if (item != null) {
		  AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		  dialog.setTitle(item.getTitle());
		  dialog.setMessage(item.getSnippet());
		  dialog.show();
	  }
	  return true;
	}
	
	@Override
	public void draw(Canvas canvas, MapView mapView, boolean shadow) {
    	super.draw(canvas, mapView, shadow);

    	for(int i=0; i < overlays.size(); i++) {
	    	AccuracyOverlayItem item = overlays.get(i);
	    	if (item.getAccuracy() != 0) {
		    	Point pt = new Point();
		    	Projection projection = mapView.getProjection();
		        projection.toPixels(item.getPoint(), pt);
		        float circleRadius = projection.metersToEquatorPixels(item.getAccuracy());	        
	            canvas.drawCircle(pt.x, pt.y, circleRadius, paint);
	    	}
	    }
	}

	/**
	 * Overlay item with accuracy info
	 */
	private class AccuracyOverlayItem extends OverlayItem {
	    private float accuracy = 0;

	    public AccuracyOverlayItem(GeoPoint point, String title, String snippet, float accuracy) {
	    	super(point, title, snippet);
	    	
	    	this.accuracy = accuracy;
	    }
	    
	    public float getAccuracy() {
	    	return accuracy;
	    }
	}
}
