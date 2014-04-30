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
package com.orangelabs.rcs.whiteboard;

import java.util.List;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Shake motion detector
 * 
 * @author Jean-Marc AUFFRET
 */
public class ShakeMotionDetector implements SensorEventListener {
	/**
	 * Last time
	 */
	private long lastDetectionTime = 0L;
	
	/**
	 * Last acceleration
	 */
	private long lastAcceleration = 0L;

    /**
     * Sensor manager
     */
    private SensorManager sensorManager;
    
    /**
     * Listener
     */
    private ShakeMotionListener listener = null;

    /**
     * Constructor
     */
    public ShakeMotionDetector(Context context) {
        sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
    }

    /**
     * Add a listener
     * 
     * @param listener Listener
     */
    public void addListener(ShakeMotionListener listener) {
    	this.listener = listener;
    }
    
    /**
     * Remove listener
     */
    public void removeListener() {
    	this.listener = null;
    }

    /**
     * Start detector
     */
    public void start() {
    	List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER); 
	    sensorManager.registerListener(this, 
	            sensors.get(0),
	            SensorManager.SENSOR_DELAY_FASTEST);
    }

    /**
     * Stop detector
     */
    public void stop() {
        sensorManager.unregisterListener(this);
    }

    /**
     * Sensor event
     * 
     * @param event Sensor event
     */
    public void onSensorChanged(SensorEvent event) {
    	float[] values = event.values;
    	long currentTime = System.currentTimeMillis();
		double acceleration = Math.sqrt(values[0] * values[0]);
		double delta = Math.abs(acceleration - lastAcceleration);
    	if ((delta >= 10) && (currentTime > lastDetectionTime + 2000)) {
    		// Reset last detection time
    		lastDetectionTime = currentTime;

    		// Notify listener
    		if (listener != null) {
    			listener.onShakeDetected();
    		}
		}
    }

    /**
     * Sensor event
     * 
     * @param sensor Sensor
     * @param accuracy Accuracy 
     */
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
