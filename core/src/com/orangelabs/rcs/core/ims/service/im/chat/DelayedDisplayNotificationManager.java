/*
 * Copyright (C) 2014 Sony Mobile Communications AB.
 * All rights, including trade secret rights, reserved.
 */

package com.orangelabs.rcs.core.ims.service.im.chat;

import com.orangelabs.rcs.core.ims.service.im.InstantMessagingService;

public class DelayedDisplayNotificationManager {

	public DelayedDisplayNotificationManager(InstantMessagingService instantMessagingService) {
		instantMessagingService.getImsModule().getCore().getListener()
				.tryToDispatchAllPendingDisplayNotifications();
	}
}
