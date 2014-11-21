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
package com.orangelabs.rcs.ri.sharing.geoloc;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.gsma.services.rcs.capability.CapabilitiesLog;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.messaging.geoloc.ShowUsInMap;

/**
 * Geoloc sharing API
 *
 * @author Jean-Marc AUFFRET
 */
public class TestGeolocSharingApi extends ListActivity {
	
	private static final String[] PROJECTION = new String[] { CapabilitiesLog.CONTACT };
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Set items
        String[] items = {
    		getString(R.string.menu_initiate_geoloc_sharing),
    		getString(R.string.menu_showus_map)
        };
        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items));
    }

    @Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		switch (position) {
		case 0:
			startActivity(new Intent(this, InitiateGeolocSharing.class));
			break;

		case 1:
			ArrayList<String> list = new ArrayList<String>();
			Cursor cursor = null;
			try {
				cursor = getContentResolver().query(CapabilitiesLog.CONTENT_URI, PROJECTION, null, null, null);
				while (cursor.moveToNext()) {
					String contact = cursor.getString(cursor.getColumnIndex(CapabilitiesLog.CONTACT));
					list.add(contact);
				}
				ShowUsInMap.startShowUsInMap(this, list);
			} catch (Exception e) {
				// Skip intentionally
			} finally {
				if (cursor != null) {
					cursor.close();
				}
			}
			break;
		}
	}
}
