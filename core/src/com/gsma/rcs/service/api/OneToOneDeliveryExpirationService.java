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
import com.gsma.rcs.utils.logger.Logger;

import android.app.IntentService;
import android.content.Intent;

public class OneToOneDeliveryExpirationService extends IntentService {

    private final Logger mLogger = Logger.getLogger(getClass().getName());

    public OneToOneDeliveryExpirationService() {
        super(OneToOneDeliveryExpirationService.class.getCanonicalName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Core core = Core.getInstance();
        if (core == null || !core.isStarted()) {
            /* Stack is not started, don't process this intent */
            if (mLogger.isActivated()) {
                mLogger.warn("Stack is not running, do not process the delivery expiration intent!");
            }
            return;
        }
        String action = intent.getAction();
        if (OneToOneUndeliveredImManager.ACTION_CHAT_MESSAGE_DELIVERY_TIMEOUT.equals(action)) {
            core.getListener().handleOneToOneChatMessageDeliveryExpiration(intent);
        } else if (OneToOneUndeliveredImManager.ACTION_FILE_TRANSFER_DELIVERY_TIMEOUT
                .equals(action)) {
            core.getListener().handleOneToOneFileTransferDeliveryExpiration(intent);
        }
    }
}
