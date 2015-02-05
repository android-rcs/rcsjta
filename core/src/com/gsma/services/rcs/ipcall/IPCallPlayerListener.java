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

package com.gsma.services.rcs.ipcall;

/**
 * This class offers callback methods on IP call player events
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class IPCallPlayerListener extends IIPCallPlayerListener.Stub {
    /**
     * Callback called when the player is opened
     */
    public abstract void onPlayerOpened();

    /**
     * Callback called when the player is started
     */
    public abstract void onPlayerStarted();

    /**
     * Callback called when the player is stopped
     */
    public abstract void onPlayerStopped();

    /**
     * Callback called when the player is closed
     */
    public abstract void onPlayerClosed();

    /**
     * Callback called when the player has failed
     * 
     * @param error Error
     */
    public abstract void onPlayerError(int error);
}
