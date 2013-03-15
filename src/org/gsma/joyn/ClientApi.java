/*
 * Copyright 2013, France Telecom
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 *    http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsma.joyn;

/**
 * Class ClientApi.
 */
public abstract class ClientApi {
    /**
     * The ctx.
     */
    protected android.content.Context ctx;

    /**
     * The ims core api.
     */
    protected IImsApi imsCoreApi;

    /**
     * The ims api connection.
     */
    protected android.content.ServiceConnection imsApiConnection;

    /**
     * The ims connection receiver.
     */
    protected android.content.BroadcastReceiver imsConnectionReceiver;

    /**
     * Creates a new instance of ClientApi.
     *
     * @param ctx Application context
     */
    public ClientApi(android.content.Context ctx) {

    }

    public void connectApi() {

    }

    public void disconnectApi() {

    }

    /**
     * Adds a api event listener.
     *
     * @param listener
     */
    public void addApiEventListener(ClientApiListener listener) {

    }

    /**
     * Removes a api event listener.
     *
     * @param listener
     */
    public void removeApiEventListener(ClientApiListener listener) {

    }

    /**
     * Adds a ims event listener.
     *
     * @param listener
     */
    public void addImsEventListener(ImsEventListener listener) {

    }

    /**
     * Removes a ims event listener.
     *
     * @param listener
     */
    public void removeImsEventListener(ImsEventListener listener) {

    }

    /**
     * Removes a all api event listeners.
     */
    public void removeAllApiEventListeners() {

    }

    /**
     *
     * @param ctx Application context
     * @return  The boolean.
     */
    public boolean isImsConnected(android.content.Context ctx) throws ClientApiException {
        return false;
    }

    protected void notifyEventApiConnected() {

    }

    protected void notifyEventApiDisconnected() {

    }

    protected void notifyEventApiDisabled() {

    }

} // end ClientApi
