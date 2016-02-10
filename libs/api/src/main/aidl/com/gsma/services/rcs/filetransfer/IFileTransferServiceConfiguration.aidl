package com.gsma.services.rcs.filetransfer;

/**
 * File transfer service configuration interface
 */
interface IFileTransferServiceConfiguration {

	long getWarnSize();

	long getMaxSize();

    long getMaxAudioMessageDuration();

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