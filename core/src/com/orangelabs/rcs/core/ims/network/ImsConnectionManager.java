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

package com.orangelabs.rcs.core.ims.network;

import java.util.Random;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.telephony.TelephonyManager;

import com.orangelabs.rcs.core.CoreException;
import com.orangelabs.rcs.core.ims.ImsModule;
import com.orangelabs.rcs.core.ims.network.ImsNetworkInterface.DnsResolvedFields;
import com.orangelabs.rcs.platform.AndroidFactory;
import com.orangelabs.rcs.platform.network.NetworkFactory;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provider.settings.RcsSettingsData.NetworkAccessType;
import com.orangelabs.rcs.service.LauncherUtils;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * IMS connection manager
 *
 * @author JM. Auffret
 * @author Deutsche Telekom
 */
public class ImsConnectionManager implements Runnable {
	/**
     * IMS module
     */
    private ImsModule imsModule;
    
    /**
     * Network interfaces
     */
    private ImsNetworkInterface[] networkInterfaces = new ImsNetworkInterface[2];

    /**
     * IMS network interface
     */
    private ImsNetworkInterface currentNetworkInterface;
    
    /**
     * IMS polling thread
     */
    private Thread imsPollingThread = null;

    /**
     * IMS polling thread Id
     */
    private long imsPollingThreadID = -1;

    /**
     * Connectivity manager
     */
	private ConnectivityManager connectivityMgr;
	
	/**
	 * Network access type
	 */
	private NetworkAccessType network;

	/**
	 * Operator
	 */
	private String operator;

	/**
	 * APN
	 */
	private String apn;

	/**
	 * DNS resolved fields
	 */
	private DnsResolvedFields mDnsResolvedFields = null;
	
    /**
     * Battery level state
     */
    private boolean disconnectedByBattery = false;

    /**
     * IMS services already started
     */
    private boolean imsServicesStarted = false;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
	 * Constructor
	 * 
	 * @param imsModule IMS module
	 * @throws CoreException
	 */
	public ImsConnectionManager(ImsModule imsModule) throws CoreException {
		this.imsModule = imsModule;

		RcsSettings rcsSettings = RcsSettings.getInstance();
		// Get network access parameters
		network = rcsSettings.getNetworkAccess();

		// Get network operator parameters
		operator = rcsSettings.getNetworkOperator();
		apn = rcsSettings.getNetworkApn();
		
		// Set the connectivity manager
		connectivityMgr = (ConnectivityManager)AndroidFactory.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
		
        // Instantiates the IMS network interfaces
        networkInterfaces[0] = new MobileNetworkInterface(imsModule, rcsSettings);
        networkInterfaces[1] = new WifiNetworkInterface(imsModule, rcsSettings);

        // Set the mobile network interface by default
		currentNetworkInterface = getMobileNetworkInterface();

		// Load the user profile
		loadUserProfile();
		
		// Register network state listener
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		AndroidFactory.getApplicationContext().registerReceiver(networkStateListener, intentFilter);

        // Battery management
        AndroidFactory.getApplicationContext().registerReceiver(batteryLevelListener, new IntentFilter(Intent.ACTION_BATTERY_CHANGED)); 
	}
	
	/**
     * Returns the current network interface
     * 
     * @return Current network interface
     */
	public ImsNetworkInterface getCurrentNetworkInterface() {
		return currentNetworkInterface;
	}

	/**
     * Returns the mobile network interface
     * 
     * @return Mobile network interface
     */
	public ImsNetworkInterface getMobileNetworkInterface() {
		return networkInterfaces[0];
	}
	
	/**
     * Returns the Wi-Fi network interface
     * 
     * @return Wi-Fi network interface
     */
	public ImsNetworkInterface getWifiNetworkInterface() {
		return networkInterfaces[1];
	}
	
