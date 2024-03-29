/*
 * Copyright (C) 2009 The Android Open Source Project
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
 */

package com.android.camera.gallery;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore.Images;
import android.util.Log;

import com.android.camera.BitmapManager;
import com.android.camera.Util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a particular image and provides access to the underlying bitmap
 * and two thumbnail bitmaps as well as other information such as the id, and
 * the path to the actual image data.
 */
public abstract class BaseImage implements IImage {
    private static final String TAG = "BaseImage";
    private static final int UNKNOWN_LENGTH = -1;
    protected ContentResolver mContentResolver;

    // Database field
    protected Uri mUri;
    protected long mId;
    protected String mDataPath;
    protected long mMiniThumbMagic;
    protected final int mIndex;
    protected String mMimeType;
    private final long mDateTaken;
    private String mTitle;
    private final String mDisplayName;

    protected BaseImageList mContainer;

    private int mWidth = UNKNOWN_LENGTH;
    private int mHeight = UNKNOWN_LENGTH;

    protected BaseImage(BaseImageList container, ContentResolver cr,
                        long id, int index, Uri uri, String dataPath, long miniThumbMagic,
                        String mimeType, long dateTaken, String title, String displayName) {
        mContainer = container;
        mContentResolver = cr;
        mId = id;
        mIndex = index;
        mUri = uri;
        mDataPath = dataPath;
        mMiniThumbMagic = miniThumbMagic;
        mMimeType = mimeType;
        mDateTaken = dateTaken;
        mTitle = title;
        mDisplayName = displayName;
    }

    public String getDataPath() {
        return mDataPath;
    }

    @Override
    public boolean equals(Object other) {
        return !(other == null || !(other instanceof Image)) && mUri.equals(((Image) other).mUri);
    }

    @Override
    public int hashCode() {
        return mUri.hashCode();
    }

    public Bitmap fullSizeBitmap(int minSideLength, int maxNumberOfPixels) {
        return fullSizeBitmap(minSideLength, maxNumberOfPixels,
                IImage.ROTATE_AS_NEEDED, IImage.NO_NATIVE);
    }

    public Bitmap fullSizeBitmap(int minSideLength, int maxNumberOfPixels,
                                 boolean rotateAsNeeded, boolean useNative) {
        Uri url = mContainer.contentUri(mId);
        if (url == null) return null;

        Bitmap b = Util.makeBitmap(minSideLength, maxNumberOfPixels,
                url, mContentResolver, useNative);

        if (b != null && rotateAsNeeded) {
            b = Util.rotate(b, getDegreesRotated());
        }

        return b;
    }

    public InputStream fullSizeImageData() {
        try {
            return mContentResolver.openInputStream(mUri);

        } catch (IOException ex) {
            return null;
        }
    }

    public long fullSizeImageId() {
        return mId;
    }

    public Uri fullSizeImageUri() {
        return mUri;
    }

    public IImageList getContainer() {
        return mContainer;
    }

    public long getDateTaken() {
        return mDateTaken;
    }

    public int getDegreesRotated() {
        return 0;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    private void setupDimension() {
        ParcelFileDescriptor input = null;
        try {
            input = mContentResolver.openFileDescriptor(mUri, "r");
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapManager.instance().decodeFileDescriptor(
                    input.getFileDescriptor(), options);
            mWidth = options.outWidth;
            mHeight = options.outHeight;
        } catch (FileNotFoundException ex) {
            mWidth = 0;
            mHeight = 0;
        } finally {
            Util.closeSilently(input);
        }
    }

    public int getWidth() {
        if (mWidth == UNKNOWN_LENGTH) setupDimension();
        return mWidth;
    }

    public int getHeight() {
        if (mHeight == UNKNOWN_LENGTH) setupDimension();
        return mHeight;
    }

    public Bitmap miniThumbBitmap() {
        Bitmap b;
        try {
            long id = mId;
            b = Images.Thumbnails.getThumbnail(mContentResolver, id,
                    Images.Thumbnails.MICRO_KIND, null);
        } catch (Throwable ex) {
            Log.e(TAG, "miniThumbBitmap got exception", ex);
            return null;
        }
        if (b != null) {
            b = Util.rotate(b, getDegreesRotated());
        }
        return b;
    }

    protected void onRemove() {
    }

    @Override
    public String toString() {
        return mUri.toString();
    }
}
