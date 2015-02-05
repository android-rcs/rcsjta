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

package com.orangelabs.rcs.provider.fthttp;

import java.util.List;

/**
 * @author YPLO6403 Interface to get access to FT HTTP data objects
 */
public interface FtHttpResumeDao {

    /**
     * Query all entries sorted in _ID ascending order
     * 
     * @return the list of entries
     */
    public List<FtHttpResume> queryAll();

    /**
     * Query the upload entry with TID
     * 
     * @param tId the {@code tId} value.
     * @return the entry (Can be {@code null}).
     */
    public FtHttpResumeUpload queryUpload(String tId);
}
