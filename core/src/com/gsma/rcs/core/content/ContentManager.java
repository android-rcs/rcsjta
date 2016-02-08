/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications AB.
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
 * NOTE: This file has been modified by Sony Mobile Communications AB.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.core.content;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.rtp.codec.video.h264.H264Config;
import com.gsma.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.gsma.rcs.core.ims.protocol.sdp.MediaDescription;
import com.gsma.rcs.core.ims.protocol.sdp.SdpParser;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.gsma.rcs.platform.file.FileFactory;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.MimeManager;

import android.net.Uri;

import java.io.File;
import java.util.Vector;

/**
 * Multimedia content manager
 * 
 * @author jexa7410
 */
public class ContentManager {
    /**
     * Generate an Uri for the received content
     * 
     * @param fileName File name
     * @param mime MIME type
     * @param rcsSettings
     * @return Uri
     */
    public static Uri generateUriForReceivedContent(String fileName, String mime,
            RcsSettings rcsSettings) {
        /* Generate a file path */
        String path;
        if (MimeManager.isImageType(mime)) {
            path = rcsSettings.getPhotoRootDirectory();

        } else if (MimeManager.isVideoType(mime)) {
            path = rcsSettings.getVideoRootDirectory();

        } else {
            path = rcsSettings.getFileRootDirectory();
        }

        /*
         * Check that the fileName will not overwrite existing file We modify it if a file of the
         * same name exists, by appending _1 before the extension For example if image.jpeg exists,
         * next file will be image_1.jpeg, then image_2.jpeg etc.
         */
        StringBuilder extension = new StringBuilder("");
        if ((fileName != null) && (fileName.indexOf('.') != -1)) {
            // if extension is present, split it
            extension = new StringBuilder(".")
                    .append(fileName.substring(fileName.lastIndexOf('.') + 1));
            fileName = fileName.substring(0, fileName.lastIndexOf('.'));
        }
        String destination = fileName;
        int i = 1;
        while (new File(new StringBuilder(path).append(destination).append(extension).toString())
                .exists()) {
            destination = new StringBuilder(fileName).append('_').append(i).toString();
            i++;
        }

        /* Return free destination URI */
        return Uri.fromFile(new File(new StringBuilder(path).append(destination).append(extension)
                .toString()));
    }

    /**
     * Create a content object from URI description
     * 
     * @param uri Content URI
     * @param size Content size
     * @param fileName The file name
     * @return Content instance
     */
    public static MmContent createMmContent(Uri uri, long size, String fileName) {
        String extension = MimeManager.getFileExtension(fileName);
        String mime = MimeManager.getInstance().getMimeType(extension);
        if (size < 0 || fileName == null || mime == null) {
            throw new IllegalArgumentException(new StringBuilder("Invalid file, size ")
                    .append(size).append(" fileName ").append(fileName).append(" mimeType ")
                    .append(mime).append(" unable to create MmContent.").toString());
        }
        return createMmContentFromMime(uri, mime, size, fileName);
    }

    /**
     * Create a content object from MIME type
     * 
     * @param uri Content URI
     * @param mime MIME type
     * @param size Content size
     * @param fileName The file name
     * @return Content instance
     */
    public static MmContent createMmContentFromMime(Uri uri, String mime, long size, String fileName) {
        if (mime != null) {
            if (MimeManager.isImageType(mime)) {
                return new PhotoContent(uri, mime, size, fileName);
            }
            if (MimeManager.isVideoType(mime)) {
                return new VideoContent(uri, mime, size, fileName);
            }
            if (MimeManager.isAudioType(mime)) {
                return new AudioContent(uri, mime, size, fileName);
            }
            if (MimeManager.isVCardType(mime)) {
                return new VisitCardContent(uri, mime, size, fileName);
            }
            if (MimeManager.isGeolocType(mime)) {
                return new GeolocContent(uri, size, fileName);
            }
        }
        return new FileContent(uri, size, fileName);
    }

    /**
     * Create a live video content object
     * 
     * @param codec Codec
     * @param width Width
     * @param height Height
     * @return Content instance
     */
    public static LiveVideoContent createLiveVideoContent(String codec, int width, int height) {
        LiveVideoContent videoContent = new LiveVideoContent("video/" + codec);
        videoContent.setWidth(width);
        videoContent.setHeight(height);
        return videoContent;
    }

