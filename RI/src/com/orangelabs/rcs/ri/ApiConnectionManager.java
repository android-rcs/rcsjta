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
package com.orangelabs.rcs.ri;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.gsma.services.rcs.JoynService;
import com.gsma.services.rcs.JoynServiceListener;
import com.gsma.services.rcs.capability.CapabilityService;
import com.gsma.services.rcs.chat.ChatService;
import com.gsma.services.rcs.contacts.ContactsService;
import com.gsma.services.rcs.extension.MultimediaSessionService;
import com.gsma.services.rcs.ft.FileTransferService;
import com.gsma.services.rcs.gsh.GeolocSharingService;
import com.gsma.services.rcs.ipcall.IPCallService;
import com.gsma.services.rcs.ish.ImageSharingService;
import com.gsma.services.rcs.upload.FileUploadService;
import com.gsma.services.rcs.vsh.VideoSharingService;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * A class which manages connection to APIs
 * 
 * @author YPLO6403
 *
 */
public class ApiConnectionManager {

	/**
	 * Singleton of ContactUtils
	 */
	private static volatile ApiConnectionManager instance;

	/**
	 * RCS services
	 *
	 */
	public enum RcsServices {
		Capability, Contacts, Chat, FileTransfer, ImageSharing, VideoSharing, GeolocSharing, FileUpload, IpCall, Multimedia;
	};

	/**
	 * Client connection listener
	 *
	 */
	private class ClientConnectionNotifier {
		/**
		 * The set of monitored services
		 */
		private Set<RcsServices> monitoredServices;
		private Activity activity;
		private LockAccess triggerOnlyOnce;

		/**
		 * Constructor
		 * 
		 * @param activity
		 *            the activity to notify
		 * @param triggerOnlyOnce
		 *            lock access to trigger only once
		 * @param services
		 *            the list of services to monitor
		 */
		public ClientConnectionNotifier(Activity activity, LockAccess triggerOnlyOnce, RcsServices... services) {
			this.activity = activity;
			this.triggerOnlyOnce = triggerOnlyOnce;
			this.monitoredServices = new HashSet<RcsServices>();
			for (RcsServices service : services) {
				this.monitoredServices.add(service);
			}
		}

		public void notifyDisconnection() {
			Utils.showMessageAndExit(activity, activity.getString(R.string.label_api_disabled), triggerOnlyOnce);
		}

		public Set<RcsServices> getMonitoredServices() {
			return monitoredServices;
		}

	}

	/**
	 * Set of connected services
	 */
	final private Set<RcsServices> connectedServices;

	/**
	 * Map of Activity / Client Connection notifier
	 */
	final private Map<Activity, ClientConnectionNotifier> clientsToNotify;

	/**
	 * Map of RCS services and listeners
	 */
	final private Map<RcsServices, JoynService> apis;

	/**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(ApiConnectionManager.class.getSimpleName());

	/**
	 * Get an instance of ApiConnectionManager.
	 * 
	 * @param context
	 *            the context
	 * @return the singleton instance.
	 */
	public static ApiConnectionManager getInstance(Context context) {
		if (instance == null) {
			synchronized (ApiConnectionManager.class) {
				if (instance == null) {
					if (context == null) {
						throw new IllegalArgumentException("Context is null");
					}
					instance = new ApiConnectionManager(context);
				}
			}
		}
		return instance;
	}

	/**
	 * Create a Joyn service listener to monitor connection
	 * 
	 * @param service
	 *            the service to monitor
	 * @return the listerner
	 */
	private JoynServiceListener newJoynServiceListener(final RcsServices service) {
		return new JoynServiceListener() {
			@Override
			public void onServiceDisconnected(int error) {
				if (LogUtils.isActive) {
					Log.d(LOGTAG, service + " Service disconnected");
				}
				connectedServices.remove(service);
				notifyDisconnection(service, error);
			}

			@Override
			public void onServiceConnected() {
				if (LogUtils.isActive) {
					Log.d(LOGTAG, service + " Service connected");
				}
				connectedServices.add(service);
			}
		};

	}

