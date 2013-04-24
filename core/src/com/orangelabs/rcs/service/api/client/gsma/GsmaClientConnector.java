package com.orangelabs.rcs.service.api.client.gsma;

import java.util.List;
import java.util.Vector;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

/**
 * GSMA client connector based on GSMA Implementation guidelines 3.0
 * 
 * @author jexa7410
 */
public class GsmaClientConnector {
	/**
	 * GSMA client registry name
	 */
	public static final String GSMA_PREFS_NAME = "gsma.joyn.preferences";
	
	/**
	 * GSMA client tag
	 */
	public static final String GSMA_CLIENT = "gsma.joyn.client";
	
	/**
	 * GSMA client enabled tag
	 */
	public static final String GSMA_CLIENT_ENABLED = "gsma.joyn.enabled";
	
	/**
     * Is device RCS compliant
     * 
     * @param ctx Context
     * @return Boolean
     */
    public static boolean isDeviceRcsCompliant(Context ctx) {
    	try {
    		String me = ctx.getApplicationInfo().packageName;
    	    List<ApplicationInfo> apps = ctx.getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
    	    for(int i=0; i < apps.size(); i++) {
    	    	ApplicationInfo info = apps.get(i);
    	        if (info.metaData != null) {
    	        	if (info.metaData.getBoolean(GsmaClientConnector.GSMA_CLIENT, false) && !info.packageName.equals(me)) {
    	        		return true;
    	        	}
    	        }
    	    }
    		return false;
    	} catch(Exception e) {
    		return false;
    	}
    }
    
	/**
     * Returns list of installed RCS clients
     * 
     * @param ctx Context
     * @return List of clients
     */
    public static Vector<ApplicationInfo> getRcsClients(Context ctx) {
    	Vector<ApplicationInfo> result = new Vector<ApplicationInfo>();
    	try {
    		String me = ctx.getApplicationInfo().packageName;
    	    List<ApplicationInfo> apps = ctx.getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
    	    for(int i=0; i < apps.size(); i++) {
    	    	ApplicationInfo info = apps.get(i);
    	        if (info.metaData != null) {
    	        	if (info.metaData.getBoolean(GsmaClientConnector.GSMA_CLIENT, false) && !info.packageName.equals(me)) {
    	        		result.add(info);
    	        	}
    	        }
    	    }
    	} catch(Exception e) {
    	}
    	return result;
    }
    
    /**
     * Is RCS client activated
     * 
     * @param ctx Context
     * @param packageName Client package name
     * @return Boolean
     */
    public static boolean isRcsClientActivated(Context ctx, String packageName) {
		try {
			Context appContext = ctx.createPackageContext(packageName, Context.MODE_WORLD_WRITEABLE);
			if (appContext == null) {
				return false;
			}
			
			SharedPreferences prefs = appContext.getSharedPreferences(GsmaClientConnector.GSMA_PREFS_NAME, Context.MODE_WORLD_READABLE);
			if (prefs != null) {
				return prefs.getBoolean(GsmaClientConnector.GSMA_CLIENT_ENABLED, false);
			} else {
				return false;
			}			
		} catch(Exception e) {
			return false;
		}
    }    

    /**
     * Get the RCS settings intent
     * 
     * @param ctx Context
     * @param packageName Client package name
     * @return Intent or null
     */
    public static Intent getRcsSettingsActivityIntent(Context ctx, String packageName) {
    	try {
    	    List<ApplicationInfo> apps = ctx.getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA);
    	    for(int i=0; i < apps.size(); i++) {
    	    	ApplicationInfo info = apps.get(i);
    	        if ((info.metaData != null) && (info.packageName.equals(packageName))) {
    	        	String activity = info.metaData.getString("gsma.joyn.settings.activity");
    	        	if (activity != null) {
    	        		return new Intent(activity);
    	        	}
    	        }
    	    }
    		return null;
    	} catch(Exception e) {
    		return null;
    	}
    }
}
