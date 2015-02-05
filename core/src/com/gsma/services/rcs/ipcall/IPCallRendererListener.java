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
 * This class offers callback methods on IP call renderer events
 * 
 * @author Jean-Marc AUFFRET
 */
public abstract class IPCallRendererListener extends IIPCallRendererListener.Stub {
    /**
     * Callback called when the renderer is opened
     */
    public abstract void onRendererOpened();

    /**
     * Callback called when the renderer is started
     */
    public abstract void onRendererStarted();

    /**
     * Callback called when the renderer is stopped
     */
    public abstract void onRendererStopped();

    /**
     * Callback called when the renderer is closed
     */
    public abstract void onRendererClosed();

    /**
     * Callback called when the renderer has failed
     * 
     * @param error Error
     */
    public abstract void onRendererError(int error);
}