    /**
     * Create a live audio content object
     * 
     * @param codec Codec
     * @return Content instance
     */
    public static LiveAudioContent createLiveAudioContent(String codec) {
        return new LiveAudioContent("audio/" + codec);
    }

    /**
     * Create a generic live video content object
     * 
     * @return Content instance
     */
    public static LiveVideoContent createGenericLiveVideoContent() {
        return new LiveVideoContent("video/*");
    }

    /**
     * Create a generic live audio content object
     * 
     * @return Content instance
     */
    public static LiveAudioContent createGenericLiveAudioContent() {
        return new LiveAudioContent("audio/*");
    }

    /**
     * Create a live video content object
     * 
     * @param sdp SDP part
     * @return Content instance
     */
    public static LiveVideoContent createLiveVideoContentFromSdp(byte[] sdp) {
        /* Parse the remote SDP part */
        SdpParser parser = new SdpParser(sdp);
        Vector<MediaDescription> media = parser.getMediaDescriptions();
        if (media.size() == 0) { /* there is no media in SDP */
            return null;
        }
        MediaDescription desc = media.elementAt(0);
        if (media.size() == 1) {
            /*
             * if only one media in SDP, test if 'video', if not then return null.
             */
            if (!desc.mName.equals("video")) {
                return null;
            }
        }
        if (media.size() == 2) {
            /*
             * if two media in SDP, test if first 'video', if not then choose second and test if
             * video, if not return null.
             */
            if (!desc.mName.equals("video")) {
                desc = media.elementAt(1);
                if (!desc.mName.equals("video")) {
                    return null;
                }
            }
        }

        String rtpmap = desc.getMediaAttribute("rtpmap").getValue();
        /* Extract the video encoding */
        String encoding = rtpmap.substring(rtpmap.indexOf(desc.mPayload) + desc.mPayload.length()
                + 1);
        String codec = encoding.toLowerCase().trim();
        int index = encoding.indexOf("/");
        if (index != -1) {
            codec = encoding.substring(0, index);
        }

        /* Extract video size */
        MediaAttribute frameSize = desc.getMediaAttribute("framesize");
        int width = 0;
        int height = 0;
        if (frameSize != null) {
            try {
                String value = frameSize.getValue();
                index = value.indexOf(desc.mPayload);
                int separator = value.indexOf('-');
                if (index != -1 && separator != -1) {
                    width = Integer.parseInt(value.substring(index + desc.mPayload.length() + 1,
                            separator));
                    height = Integer.parseInt(value.substring(separator + 1));
                }
            } catch (NumberFormatException e) {
                /* Use default value */
                width = H264Config.QCIF_WIDTH;
                height = H264Config.QCIF_WIDTH;
            }
        }

        return createLiveVideoContent(codec, width, height);
    }

    /**
     * Create a live audio content object
     * 
     * @param sdp SDP part
     * @return Content instance
     */
    public static LiveAudioContent createLiveAudioContentFromSdp(byte[] sdp) {
        /* Parse the remote SDP part */
        SdpParser parser = new SdpParser(sdp);
        Vector<MediaDescription> media = parser.getMediaDescriptions(); // TODO replace with
                                                                        // getMediaDescriptions(audio)
        if (media.size() == 0) {
            return null;
        }
        MediaDescription desc = media.elementAt(0);
        if (media.size() == 1) {
            /*
             * if only one media in SDP, test if 'audio', if not then return null.
             */
            if (!desc.mName.equals("audio")) {
                return null;
            }
        }
        if (media.size() == 2) {
            /*
             * if two media in SDP, test if first 'audio', if not then choose second and test if
             * 'audio', if not return null.
             */
            if (!desc.mName.equals("audio")) {
                desc = media.elementAt(1);
                if (!desc.mName.equals("audio")) {
                    return null;
                }
            }
        }
        if (!desc.mName.equals("audio")) {
            return null;
        }
        String rtpmap = desc.getMediaAttribute("rtpmap").getValue();

        /* Extract the audio encoding */
        String encoding = rtpmap.substring(rtpmap.indexOf(desc.mPayload) + desc.mPayload.length()
                + 1);
        String codec = encoding.toLowerCase().trim();
        int index = encoding.indexOf("/");
        if (index != -1) {
            codec = encoding.substring(0, index);
        }
        return createLiveAudioContent(codec);
    }

