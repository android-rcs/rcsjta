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

package com.orangelabs.rcs.core.ims.protocol.rtp.core;

import static com.orangelabs.rcs.utils.StringUtils.UTF8;

/**
 * RCTP SDES item
 *
 * @author jexa7410
 */
public class RtcpSdesItem {
	public int type;
	public byte[] data;

	public RtcpSdesItem() {
	}

	public RtcpSdesItem(int i, String string) {
		type = i;
		data = string.getBytes(UTF8);
	}
}
