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

package com.orangelabs.rcs.ri.utils;

import java.io.BufferedReader;
import java.io.FileReader;

public class CpuMonitor {
	/**
	 * Activation flag
	 */
    private static boolean ENABLED = false;

	/**
	 * Control thread
	 */
	private boolean stopMonitor = false;

	/**
	 * Data statistcs
	 */
	private int total_cpu = 0;
	private int current_cpu = 0;
	private int previous_used_cpu = 0;
	private int previous_total_cpu = 0;
	private int mean_cpu = 0;
	private int cpt_cpu = 0;

	/**
	 * Constructor
	 */
	public CpuMonitor() {
	}

	/**
	 * Start monitor
	 */
	public void start(){
		if (ENABLED) {
			monitoringThread.start();
		}
	}

	/**
	 * Stop monitor
	 */
	public void stop(){
		if (ENABLED) {
			stopMonitor = true;
			try {
				monitoringThread.join();
			} catch (InterruptedException e){}
		}
	}

	/**
	 * Monitoring thread
	 */
	private Thread monitoringThread = new Thread(){
		public void run(){
			while (!stopMonitor){
				try {
					getCpu();
					sleep(1000);
				} catch (InterruptedException e) {}
			}
		}
	};

	/**
	 * Retreive CPU usage
	 */
	private void getCpu(){
		try {
			// Get CPU
			FileReader fileReader = new FileReader("/proc/stat");
	        BufferedReader in = new BufferedReader(fileReader, 100);
	        String line = in.readLine();
	        if (line.startsWith("cpu")){
	        	String val[] = line.split(" +");
	        	int total = Integer.parseInt(val[1]) +
	        				Integer.parseInt(val[2]) +
	        				Integer.parseInt(val[3]) +
	        				Integer.parseInt(val[4]);
	        	int used = Integer.parseInt(val[1]) +
							Integer.parseInt(val[2])+
							Integer.parseInt(val[3]);
	        	current_cpu = ((used-previous_used_cpu)*100)/(total-previous_total_cpu);
	        	previous_total_cpu = total;
	        	previous_used_cpu = used;
	        	total_cpu +=current_cpu;
	        	cpt_cpu++;
	        	mean_cpu = total_cpu/cpt_cpu;
	        }
	        fileReader.close();

	        // Print result
	    	System.out.println(mean_cpu + "% (" + current_cpu + "%)");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}