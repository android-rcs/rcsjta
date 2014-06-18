package com.gsma.service.rcs.ft;

import java.util.Random;

import android.os.Parcel;
import android.test.AndroidTestCase;

import com.gsma.services.rcs.ft.FileTransferServiceConfiguration;

public class FileTransferServiceConfigurationTest extends AndroidTestCase {

	long warnSize;
	long maxSize;
	boolean autoAcceptModeChangeable;
	boolean autoAcceptMode;
	boolean autoAcceptModeInRoaming;
	boolean fileIcon;
	int maxFileTransfers;
	int imageResizeOption;

	protected void setUp() throws Exception {
		super.setUp();
		Random random = new Random();
		warnSize = random.nextLong();
		maxSize = random.nextLong();
		autoAcceptModeChangeable = random.nextBoolean();
		autoAcceptMode = random.nextBoolean();
		autoAcceptModeInRoaming = random.nextBoolean();
		fileIcon = random.nextBoolean();
		maxFileTransfers = random.nextInt();
		imageResizeOption = random.nextInt();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	private boolean fileTransferServiceConfigurationisEqual(FileTransferServiceConfiguration conf1,
			FileTransferServiceConfiguration conf2) {
		if (conf1.getWarnSize() != conf2.getWarnSize())
			return false;
		if (conf1.getMaxSize() != conf2.getMaxSize())
			return false;
		if (conf1.isAutoAcceptModeChangeable() != conf2.isAutoAcceptModeChangeable())
			return false;
		if (conf1.isAutoAcceptEnabled() != conf2.isAutoAcceptEnabled())
			return false;
		if (conf1.isAutoAcceptInRoamingEnabled() != conf2.isAutoAcceptInRoamingEnabled())
			return false;
		if (conf1.isFileIconSupported() != conf2.isFileIconSupported())
			return false;
		if (conf1.getMaxFileTransfers() != conf2.getMaxFileTransfers())
			return false;
		if (conf1.getImageResizeOption() != conf2.getImageResizeOption())
			return false;
		return true;

	}

	public void testChatServiceConfiguration() {
		FileTransferServiceConfiguration config = new FileTransferServiceConfiguration(warnSize, maxSize, autoAcceptModeChangeable,
				autoAcceptMode, autoAcceptModeInRoaming, fileIcon, maxFileTransfers, imageResizeOption);
		Parcel parcel = Parcel.obtain();
		config.writeToParcel(parcel, 0);
		// done writing, now reset parcel for reading
		parcel.setDataPosition(0);
		// finish round trip
		FileTransferServiceConfiguration createFromParcel = FileTransferServiceConfiguration.CREATOR.createFromParcel(parcel);
		assertTrue(fileTransferServiceConfigurationisEqual(createFromParcel, config));
	}
}
