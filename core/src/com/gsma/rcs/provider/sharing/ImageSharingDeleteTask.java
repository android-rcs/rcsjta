
package com.gsma.rcs.provider.sharing;

import com.gsma.rcs.core.ims.service.richcall.RichcallService;
import com.gsma.rcs.core.ims.service.richcall.image.ImageTransferSession;
import com.gsma.rcs.provider.DeleteTask;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.service.api.ImageSharingServiceImpl;
import com.gsma.services.rcs.contact.ContactId;

import java.util.Set;

public class ImageSharingDeleteTask extends DeleteTask.GroupedByContactId {

    private final ImageSharingServiceImpl mImageSharingService;

    private final RichcallService mRichcallService;

    /**
     * Deletion of all image sharing.
     * 
     * @param imageSharingService the image service impl
     * @param richcallService the rich call service
     * @param contentResolver the local content resolver
     * @param imsLock the ims operation lock
     */
    public ImageSharingDeleteTask(ImageSharingServiceImpl imageSharingService,
            RichcallService richcallService, LocalContentResolver contentResolver, Object imsLock) {
        super(contentResolver, imsLock, ImageSharingData.CONTENT_URI,
                ImageSharingData.KEY_SHARING_ID, ImageSharingData.KEY_CONTACT, (String) null);
        mImageSharingService = imageSharingService;
        mRichcallService = richcallService;
    }

    /**
     * Deletion of a specific image sharing.
     * 
     * @param imageSharingService the image service impl
     * @param richcallService the rich call service
     * @param contentResolver the local content resolver
     * @param imsLock the ims operation lock
     * @param sharingId the sharing id
     */
    public ImageSharingDeleteTask(ImageSharingServiceImpl imageSharingService,
            RichcallService richcallService, LocalContentResolver contentResolver, Object imsLock,
            String sharingId) {
        super(contentResolver, imsLock, ImageSharingData.CONTENT_URI,
                ImageSharingData.KEY_SHARING_ID, ImageSharingData.KEY_CONTACT, null, sharingId);
        mImageSharingService = imageSharingService;
        mRichcallService = richcallService;
    }

    /**
     * Deletion of all image sharing with a specific contact.
     * 
     * @param imageSharingService the image service impl
     * @param richcallService the rich call service
     * @param contentResolver the local content resolver
     * @param imsLock the ims operation lock
     * @param contact the contact id
     */
    public ImageSharingDeleteTask(ImageSharingServiceImpl imageSharingService,
            RichcallService richcallService, LocalContentResolver contentResolver, Object imsLock,
            ContactId contact) {
        super(contentResolver, imsLock, ImageSharingData.CONTENT_URI,
                ImageSharingData.KEY_SHARING_ID, ImageSharingData.KEY_CONTACT, contact);
        mImageSharingService = imageSharingService;
        mRichcallService = richcallService;
    }

    @Override
    protected void onRowDelete(ContactId contact, String sharingId) {
        ImageTransferSession session = mRichcallService.getImageTransferSession(sharingId);
        if (session == null) {
            mImageSharingService.removeImageSharing(sharingId);
            return;

        }
        session.deleteSession();
        mImageSharingService.removeImageSharing(sharingId);
    }

    @Override
    protected void onCompleted(ContactId contact, Set<String> deletedIds) {
        mImageSharingService.broadcastDeleted(contact, deletedIds);
    }
}
