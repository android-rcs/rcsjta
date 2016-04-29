/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
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

package com.gsma.rcs.core.ims.service.capability;

import com.gsma.rcs.core.content.GeolocContent;
import com.gsma.rcs.core.ims.network.sip.FeatureTags;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.rtp.MediaRegistry;
import com.gsma.rcs.core.ims.protocol.rtp.format.video.VideoFormat;
import com.gsma.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.gsma.rcs.core.ims.protocol.sdp.MediaDescription;
import com.gsma.rcs.core.ims.protocol.sdp.SdpParser;
import com.gsma.rcs.core.ims.protocol.sdp.SdpUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipMessage;
import com.gsma.rcs.core.ims.service.richcall.image.ImageTransferSession;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.MimeManager;
import com.gsma.rcs.utils.NetworkUtils;
import com.gsma.rcs.utils.StringUtils;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 * Capability utility functions
 * 
 * @author jexa7410
 */
public class CapabilityUtils {

    /**
     * Get supported feature tags for capability exchange
     * 
     * @param richcall Rich call supported
     * @param rcsSettings the accessor to RCS settings
     * @return List of tags
     */
    public static String[] getSupportedFeatureTags(boolean richcall, RcsSettings rcsSettings) {
        List<String> tags = new ArrayList<>();
        List<String> icsiTags = new ArrayList<>();
        List<String> iariTags = new ArrayList<>();
        // Video share support
        if (rcsSettings.isVideoSharingSupported() && richcall
                && (NetworkUtils.getNetworkAccessType() >= NetworkUtils.NETWORK_ACCESS_3G)) {
            tags.add(FeatureTags.FEATURE_3GPP_VIDEO_SHARE);
        }
        // Chat support
        if (rcsSettings.isImSessionSupported()) {
            iariTags.add(FeatureTags.FEATURE_RCSE_CHAT);
        }
        // FT support
        if (rcsSettings.isFileTransferSupported()) {
            iariTags.add(FeatureTags.FEATURE_RCSE_FT);
        }
        // FT over HTTP support
        if (rcsSettings.isFileTransferHttpSupported()) {
            iariTags.add(FeatureTags.FEATURE_RCSE_FT_HTTP);
        }
        // Image share support
        if (rcsSettings.isImageSharingSupported() && richcall) {
            iariTags.add(FeatureTags.FEATURE_RCSE_IMAGE_SHARE);
        }
        // Presence discovery support
        if (rcsSettings.isPresenceDiscoverySupported()) {
            iariTags.add(FeatureTags.FEATURE_RCSE_PRESENCE_DISCOVERY);
        }
        // Social presence support
        if (rcsSettings.isSocialPresenceSupported()) {
            iariTags.add(FeatureTags.FEATURE_RCSE_SOCIAL_PRESENCE);
        }
        // Geolocation push support
        if (rcsSettings.isGeoLocationPushSupported()) {
            iariTags.add(FeatureTags.FEATURE_RCSE_GEOLOCATION_PUSH);
        }
        // FT thumbnail support
        if (rcsSettings.isFileTransferThumbnailSupported()) {
            iariTags.add(FeatureTags.FEATURE_RCSE_FT_THUMBNAIL);
        }
        // FT S&F support
        if (rcsSettings.isFileTransferStoreForwardSupported()) {
            iariTags.add(FeatureTags.FEATURE_RCSE_FT_SF);
        }
        // Group chat S&F support
        if (rcsSettings.isGroupChatStoreForwardSupported()) {
            iariTags.add(FeatureTags.FEATURE_RCSE_GC_SF);
        }
        // IP call support
        if (rcsSettings.isIPVoiceCallSupported()) {
            tags.add(FeatureTags.FEATURE_RCSE_IP_VOICE_CALL);
        }
        if (rcsSettings.isIPVideoCallSupported()) {
            tags.add(FeatureTags.FEATURE_RCSE_IP_VIDEO_CALL);
        }
        if (rcsSettings.isIPVoiceCallSupported() || rcsSettings.isIPVideoCallSupported()) {
            icsiTags.add(FeatureTags.FEATURE_3GPP_IP_VOICE_CALL);
        }
        // Automata flag
        if (rcsSettings.isSipAutomata()) {
            tags.add(FeatureTags.FEATURE_SIP_AUTOMATA);
        }
        // Extensions
        if (rcsSettings.isExtensionsAllowed()) {
            for (String extension : rcsSettings.getSupportedRcsExtensions()) {
                if (extension.startsWith("gsma.")) {
                    icsiTags.add(FeatureTags.FEATURE_RCSE_ICSI_EXTENSION + "." + extension);
                } else {
                    iariTags.add(FeatureTags.FEATURE_RCSE_IARI_EXTENSION + "." + extension);
                }
            }
            icsiTags.add(FeatureTags.FEATURE_3GPP_EXTENSION);
        }
        // Add IARI prefix
        if (!iariTags.isEmpty()) {
            tags.add(FeatureTags.FEATURE_RCSE + "=\"" + TextUtils.join(",", iariTags) + "\"");
        }
        // Add ICSI prefix
        if (!icsiTags.isEmpty()) {
            tags.add(FeatureTags.FEATURE_3GPP + "=\"" + TextUtils.join(",", icsiTags) + "\"");
        }
        return tags.toArray(new String[tags.size()]);
    }

