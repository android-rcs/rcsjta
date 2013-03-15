/**
 * The API permits to share contents during a CS call (i.e. rich call
 * service). This API should be used in coordination with the
 * Capability API to discover if the remote contact supports “Image
 * share” and “video share”. The capability discovering is
 * automatically initiated by the RCS stack when the call is
 * established, then the rich call application has just to catch the
 * result to update the button “Share” in the dialer application.
 * <p>
 * See also the media API for video player and vide recorder.
 * <p>
 * See classes:
 * RichCallApi
 * RichCallApiIntents
 * IVideoSharingSession
 * IVideoSharingEventListener
 * IImageSharingSession
 * IImageSharingEventListener
 */

package org.gsma.joyn.richcall;
