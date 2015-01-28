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

package com.orangelabs.rcs.utils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Build;

import com.gsma.services.rcs.vsh.VideoCodec;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.H264Config;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.JavaPacketizer;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.profiles.H264Profile1_2;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.profiles.H264Profile1_3;
import com.orangelabs.rcs.core.ims.protocol.rtp.codec.video.h264.profiles.H264Profile1b;
import com.orangelabs.rcs.core.ims.protocol.rtp.format.video.H264VideoFormat;

/**
 * Codecs utility functions
 *
 * @author hlxn7157
 */
public class CodecsUtils {
	
    // Codec parameters
	private final static String PARAM_1_3 = new StringBuilder(H264Config.CODEC_PARAM_PROFILEID).append("=")
			.append(H264Profile1_3.BASELINE_PROFILE_ID).append(";")
			.append(H264Config.CODEC_PARAM_PACKETIZATIONMODE).append("=")
			.append(JavaPacketizer.H264_ENABLED_PACKETIZATION_MODE).toString();
	
	private final static String PARAM_1_2 = new StringBuilder(H264Config.CODEC_PARAM_PROFILEID).append("=")
			.append(H264Profile1_2.BASELINE_PROFILE_ID).append(";")
			.append(H264Config.CODEC_PARAM_PACKETIZATIONMODE).append("=")
			.append(JavaPacketizer.H264_ENABLED_PACKETIZATION_MODE).toString();
	
	private final static String PARAM_1_b = new StringBuilder(H264Config.CODEC_PARAM_PROFILEID).append("=")
			.append(H264Profile1b.BASELINE_PROFILE_ID).append(";")
			.append(H264Config.CODEC_PARAM_PACKETIZATIONMODE).append("=")
			.append(JavaPacketizer.H264_ENABLED_PACKETIZATION_MODE).toString();
	
    /**
     * Get list of supported video codecs according to current network
     *
     * @return Codecs list
     */
    public static VideoCodec[] getPlayerCodecList() {
        // Get supported sizes of camera 
        boolean cif_available = true;
        boolean qvga_available = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
                Camera camera = Camera.open(i);
                Parameters param = camera.getParameters();
                List<Camera.Size> sizes = param.getSupportedPreviewSizes();
                if (!sizeContains(sizes, H264Config.CIF_WIDTH, H264Config.CIF_HEIGHT)) {
                    cif_available = false;
                }
                if (!sizeContains(sizes, H264Config.QVGA_WIDTH, H264Config.QVGA_HEIGHT)) {
                    qvga_available = false;
                }
                camera.release();
            }
        } else {
            Camera camera = Camera.open();
            Parameters param = camera.getParameters();
            List<Camera.Size> sizes = param.getSupportedPreviewSizes();
            if (!sizeContains(sizes, H264Config.CIF_WIDTH, H264Config.CIF_HEIGHT)) {
                cif_available = false;
            }
            if (!sizeContains(sizes, H264Config.QVGA_WIDTH, H264Config.QVGA_HEIGHT)) {
                qvga_available = false;
            }
            camera.release();
        }

        return getSupportedCodecList(cif_available, qvga_available);
    }

    /**
     * Test if size is in list.
     * Can't use List.contains because it doesn't work with some devices.
     *
     * @param list
     * @param width
     * @param height
     * @return boolean
     */
    private static boolean sizeContains(List<Camera.Size> list, int width, int height) {
    	for (Camera.Size size : list) {
    		 if (size.width == width && size.height == height) {
                 return true;
             }
		}
        return false;
    }

    /**
     * Get list of supported video codecs according to current network
     *
     * @param cif true if available
     * @param qvga true if available
     * @return Codecs list
     */
    private static VideoCodec[] getSupportedCodecList(boolean cif, boolean qvga) {
        int networkLevel = NetworkUtils.getNetworkAccessType();
        int payload_count = H264VideoFormat.PAYLOAD - 1;
        Set<VideoCodec> videoCodes = new HashSet<VideoCodec>();

        // Add codecs settings (ordered list)
        /*
         * 3G/3g+ -> level 1.B: profile-level-id=42900b, frame_rate=15, frame_size=QCIF, bit_rate=96k
         *
         * WIFI   -> level 1.2: profile-level-id=42800c, frame_rate=15, frame_size=QVGA, bit_rate=384k
         * WIFI   -> level 1.2: profile-level-id=42800c, frame_rate=15, frame_size=CIF, bit_rate=384k
         * WIFI   -> level 1.3: profile-level-id=42800d, frame_rate=15, frame_size=CIF, bit_rate=384k
         */
    	if (NetworkUtils.NETWORK_ACCESS_WIFI == networkLevel || NetworkUtils.NETWORK_ACCESS_4G == networkLevel) {
            if (cif) {
            	videoCodes.add(new VideoCodec(H264Config.CODEC_NAME,
                        ++payload_count,
                        H264Config.CLOCK_RATE,
                        15,
                        256000,
                        H264Config.CIF_WIDTH, H264Config.CIF_HEIGHT,
                        PARAM_1_3));
                
            	videoCodes.add(new VideoCodec(H264Config.CODEC_NAME,
                        ++payload_count,
                        H264Config.CLOCK_RATE,
                        15,
                        176000,
                        H264Config.CIF_WIDTH, H264Config.CIF_HEIGHT,
                        PARAM_1_2));
            }
            if (qvga) {
            	videoCodes.add(new VideoCodec(H264Config.CODEC_NAME,
                        ++payload_count,
                        H264Config.CLOCK_RATE,
                        15,
                        176000,
                        H264Config.QVGA_WIDTH, H264Config.QVGA_HEIGHT,                        
                        PARAM_1_2));
            }
        }
    	videoCodes.add(new VideoCodec(H264Config.CODEC_NAME,
                ++payload_count,
                H264Config.CLOCK_RATE,
                15,
                96000,
                H264Config.QCIF_WIDTH, H264Config.QCIF_HEIGHT,                        
                PARAM_1_b));

        return (VideoCodec[]) videoCodes.toArray(new VideoCodec[videoCodes.size()]);
    }
}