	/**
	 * Constructor
	 * 
	 * @param context
	 */
	private ApiConnectionManager(Context context) {
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "ApiConnectionManager");
		}
		// Construct list of connected services
		connectedServices = new HashSet<RcsServices>();
		// Construct list of clients to notify
		clientsToNotify = new HashMap<Activity, ClientConnectionNotifier>();
		// Construct list of APIs
		apis = new HashMap<RcsServices, JoynService>();
		// Instantiate APIs
		apis.put(RcsServices.Capability, new CapabilityService(context, newJoynServiceListener(RcsServices.Capability)));
		apis.put(RcsServices.Chat, new ChatService(context, newJoynServiceListener(RcsServices.Chat)));
		apis.put(RcsServices.Contacts, new ContactsService(context, newJoynServiceListener(RcsServices.Contacts)));
		apis.put(RcsServices.FileTransfer, new FileTransferService(context, newJoynServiceListener(RcsServices.FileTransfer)));
		apis.put(RcsServices.ImageSharing, new ImageSharingService(context, newJoynServiceListener(RcsServices.ImageSharing)));
		apis.put(RcsServices.VideoSharing, new VideoSharingService(context, newJoynServiceListener(RcsServices.VideoSharing)));
		apis.put(RcsServices.FileUpload, new FileUploadService(context, newJoynServiceListener(RcsServices.FileUpload)));
		apis.put(RcsServices.GeolocSharing, new GeolocSharingService(context, newJoynServiceListener(RcsServices.GeolocSharing)));
		apis.put(RcsServices.IpCall, new IPCallService(context, newJoynServiceListener(RcsServices.IpCall)));
		apis.put(RcsServices.Multimedia, new MultimediaSessionService(context, newJoynServiceListener(RcsServices.Multimedia)));
		// Connect APIs
		connectApis();
	}

	/* package private */void connectApis() {
		// Connect all disconnected APIs
		for (RcsServices service : apis.keySet()) {
			if (!isServiceConnected(service)) {
				if (LogUtils.isActive) {
					Log.d(LOGTAG, "Connect API " + service);
				}
				apis.get(service).connect();
			}
		}
	}

	/**
	 * Notify API disconnection to client
	 * 
	 * @param service
	 *            the disconnected service
	 * @param error
	 *            the error
	 */
	private void notifyDisconnection(RcsServices service, int error) {
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "notifyDisConnection error=" + error);
		}
		for (ClientConnectionNotifier clienttoNotify : clientsToNotify.values()) {
			if (clienttoNotify.getMonitoredServices().contains(service)) {
				clienttoNotify.notifyDisconnection();
			}
		}
	}

	/**
	 * Check if services are connected
	 * 
	 * @param services
	 *            list of services
	 * @return true if all services of the list are connected
	 */
	public boolean isServiceConnected(RcsServices... services) {
		for (RcsServices service : services) {
			if (!connectedServices.contains(service)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Start monitoring the service
	 * 
	 * @param activity
	 *            the activity requesting to start monitoring the service
	 * @param exitOnce
	 *            a locker
	 * @param services
	 *            the list of services to monitor
	 */
	public void startMonitorServices(Activity activity, LockAccess exitOnce, RcsServices... services) {
		clientsToNotify.put(activity, new ClientConnectionNotifier(activity, exitOnce, services));
	}

	/**
	 * Stop monitoring the service
	 * 
	 * @param activity
	 *            the activity requesting to stop monitoring the service
	 */
	public void stopMonitorServices(Activity activity) {
		clientsToNotify.remove(activity);
	}

	public CapabilityService getCapabilityApi() {
		return (CapabilityService) apis.get(RcsServices.Capability);
	}

	public ChatService getChatApi() {
		return (ChatService) apis.get(RcsServices.Chat);
	}

	public ContactsService getContactsApi() {
		return (ContactsService) apis.get(RcsServices.Contacts);
	}

	public FileTransferService getFileTransferApi() {
		return (FileTransferService) apis.get(RcsServices.FileTransfer);
	}

	public VideoSharingService getVideoSharingApi() {
		return (VideoSharingService) apis.get(RcsServices.VideoSharing);
	}

	public ImageSharingService getImageSharingApi() {
		return (ImageSharingService) apis.get(RcsServices.ImageSharing);
	}

	public GeolocSharingService getGeolocSharingApi() {
		return (GeolocSharingService) apis.get(RcsServices.GeolocSharing);
	}

	public FileUploadService getFileUploadApi() {
		return (FileUploadService) apis.get(RcsServices.FileUpload);
	}

	public IPCallService getIPCallApi() {
		return (IPCallService) apis.get(RcsServices.IpCall);
	}
	
	public MultimediaSessionService getMultimediaSessionApi() {
		return (MultimediaSessionService) apis.get(RcsServices.Multimedia);
	}
}
