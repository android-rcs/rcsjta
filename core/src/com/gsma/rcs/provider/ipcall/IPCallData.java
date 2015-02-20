/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.provider.ipcall;

import com.gsma.rcs.service.ipcalldraft.IPCallLog;

/**
 * IP call history provider data
 * 
 * @author owom5460
 */
public class IPCallData {

    /**
     * Column name
     */
    static final String KEY_BASECOLUMN_ID = IPCallLog.CONTACT;

    /**
     * Column name
     */
    static final String KEY_CONTACT = IPCallLog.CONTACT;

    /**
     * Column name
     */
    static final String KEY_DIRECTION = IPCallLog.DIRECTION;

    /**
     * Column name
     */
    static final String KEY_TIMESTAMP = IPCallLog.TIMESTAMP;

    /**
     * Column name
     */
    static final String KEY_STATE = IPCallLog.STATE;

    /**
     * Column name
     */
    static final String KEY_REASON_CODE = IPCallLog.REASON_CODE;

    /**
     * Column name
     */
    static final String KEY_CALL_ID = IPCallLog.CALL_ID;

    static final String KEY_VIDEO_ENCODING = IPCallLog.VIDEO_ENCODING;

    static final String KEY_AUDIO_ENCODING = IPCallLog.AUDIO_ENCODING;

    static final String KEY_WIDTH = IPCallLog.WIDTH;

    static final String KEY_HEIGHT = IPCallLog.HEIGHT;
}
