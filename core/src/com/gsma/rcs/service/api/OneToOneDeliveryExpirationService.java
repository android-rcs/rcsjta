/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gsma.rcs.service.api;

import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.imdn.DeliveryExpirationManager;
import com.gsma.rcs.utils.logger.Logger;

import android.app.IntentService;
import android.content.Intent;

public class OneToOneDeliveryExpirationService extends IntentService {

    private final Logger sLogger = Logger.getLogger(OneToOneDeliveryExpirationService.class
            .getName());

    public OneToOneDeliveryExpirationService() {
        super(OneToOneDeliveryExpirationService.class.getCanonicalName());
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        final Core core = Core.getInstance();
        if (core == null || !core.isStarted()) {
            /* Stack is not started, don't process this intent */
            if (sLogger.isActivated()) {
                sLogger.warn("Stack is not running, do not process the delivery expiration intent!");
            }
            return;
        }
        InstantMessagingService imService = core.getImService();
        final DeliveryExpirationManager deliveryExpirarationManager = imService
                .getDeliveryExpirationManager();
        imService.scheduleImOperation(new Runnable() {
            @Override
            public void run() {
                String action = intent.getAction();
                try {
                    if (DeliveryExpirationManager.ACTION_CHAT_MESSAGE_DELIVERY_TIMEOUT
                            .equals(action)) {
                        deliveryExpirarationManager.onChatMessageDeliveryExpirationReceived(intent);
                    } else if (DeliveryExpirationManager.ACTION_FILE_TRANSFER_DELIVERY_TIMEOUT
                            .equals(action)) {
                        deliveryExpirarationManager
                                .onFileTransferDeliveryExpirationReceived(intent);
                    }
                } catch (RuntimeException e) {
                    /*
                     * Normally we are not allowed to catch runtime exceptions as these are genuine
                     * bugs which should be handled/fixed within the code. However the cases when we
                     * are executing operations on a thread unhandling such exceptions will
                     * eventually lead to exit the system and thus can bring the whole system down,
                     * which is not intended.
                     */
                    sLogger.error(
                            new StringBuilder(
                                    "Unable to handle one to one delivery expiration event for intent action : ")
                                    .append(action).toString(), e);
                }
            }
        });
    }
}