	/**
     * Is connected to Wi-Fi
     * 
     * @return Boolean
     */
	public boolean isConnectedToWifi() {
		if (currentNetworkInterface == getWifiNetworkInterface()) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
     * Is connected to mobile
     * 
     * @return Boolean
     */
	public boolean isConnectedToMobile() {
		if (currentNetworkInterface == getMobileNetworkInterface()) {
			return true;
		} else {
			return false;
		}
	}	

    /**
     * Is disconnected by battery
     *
     * @return Returns true if disconnected by battery, else returns false
     */
    public boolean isDisconnectedByBattery() {
        return disconnectedByBattery;
    }	
	
	/**
	 * Load the user profile associated to the network interface
	 */
	private void loadUserProfile() {
    	ImsModule.IMS_USER_PROFILE = currentNetworkInterface.getUserProfile();
		if (logger.isActivated()) {
			logger.debug("User profile has been reloaded");
		}
	}
	
	/**
     * Terminate the connection manager
     */
    public void terminate() {
    	if (logger.isActivated()) {
    		logger.info("Terminate the IMS connection manager");
    	}

        // Unregister battery listener
        try {
            AndroidFactory.getApplicationContext().unregisterReceiver(batteryLevelListener);
        } catch (IllegalArgumentException e) {
            // Nothing to do
        }

        // Unregister network state listener
    	try {
    		AndroidFactory.getApplicationContext().unregisterReceiver(networkStateListener);
        } catch (IllegalArgumentException e) {
        	// Nothing to do
        }

    	// Stop the IMS connection manager
    	stopImsConnection();
    	
    	// Unregister from the IMS
		currentNetworkInterface.unregister();
		    	
    	if (logger.isActivated()) {
    		logger.info("IMS connection manager has been terminated");
    	}
    }

    /**
     * Network state listener
     */
	private BroadcastReceiver networkStateListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
        	Thread t = new Thread() {
        		public void run() {
        			connectionEvent(intent);
        		}
        	};
        	t.start();
        }
    };    

    /**
     * Connection event
     * 
     * @param intent Intent
     */
    private synchronized void connectionEvent(Intent intent) {
    	if (disconnectedByBattery) {
    		return;
    	}
    	
		if (!intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
			return;
		}
		
		boolean connectivity = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
        String reason = intent.getStringExtra(ConnectivityManager.EXTRA_REASON);
        boolean failover = intent.getBooleanExtra(ConnectivityManager.EXTRA_IS_FAILOVER, false);
		if (logger.isActivated()) {
			logger.debug("Connectivity event change: failover=" + failover + ", connectivity=" + !connectivity + ", reason=" + reason);
		}

		// Check received network info
    	NetworkInfo networkInfo = connectivityMgr.getActiveNetworkInfo();
		if (networkInfo == null) {
			// Disconnect from IMS network interface
			if (logger.isActivated()) {
				logger.debug("Disconnect from IMS: no network (e.g. air plane mode)");
			}
			disconnectFromIms();
			return;
		}

        // Check if SIM account has changed (i.e. hot SIM swap)
		if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
	        String lastUserAccount = LauncherUtils.getLastUserAccount(AndroidFactory.getApplicationContext());
	        String currentUserAccount = LauncherUtils.getCurrentUserAccount(AndroidFactory.getApplicationContext());
	        if (lastUserAccount != null) {
	            if ((currentUserAccount == null) || !currentUserAccount.equalsIgnoreCase(lastUserAccount)) {
	                imsModule.getCoreListener().handleSimHasChanged();
	                return;
	            }
	        }
		}
		
		// Get the current local IP address
		String localIpAddr = null;

		// Check if the network access type has changed 
		if (networkInfo.getType() != currentNetworkInterface.getType()) {
			// Network interface changed
			if (logger.isActivated()) {
				logger.info("Data connection state: NETWORK ACCESS CHANGED");
			}

			// Disconnect from current IMS network interface
			if (logger.isActivated()) {
				logger.debug("Disconnect from IMS: network access has changed");
			}
			disconnectFromIms();

			// Change current network interface
			if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
				if (logger.isActivated()) {
					logger.debug("Change the network interface to mobile");
				}
				currentNetworkInterface = getMobileNetworkInterface();
			} else
			if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
				if (logger.isActivated()) {
					logger.debug("Change the network interface to Wi-Fi");
				}
				currentNetworkInterface = getWifiNetworkInterface();
			}				

			// Load the user profile for the new network interface
			loadUserProfile();
			
			// update DNS entry
						try {
							mDnsResolvedFields = currentNetworkInterface.getDnsResolvedFields();
						} catch (Exception e) {
							if (logger.isActivated()) {
								logger.error(
									"Resolving remote IP address to figure out initial local IP address failed!", e);
							}
						}
						
						// get latest local IP address
						localIpAddr = NetworkFactory.getFactory().getLocalIpAddress(mDnsResolvedFields, networkInfo.getType());

		} else {
			// Check if the IP address has changed
			try {
				if (mDnsResolvedFields == null) {
					mDnsResolvedFields = currentNetworkInterface.getDnsResolvedFields();
				}
			} catch (Exception e) {
				if (logger.isActivated()) {
					logger.error("Resolving remote IP address to figure out initial local IP address failed!", e);
				}
			}
			localIpAddr = NetworkFactory.getFactory().getLocalIpAddress(mDnsResolvedFields, networkInfo.getType());
			
			if (localIpAddr != null) {
			    String lastIpAddr = currentNetworkInterface.getNetworkAccess().getIpAddress();
				if (!localIpAddr.equals(lastIpAddr)) {
		            // Changed by Deutsche Telekom
				    if (lastIpAddr != null) {
    					// Disconnect from current IMS network interface
    					if (logger.isActivated()) {
    						logger.debug("Disconnect from IMS: IP address has changed");
    					}
    					disconnectFromIms();
				    } else {
                        if (logger.isActivated()) {
                            logger.debug("IP address available (again)");
                        }				            
				    }
				} else {
					// Changed by Deutsche Telekom
					if (logger.isActivated()) {
						logger.debug("Neither interface nor IP address has changed; nothing to do.");
					}
					return;
				}
			}
		}
		
		// Check if there is an IP connectivity
		if (networkInfo.isConnected() && (localIpAddr != null)) {
			String remoteAddress;
			if (mDnsResolvedFields != null) {
				remoteAddress = mDnsResolvedFields.ipAddress;
			} else {
				remoteAddress = new String("unresolved");
			}

			if (logger.isActivated()) {
				logger.info("Data connection state: CONNECTED to " + networkInfo.getTypeName() + " with local IP " + localIpAddr + " valid for " + remoteAddress);
			}

			// Test network access type
			if (!NetworkAccessType.ANY.equals(network) && (network.toInt() != networkInfo.getType())) {
				if (logger.isActivated()) {
					logger.warn("Network access " + networkInfo.getTypeName() + " is not authorized");
				}
				return;
			}

			// Test the operator id
			TelephonyManager tm = (TelephonyManager)AndroidFactory.getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
			String currentOpe = tm.getSimOperatorName();
			if ((operator.length() > 0) && !currentOpe.equalsIgnoreCase(operator)) {
				if (logger.isActivated()) {
					logger.warn("Operator not authorized");
				}
				return;
			}

            if (Build.VERSION.SDK_INT < 17) { // From Android 4.2, the management of APN is only for system app 
				// Test the default APN configuration if mobile network
				if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
					ContentResolver cr = AndroidFactory.getApplicationContext().getContentResolver();
					String currentApn = null;
					Cursor c = cr.query(Uri.parse("content://telephony/carriers/preferapn"),
							new String[] { "apn" }, null, null, null);
					if (c != null) {
						final int apnIndex = c.getColumnIndexOrThrow("apn");
						if (c.moveToFirst()) {
							currentApn = c.getString(apnIndex);
						}
						c.close();
					}
					if ((apn.length() > 0) && !apn.equalsIgnoreCase(currentApn)) {
						if (logger.isActivated()) {
							logger.warn("APN not authorized");
						}
						return;
					}
				}
            }

			// Test the configuration
			if (!currentNetworkInterface.isInterfaceConfigured()) {
				if (logger.isActivated()) {
					logger.warn("IMS network interface not well configured");
				}
				return;
			}

			// Connect to IMS network interface
			if (logger.isActivated()) {
				logger.debug("Connect to IMS");
			}
			connectToIms(localIpAddr);
		} else {
			if (logger.isActivated()) {
				logger.info("Data connection state: DISCONNECTED from " + networkInfo.getTypeName());
			}

			// Disconnect from IMS network interface
			if (logger.isActivated()) {
				logger.debug("Disconnect from IMS: IP connection lost");
			}
			disconnectFromIms();
    	}
    }    
    
    /**
     * Connect to IMS network interface
     * 
     * @param ipAddr IP address
     */
    private void connectToIms(String ipAddr) {
    	// Connected to the network access
		currentNetworkInterface.getNetworkAccess().connect(ipAddr);

		// Start the IMS connection
		startImsConnection();
    }
    
    /**
     * Disconnect from IMS network interface
     */
    private void disconnectFromIms() {
		// Stop the IMS connection
		stopImsConnection();

		// Registration terminated 
		currentNetworkInterface.registrationTerminated();
		
		// Disconnect from the network access
		currentNetworkInterface.getNetworkAccess().disconnect();
    }
    
	/**
	 * Start the IMS connection
	 */
	private synchronized void startImsConnection() {
		if (imsPollingThreadID >= 0) {
			// Already connected
			return;
		}
		
		// Set the connection flag
    	if (logger.isActivated()) {
    		logger.info("Start the IMS connection manager");
    	}

    	// Start background polling thread
		try {
			imsPollingThread = new Thread(this);
            imsPollingThreadID = imsPollingThread.getId();
			imsPollingThread.start();
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Internal exception while starting IMS polling thread", e);
			}
		}
	}
	
	/**
	 * Stop the IMS connection
	 */
	private synchronized void stopImsConnection() {
		if (imsPollingThreadID == -1) {
			// Already disconnected
			return;
		}

		// Set the connection flag
		if (logger.isActivated()) {
    		logger.info("Stop the IMS connection manager");
    	}
        imsPollingThreadID = -1;

    	// Stop background polling thread
		try {
			imsPollingThread.interrupt();
			imsPollingThread = null;
		} catch(Exception e) {
			if (logger.isActivated()) {
				logger.error("Internal exception while stopping IMS polling thread", e);
			}
		}
		
		// Stop IMS services
        if (imsServicesStarted) {
            imsModule.stopImsServices();
            imsServicesStarted = false;
        }
	}

	/**
	 * Background processing
	 */
	public void run() {
    	if (logger.isActivated()) {
    		logger.debug("Start polling of the IMS connection");
    	}
    	
		int servicePollingPeriod = RcsSettings.getInstance().getImsServicePollingPeriod();
		int regBaseTime = RcsSettings.getInstance().getRegisterRetryBaseTime();
		int regMaxTime = RcsSettings.getInstance().getRegisterRetryMaxTime();
		Random random = new Random();
		int nbFailures = 0;

        while (imsPollingThreadID == Thread.currentThread().getId()) {
	    	if (logger.isActivated()) {
	    		logger.debug("Polling: check IMS connection");
	    	}

	    	// Connection management
    		try {
    	    	// Test IMS registration
    			if (!currentNetworkInterface.isRegistered()) {
    				if (logger.isActivated()) {
    					logger.debug("Not yet registered to IMS: try registration");
    				}

    				// Try to register to IMS
    	    		if (currentNetworkInterface.register(mDnsResolvedFields)) {
                        // InterruptedException thrown by stopImsConnection() may be caught by one
                        // of the methods used in currentNetworkInterface.register() above
                        if (imsPollingThreadID != Thread.currentThread().getId()) {
                        	if (logger.isActivated()) {
                        		logger.debug("IMS connection polling thread race condition");
                        	}
                            break;
                        } else {
                            if (logger.isActivated()) {
                                logger.debug("Registered to the IMS with success: start IMS services");
                            }
                            if (imsModule.isReady() && !imsServicesStarted) {
                                imsModule.startImsServices();
                                imsServicesStarted = true;
                            }

                            // Reset number of failures
                            nbFailures = 0;
                        }
    	    		} else {
    	            	if (logger.isActivated()) {
    	            		logger.debug("Can't register to the IMS");
    	            	}
    	            	
    	            	// Increment number of failures
    	            	nbFailures++;
    	            	
    	            	// Force to perform a new DNS lookup 
    	            	mDnsResolvedFields = null;
    	    		}
    			} else {
                    if (imsModule.isReady()) {
                        if (!imsServicesStarted) {
                            if (logger.isActivated()) {
                                logger.debug("Already registered to IMS: start IMS services");
                            }
                            imsModule.startImsServices();
                            imsServicesStarted = true;
                        } else {
                            if (logger.isActivated()) {
                                logger.debug("Already registered to IMS: check IMS services");
                            }
                            imsModule.checkImsServices();
                        }
                    } else {
                        if (logger.isActivated()) {
                            logger.debug("Already registered to IMS: IMS services not yet started");
                        }
                    }
                }
            } catch (Exception e) {
				if (logger.isActivated()) {
		    		logger.error("Internal exception", e);
		    	}
    	        // Force to perform a new DNS lookup 
    	        mDnsResolvedFields = null;
			}

            // InterruptedException thrown by stopImsConnection() may be caught by one
            // of the methods used in currentNetworkInterface.register() above
            if (imsPollingThreadID != Thread.currentThread().getId()) {
                logger.debug("IMS connection polling thread race condition");
                break;
            }

			// Make a pause before the next polling
	    	try {
    			if (!currentNetworkInterface.isRegistered()) {
    				// Pause before the next register attempt
    				double w = Math.min(regMaxTime, (regBaseTime * Math.pow(2, nbFailures)));
    				double coeff = (random.nextInt(51) + 50) / 100.0; // Coeff between 50% and 100%
    				int retryPeriod = (int)(coeff * w);
    	        	if (logger.isActivated()) {
    	        		logger.debug("Wait " + retryPeriod + "s before retry registration (failures=" + nbFailures + ", coeff="+ coeff + ")");
    	        	}
    				Thread.sleep(retryPeriod * 1000);
                } else if (!imsServicesStarted) {
                    int retryPeriod = 5;
                    if (logger.isActivated()) {
                        logger.debug("Wait " + retryPeriod + "s before retry to start services");
                    }
                    Thread.sleep(retryPeriod * 1000);
	    		} else {
    				// Pause before the next service check
	    			Thread.sleep(servicePollingPeriod * 1000);
	    		}
            } catch(InterruptedException e) {
                break;
            }
		}

		if (logger.isActivated()) {
    		logger.debug("IMS connection polling is terminated");
    	}
	}

    /**
     * Battery level listener
     */
    private BroadcastReceiver batteryLevelListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int batteryLimit = RcsSettings.getInstance().getMinBatteryLevel();
            if (batteryLimit > 0) {
                int batteryLevel = intent.getIntExtra("level", 0);
                int batteryPlugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 1);
                if (logger.isActivated()) {
                    logger.info("Battery level: " + batteryLevel + "% plugged: " + batteryPlugged);
                }
                if ((batteryLevel <= batteryLimit) && (batteryPlugged == 0)) {
                    if (!disconnectedByBattery) {
                        disconnectedByBattery = true;

                        // Disconnect
                        disconnectFromIms();
                    }
                } else {
                    if (disconnectedByBattery) {
                        disconnectedByBattery = false;
                        
                        // Reconnect with a connection event
                        connectionEvent(new Intent(ConnectivityManager.CONNECTIVITY_ACTION));
                    }
                }
            } else {
                disconnectedByBattery = false;
            }
        }
    };
    
	/**
	 * @return true is device is in roaming
	 */
	public boolean isInRoaming() {
		if (connectivityMgr != null && connectivityMgr.getActiveNetworkInfo() != null) {
			return connectivityMgr.getActiveNetworkInfo().isRoaming();
		}
		return false;
	}
}
