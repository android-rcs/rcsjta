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

import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog.Message;
import com.gsma.services.rcs.contact.ContactId;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.RcsContactUtil;
import com.orangelabs.rcs.ri.utils.Utils;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Display geoloc on a Google map v2
 *
 * @author yplo6403
 */
public class DisplayGeoloc extends FragmentActivity implements OnMapReadyCallback {
    /**
     * Intent parameters: geolocation
     */
    public final static String EXTRA_GEOLOC = "geoloc";

    private HashMap<String, Geoloc> mMapContactGeoloc;

    private static BitmapDescriptor sMarkerIcon;

    private static final int LARGE_ZOOM = 12;
    private static final int SMALL_ZOOM = 4;

    private final static String QUERY_SORT_ORDER = Message.TIMESTAMP + " DESC";

    private final static String QUERY_WHERE_CLAUSE = Message.MIME_TYPE + "='"
            + Message.MimeType.GEOLOC_MESSAGE + "' AND " + Message.DIRECTION + " = "
            + Direction.OUTGOING.toInt();

    private final static String[] QUERY_PROJECTION = new String[] {
        Message.CONTENT
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.geoloc_display);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        /* Get geoloc value from intent */
        mMapContactGeoloc = (HashMap<String, Geoloc>) getIntent()
                .getSerializableExtra(EXTRA_GEOLOC);
        if (sMarkerIcon == null) {
            sMarkerIcon = BitmapDescriptorFactory.fromResource(R.drawable.ri_map_icon);
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        boolean singleEntry = (mMapContactGeoloc.size() == 1);
        LatLng latLng = null;
        LatLng myPosition = null;
        for (Entry<String, Geoloc> entry : mMapContactGeoloc.entrySet()) {
            Geoloc geoloc = entry.getValue();
            latLng = new LatLng(geoloc.getLatitude(), geoloc.getLongitude());
            String contact = entry.getKey();
            MarkerOptions options = new MarkerOptions().position(latLng).title(contact)
                    .icon(sMarkerIcon);
            if (contact.equals(getString(R.string.label_me))) {
                myPosition = latLng;
            }
            Marker marker = map.addMarker(options);
            if (singleEntry) {
                marker.showInfoWindow();
            }
        }
        LatLng target = latLng;
        int zoom = SMALL_ZOOM;
        if (singleEntry && latLng != null) {
            zoom = LARGE_ZOOM;
        } else if (myPosition != null) {
            target = myPosition;
        }
        CameraPosition cameraPosition = new CameraPosition.Builder().target(target).zoom(zoom)
                .build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private static Geoloc getLastGeoloc(Context ctx, ContactId contact) {
        Cursor cursor = null;
        String where = Message.CONTACT + "='" + contact.toString() + "' AND " + Message.MIME_TYPE
                + "='" + Message.MimeType.GEOLOC_MESSAGE + "' AND " + Message.DIRECTION + " = "
                + Direction.INCOMING.toInt();
        try {
            // TODO CR025 Geoloc sharing provider
            cursor = ctx.getContentResolver().query(Message.CONTENT_URI, QUERY_PROJECTION, where,
                    null, QUERY_SORT_ORDER);
            if (!cursor.moveToNext()) {
                return null;
            }
            String content = cursor.getString(cursor.getColumnIndexOrThrow(Message.CONTENT));
            return new Geoloc(content);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static Geoloc getMyLastGeoloc(Context ctx) {
        Cursor cursor = null;
        try {
            // TODO CR025 Geoloc sharing provider
            cursor = ctx.getContentResolver().query(Message.CONTENT_URI, QUERY_PROJECTION,
                    QUERY_WHERE_CLAUSE, null, QUERY_SORT_ORDER);
            if (!cursor.moveToNext()) {
                return null;
            }
            String content = cursor.getString(cursor.getColumnIndexOrThrow(Message.CONTENT));
            return new Geoloc(content);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Show geolocation for a set of contacts
     * 
     * @param ctx context
     * @param contacts set of contacts
     */
    public static void showContactsOnMap(Context ctx, Set<ContactId> contacts) {
        HashMap<String, Geoloc> mapContactGeoloc = new HashMap<>();
        RcsContactUtil rcsContactUtil = RcsContactUtil.getInstance(ctx);
        for (ContactId contact : contacts) {
            Geoloc geoloc = getLastGeoloc(ctx, contact);
            if (geoloc != null) {
                mapContactGeoloc.put(rcsContactUtil.getDisplayName(contact), geoloc);
            }
        }
        Geoloc myGeoloc = getMyLastGeoloc(ctx);
        if (myGeoloc != null) {
            mapContactGeoloc.put(ctx.getString(R.string.label_me), myGeoloc);
        }
        if (mapContactGeoloc.isEmpty()) {
            Utils.displayLongToast(ctx, ctx.getString(R.string.label_geoloc_not_found));
            return;
        }
        Intent intent = new Intent(ctx, DisplayGeoloc.class);
        intent.putExtra(DisplayGeoloc.EXTRA_GEOLOC, mapContactGeoloc);
        ctx.startActivity(intent);
    }

    /**
     * Show geolocation for a one contact
     * 
     * @param ctx context
     * @param contact The contact
     */
    public static void showContactOnMap(Context ctx, ContactId contact) {
        Set<ContactId> set = new HashSet<>();
        set.add(contact);
        showContactsOnMap(ctx, set);
    }

    /**
     * Show geolocation for a contact
     * 
     * @param ctx The context
     * @param contact The contact
     * @param geoloc The geolocation
     */
    public static void showContactOnMap(Context ctx, ContactId contact, Geoloc geoloc) {
        HashMap<String, Geoloc> mapContactGeoloc = new HashMap<>();
        String displayName = RcsContactUtil.getInstance(ctx).getDisplayName(contact);
        mapContactGeoloc.put(displayName, geoloc);
        Intent intent = new Intent(ctx, DisplayGeoloc.class);
        intent.putExtra(DisplayGeoloc.EXTRA_GEOLOC, mapContactGeoloc);
        ctx.startActivity(intent);
    }

}
