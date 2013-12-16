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
package com.orangelabs.rcs.ri.capabilities;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Data;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.gsma.services.rcs.capability.CapabilitiesLog;
import com.gsma.services.rcs.capability.CapabilityService;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Extensions capabilities
 * 
 * @author Jean-Marc AUFFRET
 */
public class ExtensionsCapabilities extends ListActivity implements OnItemClickListener {

	/**
	 * Contact
	 */
	private String contact = null;

    /**
     * Layout inflater
     */
    private LayoutInflater inflater;
    
    /**
     * List adapter
     */
    private ActivityListAdapter adapter;
    
    /**
     * List of activity info
     */
    private List<ResolveInfo> resolveInfos = new ArrayList<ResolveInfo>();

    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set layout
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		inflater=getLayoutInflater();
		adapter=new ActivityListAdapter();
		setListAdapter(adapter);
		getListView().setOnItemClickListener(this);
		getListView().setOnCreateContextMenuListener(this);

		try {
			// Get the corresponding contact from the intent
	        Uri contactUri = getIntent().getData();
	        Cursor cursor = managedQuery(contactUri, null, null, null, null);
	        if (cursor.moveToNext()) {
	        	contact = cursor.getString(cursor.getColumnIndex(Data.DATA1));
	        }
	        cursor.close();
	        
			// Get extensions supported by the contact
	        String[] projection = new String[] {
	        	CapabilitiesLog.CAPABILITY_EXTENSIONS
	    		};
	        String where = CapabilitiesLog.CONTACT_NUMBER + "='" + contact + "'";
			cursor = getContentResolver().query(CapabilitiesLog.CONTENT_URI, projection, where, null, null);
			String exts = null;
			if (cursor != null) {
				if (cursor.getCount() > 0) {
					cursor.moveToFirst();
					exts = cursor.getString(0);
				}
				cursor.close();
			}    	
    	
			// Build list of applications supporting the extensions
			PackageManager packageManager = getApplicationContext().getPackageManager();
			String[] extensions = exts.split(";");
			for(int i=0; i < extensions.length; i++) {
				// Intent query on current installed extension
				Intent intent = new Intent(CapabilityService.INTENT_EXTENSIONS);
				String mime = ExtensionsCapabilities.formatIntentMimeType(extensions[i]);
				intent.setType(mime);
				List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER);
				for(int j=0; j < list.size(); j++) {
					resolveInfos.add(list.get(j));
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
	    	Utils.showMessageAndExit(this, getString(R.string.label_extensions_not_found));
		}
    }
    	
    /**
     * Activity list
     */
	public class ActivityListAdapter extends BaseAdapter{
		@Override
		public int getCount() {
			return resolveInfos.size();
		}
		
		@Override
		public Object getItem(int position) {
			return resolveInfos.get(position);
		}
	
		@Override
		public long getItemId(int position) {
			return position;
		}
	
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null){
				convertView=inflater.inflate(R.layout.utils_list_item, null);
			}
			ResolveInfoViewHolder resolveInfoHolder = (ResolveInfoViewHolder) convertView.getTag();
			if (resolveInfoHolder == null){
				resolveInfoHolder = new ResolveInfoViewHolder();
				resolveInfoHolder.initViews(convertView);
			} 
			ResolveInfo resolveInfo = resolveInfos.get((int) getItemId(position));
			resolveInfoHolder.update(resolveInfo);
			convertView.setTag(resolveInfoHolder);
			return convertView;
		}
	
		private class ResolveInfoViewHolder{
			public ImageView icon;
			public TextView name;
		
			public void initViews(View root){
				icon = (ImageView)root.findViewById(R.id.icon);
				name = (TextView)root.findViewById(R.id.name);
			}
			
			public void update(ResolveInfo resolveInfo){
				name.setText(resolveInfo.loadLabel(getPackageManager()));
				
				icon.setImageDrawable(resolveInfo.loadIcon(getPackageManager()));
			}
		}

	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		ResolveInfo resolveInfo = ((ResolveInfo)adapter.getItem(position));
		Intent intent = new Intent();
		intent.setClassName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
		intent.putExtra("contact", contact);
		startActivity(intent);
		finish();
	}

	/**
	 * Format intent MIME type
	 * 
	 * @param tag Feature tag
	 * @return Intent MIME type or null
	 */
	private static String formatIntentMimeType(String tag) {
		return CapabilityService.EXTENSION_MIME_TYPE + "/" + tag;
	}
}