    /**
     * Extract features tags
     * 
     * @param msg Message
     * @return Capabilities
     */
    public static Capabilities extractCapabilities(SipMessage msg) {
        /* Analyze feature tags */
        Capabilities.CapabilitiesBuilder capaBuilder = new Capabilities.CapabilitiesBuilder();
        Set<String> tags = msg.getFeatureTags();
        boolean ipCall_RCSE = false;
        boolean ipCall_3GPP = false;
        for (String tag : tags) {
            if (tag.contains(FeatureTags.FEATURE_3GPP_VIDEO_SHARE)) {
                capaBuilder.setVideoSharing(true);

            } else if (tag.contains(FeatureTags.FEATURE_RCSE_IMAGE_SHARE)) {
                capaBuilder.setImageSharing(true);

            } else if (tag.contains(FeatureTags.FEATURE_RCSE_CHAT)) {
                capaBuilder.setImSession(true);

            } else if (tag.contains(FeatureTags.FEATURE_RCSE_FT)) {
                capaBuilder.setFileTransferMsrp(true);

            } else if (tag.contains(FeatureTags.FEATURE_RCSE_FT_HTTP)) {
                capaBuilder.setFileTransferHttp(true);

            } else if (tag.contains(FeatureTags.FEATURE_OMA_IM)) {
                /* Support both IM & FT services */
                capaBuilder.setImSession(true).setFileTransferMsrp(true);

            } else if (tag.contains(FeatureTags.FEATURE_RCSE_PRESENCE_DISCOVERY)) {
                capaBuilder.setPresenceDiscovery(true);

            } else if (tag.contains(FeatureTags.FEATURE_RCSE_SOCIAL_PRESENCE)) {
                capaBuilder.setSocialPresence(true);

            } else if (tag.contains(FeatureTags.FEATURE_RCSE_GEOLOCATION_PUSH)) {
                capaBuilder.setGeolocationPush(true);

            } else if (tag.contains(FeatureTags.FEATURE_RCSE_FT_THUMBNAIL)) {
                capaBuilder.setFileTransferThumbnail(true);

            } else if (tag.contains(FeatureTags.FEATURE_RCSE_IP_VOICE_CALL)) {
                if (ipCall_3GPP) {
                    capaBuilder.setIpVoiceCall(true);
                } else {
                    ipCall_RCSE = true;
                }
            } else if (tag.contains(FeatureTags.FEATURE_3GPP_IP_VOICE_CALL)) {
                if (ipCall_RCSE) {
                    capaBuilder.setIpVoiceCall(true);
                } else {
                    ipCall_3GPP = true;
                }
            } else if (tag.contains(FeatureTags.FEATURE_RCSE_IP_VIDEO_CALL)) {
                capaBuilder.setIpVideoCall(true);

            } else if (tag.contains(FeatureTags.FEATURE_RCSE_FT_SF)) {
                capaBuilder.setFileTransferStoreForward(true);

            } else if (tag.contains(FeatureTags.FEATURE_RCSE_GC_SF)) {
                capaBuilder.setGroupChatStoreForward(true);

            } else if (tag.contains(FeatureTags.FEATURE_RCSE_IARI_EXTENSION + ".ext")
                    || tag.contains(FeatureTags.FEATURE_RCSE_IARI_EXTENSION + ".mnc")
                    || tag.contains(FeatureTags.FEATURE_RCSE_ICSI_EXTENSION + ".gsma")) {
                // Support an RCS extension
                String serviceId = extractServiceId(tag);
                if (!"gsma.rcs.extension".equals(serviceId)) {
                    capaBuilder.addExtension(serviceId);
                }
            } else if (tag.contains(FeatureTags.FEATURE_SIP_AUTOMATA)) {
                capaBuilder.setSipAutomata(true);
            }
        }
        /* Analyze SDP part */
        byte[] content = msg.getContentBytes();
        if (content != null) {
            SdpParser parser = new SdpParser(content);
            /* Get supported video codecs */
            Vector<MediaDescription> mediaVideo = parser.getMediaDescriptions("video");
            Vector<String> videoCodecs = new Vector<>();
            for (int i = 0; i < mediaVideo.size(); i++) {
                MediaDescription desc = mediaVideo.get(i);
                MediaAttribute attr = desc.getMediaAttribute("rtpmap");
                if (attr != null) {
                    String rtpmap = attr.getValue();
                    String encoding = rtpmap.substring(rtpmap.indexOf(desc.mPayload)
                            + desc.mPayload.length() + 1);
                    String codec = encoding.toLowerCase().trim();
                    int index = encoding.indexOf("/");
                    if (index != -1) {
                        codec = encoding.substring(0, index);
                    }
                    if (MediaRegistry.isCodecSupported(codec)) {
                        videoCodecs.add(codec);
                    }
                }
            }
            if (videoCodecs.size() == 0) {
                /* No video codec supported between me and the remote contact */
                capaBuilder.setVideoSharing(false);
            }
            // Check supported image formats
            Vector<MediaDescription> mediaImage = parser.getMediaDescriptions("message");
            Vector<String> imgFormats = new Vector<>();
            for (int i = 0; i < mediaImage.size(); i++) {
                MediaDescription desc = mediaImage.get(i);
                MediaAttribute attr = desc.getMediaAttribute("accept-types");
                if (attr != null) {
                    String[] types = attr.getValue().split(" ");
                    for (String fmt : types) {
                        if ((fmt != null) && MimeManager.getInstance().isMimeTypeSupported(fmt)) {
                            imgFormats.addElement(fmt);
                        }
                    }
                }
            }
            if (imgFormats.size() == 0) {
                /* No image format supported between me and the remote contact */
                capaBuilder.setImageSharing(false);
            }
        }
        long timestamp = System.currentTimeMillis();
        capaBuilder.setTimestampOfLastResponse(timestamp);
        capaBuilder.setTimestampOfLastRequest(timestamp);
        return capaBuilder.build();
    }