    /**
     * Create a content object from SDP description of a SIP invite request
     * 
     * @param invite SIP invite request
     * @param rcsSettings
     * @return Content instance
     * @throws PayloadException
     */
    public static MmContent createMmContentFromSdp(SipRequest invite, RcsSettings rcsSettings)
            throws PayloadException {
        String remoteSdp = invite.getSdpContent();
        SipUtils.assertContentIsNotNull(remoteSdp, invite);
        SdpParser parser = new SdpParser(remoteSdp.getBytes(UTF8));
        Vector<MediaDescription> media = parser.getMediaDescriptions();
        MediaDescription desc = media.elementAt(0);
        MediaAttribute attr1 = desc.getMediaAttribute("file-selector");
        String fileSelectorValue = attr1.getValue();
        String mime = SipUtils.extractParameter(fileSelectorValue, "type:",
                "application/octet-stream");
        long size = Long.parseLong(SipUtils.extractParameter(fileSelectorValue, "size:", "-1"));
        String filename = SipUtils.extractParameter(fileSelectorValue, "name:", "");
        Uri file = ContentManager.generateUriForReceivedContent(filename, mime, rcsSettings);
        MediaAttribute attr2 = desc.getMediaAttribute("file-disposition");
        String fileDispoValue = attr2.getValue();
        MmContent content = ContentManager.createMmContent(file, size, filename);
        if (FileSharingSession.FILE_DISPOSITION_RENDER.equals(fileDispoValue)) {
            content.setPlayable(true);
        }
        return content;
    }

    /**
     * Get sent photo root directory
     * 
     * @param rcsSettings
     * @return Path of sent photo root directory
     */
    public static String getSentPhotoRootDirectory(RcsSettings rcsSettings) {
        return rcsSettings.getPhotoRootDirectory().concat(FileFactory.SENT_DIRECTORY);
    }

    /**
     * Get sent video root directory
     * 
     * @param rcsSettings
     * @return Path of sent video root directory
     */
    public static String getSentVideoRootDirectory(RcsSettings rcsSettings) {
        return rcsSettings.getVideoRootDirectory().concat(FileFactory.SENT_DIRECTORY);
    }

    /**
     * Get sent file root directory
     * 
     * @param rcsSettings
     * @return Path of sent file root directory
     */
    public static String getSentFileRootDirectory(RcsSettings rcsSettings) {
        return rcsSettings.getFileRootDirectory().concat(FileFactory.SENT_DIRECTORY);
    }

    /**
     * Generate Uri for saving the content that has to be transferred
     * 
     * @param fileName
     * @param mime
     * @param rcsSettings
     * @return Uri
     */
    public static Uri generateUriForSentContent(String fileName, String mime,
            RcsSettings rcsSettings) {
        String path;
        if (MimeManager.isImageType(mime)) {
            path = getSentPhotoRootDirectory(rcsSettings);
        } else if (MimeManager.isVideoType(mime)) {
            path = getSentVideoRootDirectory(rcsSettings);
        } else {
            path = getSentFileRootDirectory(rcsSettings);
        }
        /*
         * Check that the fileName will not overwrite existing file We modify it if a file of the
         * same name exists, by appending _1 before the extension For example if image.jpeg exists,
         * next file will be image_1.jpeg, then image_2.jpeg etc.
         */

        if (fileName.indexOf('.') == -1) {
            throw new RuntimeException("Filename without extension: fileName='" + fileName + "'!");
        }
        /* if extension is present, split it */
        int extPosition = fileName.lastIndexOf('.');
        String extension = "." + fileName.substring(extPosition + 1);
        fileName = fileName.substring(0, extPosition);
        String destination = fileName;
        int incrementIndex = 1;
        File generatedFile = new File(path + destination + extension);
        while (generatedFile.exists()) {
            destination = fileName + '_' + incrementIndex;
            generatedFile = new File(path + destination + extension);
            incrementIndex++;
        }
        return Uri.fromFile(generatedFile);
    }
}
