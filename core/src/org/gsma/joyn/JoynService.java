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
package org.gsma.joyn;

import android.content.Context;

/**
 * Abstract joyn service
 * 
 * @author jexa7410
 */
public abstract class JoynService {
	/**
	 * Application context
	 */
	protected Context ctx;
	
	/**
	 * Service listener
	 */
	protected JoynServiceListener serviceListener;
	
	/**
	 * Constructor
	 * 
     * @param ctx Application context
     * @param listener Service listener
	 */
	public JoynService(Context ctx, JoynServiceListener listener) {
		this.ctx = ctx;
		this.serviceListener = listener;
	}
	
    /**
     * Connects to the API
     */
    public abstract void connect();
    
    /**
     * Disconnects from the API
     */
    public abstract void disconnect();	
}