    /**
     * Build supported SDP part
     * 
     * @param ipAddress Local IP address
     * @param richcall Rich call supported
     * @param rcsSettings RCS settings accessor
     * @return SDP
     */
    public static String buildSdp(String ipAddress, boolean richcall, RcsSettings rcsSettings) {
        String sdp = null;
        if (richcall) {
            boolean video = rcsSettings.isVideoSharingSupported()
                    && NetworkUtils.getNetworkAccessType() >= NetworkUtils.NETWORK_ACCESS_3G;
            boolean image = rcsSettings.isImageSharingSupported();
            boolean geoloc = rcsSettings.isGeoLocationPushSupported();
            if (video | image) {
                String mimeTypes = null;
                String protocol = null;
                String selector = null;
                long maxSize = 0;
                String media = null;
                // Add video config
                if (video) {
                    // Get supported video formats
                    Vector<VideoFormat> videoFormats = MediaRegistry.getSupportedVideoFormats();
                    StringBuilder videoSharingConfig = new StringBuilder();
                    for (VideoFormat videoFormat : videoFormats) {
                        videoSharingConfig.append("m=video 0 RTP/AVP ")
                                .append(videoFormat.getPayload()).append(SipUtils.CRLF);
                        videoSharingConfig.append("a=rtpmap:").append(videoFormat.getPayload())
                                .append(" ").append(videoFormat.getCodec()).append(SipUtils.CRLF);
                    }
                    media = videoSharingConfig.toString();
                }
                // Add image and geoloc config
                if (image || geoloc) {
                    StringBuilder supportedTransferFormats = new StringBuilder();
                    // Get supported image formats
                    Set<String> imageMimeTypes = MimeManager.getInstance()
                            .getSupportedImageMimeTypes();
                    for (String imageMimeType : imageMimeTypes) {
                        supportedTransferFormats.append(imageMimeType).append(" ");
                    }
                    // Get supported geoloc
                    if (geoloc) {
                        supportedTransferFormats.append(GeolocContent.ENCODING);
                    }
                    mimeTypes = supportedTransferFormats.toString().trim();
                    protocol = SdpUtils.MSRP_PROTOCOL;
                    selector = "";
                    maxSize = ImageTransferSession.getMaxImageSharingSize(rcsSettings);
                }
                sdp = SdpUtils.buildCapabilitySDP(ipAddress, protocol, mimeTypes, selector, media,
                        maxSize);
            }
        }
        return sdp;
    }

    /**
     * Extract service ID from feature tag extension
     * 
     * @param featureTag Feature tag
     * @return Service ID
     */
    public static String extractServiceId(String featureTag) {
        String[] values = featureTag.split("=");
        String value = StringUtils.removeQuotes(values[1]);
        if (featureTag.contains(FeatureTags.FEATURE_RCSE_IARI_EXTENSION)) {
            return value.substring(FeatureTags.FEATURE_RCSE_IARI_EXTENSION.length() + 1,
                    value.length());
        } else {
            return value.substring(FeatureTags.FEATURE_RCSE_ICSI_EXTENSION.length() + 1,
                    value.length());
        }
    }
}
