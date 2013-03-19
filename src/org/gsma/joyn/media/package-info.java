/**
 * The media API permits to connect the media player and media
 * renderer to the RCS stack independently of the media itself. From
 * this abstraction, the RCS stack manages (i.e. start, stop) the
 * media stream thanks to the SIP call flow (e.g. the stack starts the
 * media only after the SIP ACK).
 *
 * The media API permits also to forward media error to the stack in
 * order to stop the session (e.g. SIP BYE).
 * <p>
 * The supported video encodings are H.263+ and H.264 in low quality
 * (QCIF).
 * <p>
 * This API contains by default the following media entities which may
 * be completely replaced by another implementation (e.g. native
 * implementation using hardware codecs) without any impact in the RCS
 * stack:
 * <p>
 * A live video player using the Camera.
 * A pre-recorder video player.
 * A video renderer.
 * <p>
 * The default video codec are software codecs implemented in C++
 * (source code from Android “opencore” framework) and integrated by
 * using a JNI interface.
 * <p>
 * Optimization from last API release: the RTP transport layer is part
 * of the media player or renderer in order to avoid to pass each RTP
 * sample via the AIDL interface to the RCS stack.
 */
package org.gsma.joyn.media;
