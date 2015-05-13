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
import android.content.Context;
import android.content.res.Resources;

import com.gsma.services.rcs.RcsService.Direction;

/**
 * This subclass of Application allows to get a resource content from a static context
 * 
 * @author YPLO6403
 */
public class RiApplication extends Application {

    private static Context mContext;

    /**
     * Array of participant statuses
     */
    public static String[] sParticipantStatuses;

    /**
     * Array of delivery statuses
     */
    public static String[] sDeliveryStatuses;

    /**
     * Array of delivery reason codes
     */
    public static String[] sDeliveryReasonCode;

    /**
     * Array of Group CHAT states
     */
    public static String[] sGroupChatStates;

    /**
     * Array of Group CHAT reason codes
     */
    public static String[] sGroupChatReasonCodes;

    /**
     * Array of message reason codes
     */
    public static String[] sMessageReasonCodes;

    /**
     * Array of message statuses
     */
    public static String[] sMessagesStatuses;

    /**
     * Array of file transfer states
     */
    public static String[] sFileTransferStates;

    /**
     * Array of file transfer reason codes
     */
    public static String[] sFileTransferReasonCodes;

    /**
     * Array of Image sharing states
     */
    public static String[] sImageSharingStates;

    /**
     * Array of Image sharing reason codes
     */
    public static String[] sImageSharingReasonCodes;

    /**
     * Array of Video sharing states
     */
    public static String[] sVideoSharingStates;

    /**
     * Array of Video sharing reason codes
     */
    public static String[] sVideoReasonCodes;

    /**
     * Array of Geolocation sharing states
     */
    public static String[] sGeolocSharingStates;

    /**
     * Array of Geolocation sharing reason codes
     */
    public static String[] sGeolocReasonCodes;

    /**
     * Array of MULTIMEDIA Messaging Session states
     */
    public static String[] sMultimediaStates;

    /**
     * Array of MULTIMEDIA Messaging Session codes
     */
    public static String[] sMultimediaReasonCodes;

    /**
     * Array of group chat events
     */
    public static String[] sGroupChatEvents;

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
        mContext = getApplicationContext();
        Resources resources = getResources();
        sParticipantStatuses = resources.getStringArray(R.array.participant_statuses);
        sDeliveryStatuses = resources.getStringArray(R.array.delivery_statuses);
        sDeliveryReasonCode = resources.getStringArray(R.array.delivery_reason_codes);
        sGroupChatStates = resources.getStringArray(R.array.group_chat_states);
        sGroupChatReasonCodes = resources.getStringArray(R.array.group_chat_reason_codes);
        sMessageReasonCodes = resources.getStringArray(R.array.message_reason_codes);
        sMessagesStatuses = resources.getStringArray(R.array.message_statuses);
        sFileTransferStates = resources.getStringArray(R.array.file_transfer_states);
        sFileTransferReasonCodes = resources.getStringArray(R.array.file_transfer_reason_codes);
        sImageSharingStates = resources.getStringArray(R.array.ish_states);
        sImageSharingReasonCodes = resources.getStringArray(R.array.ish_reason_codes);
        sVideoSharingStates = resources.getStringArray(R.array.vsh_states);
        sVideoReasonCodes = resources.getStringArray(R.array.vsh_reason_codes);
        sGeolocSharingStates = resources.getStringArray(R.array.gsh_states);
        sGeolocReasonCodes = resources.getStringArray(R.array.gsh_reason_codes);
        sMultimediaStates = resources.getStringArray(R.array.mms_states);
        sMultimediaReasonCodes = resources.getStringArray(R.array.mms_reason_codes);
        sGroupChatEvents = resources.getStringArray(R.array.group_chat_event);

        sDirectionToString = new HashMap<Direction, String>();
        sDirectionToString.put(Direction.INCOMING, resources.getString(R.string.label_incoming));
        sDirectionToString.put(Direction.OUTGOING, resources.getString(R.string.label_outgoing));
        sDirectionToString.put(Direction.IRRELEVANT,
                resources.getString(R.string.label_direction_unknown));

        ConnectionManager.getInstance(mContext).connectApis();
    }

    /**
     * Gets the application context
     * 
     * @return the application context
     */
    public static Context getAppContext() {
        return mContext;
    }

}
