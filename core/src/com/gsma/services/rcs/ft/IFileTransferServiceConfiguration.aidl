package com.gsma.services.rcs.ft;

/**
 * File transfer service configuration interface
 */
interface IFileTransferServiceConfiguration {

	long getWarnSize();

	long getMaxSize();

	boolean isAutoAcceptEnabled();
	
	void setAutoAccept(in boolean enable);

	boolean isAutoAcceptInRoamingEnabled();
	
	void setAutoAcceptInRoaming(in boolean enable);

	boolean isAutoAcceptModeChangeable();
	
	int getMaxFileTransfers();
	
	int getImageResizeOption();
	
	void setImageResizeOption(in int option);
	
	boolean isGroupFileTransferSupported();

}