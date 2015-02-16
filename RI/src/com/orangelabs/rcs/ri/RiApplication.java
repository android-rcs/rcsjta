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
import java.util.Map;

import android.app.Application;
import android.content.res.Resources;

import com.gsma.services.rcs.RcsService.Direction;

/**
 * This subclass of Application allows to get a resource content from a static context
 * 
 * @author YPLO6403
 */
public class RiApplication extends Application {

    /**
     * Array of participant status
     */
    public static String[] PARTICIPANT_STATUSES;

    /**
     * Array of delivery states
     */
    public static String[] DELIVERY_STATUSES;

    /**
     * Array of delivery reason codes
     */
    public static String[] DELIVERY_REASON_CODES;

    /**
     * Array of Group CHAT states
     */
    public static String[] GC_STATES;

    /**
     * Array of Group CHAT reason codes
     */
    public static String[] GC_REASON_CODES;

    /**
     * Array of message reason codes
     */
    public static String[] MESSAGE_REASON_CODES;

    /**
     * Array of message statuses
     */
    public static String[] MESSAGE_STATUSES;

    /**
     * Array of file transfer states
     */
    public static String[] FT_STATES;

    /**
     * Array of file transfer reason codes
     */
    public static String[] FT_REASON_CODES;

    /**
     * Array of Image sharing states
     */
    public static String[] ISH_STATES;

    /**
     * Array of Image sharing reason codes
     */
    public static String[] ISH_REASON_CODES;

    /**
     * Array of Video sharing states
     */
    public static String[] VSH_STATES;

    /**
     * Array of Video sharing reason codes
     */
    public static String[] VSH_REASON_CODES;

    /**
     * Array of Geolocation sharing states
     */
    public static String[] GSH_STATES;

    /**
     * Array of Geolocation sharing reason codes
     */
    public static String[] GSH_REASON_CODES;

    /**
     * Array of IPCall states
     */
    public static String[] IPCALL_STATES;

    /**
     * Array of IPCall reason codes
     */
    public static String[] IPCALL_REASON_CODES;

    /**
     * Array of MULTIMEDIA Messaging Session states
     */
    public static String[] MMS_STATES;

    /**
     * Array of MULTIMEDIA Messaging Session codes
     */
    public static String[] MMS_REASON_CODES;

    /**
     * Array of group chat events
     */
    public static String[] GROUP_CHAT_EVENTS;

    private static Map<Direction, String> sDirectionToString;

    /**
     * Gets direction
     * 
     * @param direction Direction
     * @return String
     */
    public static String getDirection(Direction direction) {
        return sDirectionToString.get(direction);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Resources resources = getResources();
        PARTICIPANT_STATUSES = resources.getStringArray(R.array.participant_statuses);
        DELIVERY_STATUSES = resources.getStringArray(R.array.delivery_statuses);
        DELIVERY_REASON_CODES = resources.getStringArray(R.array.delivery_reason_codes);
        GC_STATES = resources.getStringArray(R.array.group_chat_states);
        GC_REASON_CODES = resources.getStringArray(R.array.group_chat_reason_codes);
        MESSAGE_REASON_CODES = resources.getStringArray(R.array.message_reason_codes);
        MESSAGE_STATUSES = resources.getStringArray(R.array.message_statuses);
        FT_STATES = resources.getStringArray(R.array.file_transfer_states);
        FT_REASON_CODES = resources.getStringArray(R.array.file_transfer_reason_codes);
        ISH_STATES = resources.getStringArray(R.array.ish_states);
        ISH_REASON_CODES = resources.getStringArray(R.array.ish_reason_codes);
        VSH_STATES = resources.getStringArray(R.array.vsh_states);
        VSH_REASON_CODES = resources.getStringArray(R.array.vsh_reason_codes);
        GSH_STATES = resources.getStringArray(R.array.gsh_states);
        GSH_REASON_CODES = resources.getStringArray(R.array.gsh_reason_codes);
        IPCALL_STATES = resources.getStringArray(R.array.ipcall_states);
        IPCALL_REASON_CODES = resources.getStringArray(R.array.ipcall_reason_codes);
        MMS_STATES = resources.getStringArray(R.array.mms_states);
        MMS_REASON_CODES = resources.getStringArray(R.array.mms_reason_codes);
        GROUP_CHAT_EVENTS = resources.getStringArray(R.array.group_chat_event);

        sDirectionToString = new HashMap<Direction, String>();
        sDirectionToString.put(Direction.INCOMING, resources.getString(R.string.label_incoming));
        sDirectionToString.put(Direction.OUTGOING, resources.getString(R.string.label_outgoing));
        sDirectionToString.put(Direction.IRRELEVANT,
                resources.getString(R.string.label_direction_unknown));

        ConnectionManager.getInstance(getApplicationContext()).connectApis();
    }

}
