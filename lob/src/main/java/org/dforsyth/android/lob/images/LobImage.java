package org.dforsyth.android.lob.images;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.LruCache;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;

import org.dforsyth.android.lob.R;

/**
 * Helpers around NetworkImageView
 */
public class LobImage {
    private static final String TAG = LobImage.class.getSimpleName();

    private static LobImage sInstance;

    private static ImageLoader sImageLoader;

    private ImageRequestBuilder provide(Context context) {
        if (sImageLoader == null) {
            sImageLoader = new ImageLoader(
                    Volley.newRequestQueue(context),
                    new ImageLoader.ImageCache() {
                        // TODO: this needs to be byte based XXX TODO SOS
                        private final LruCache<String, Bitmap> mCache = new LruCache<>(10);
                        public void putBitmap(String url, Bitmap bitmap) {
                            mCache.put(url, bitmap);
                        }
                        public Bitmap getBitmap(String url) {
                            return mCache.get(url);
                        }
                    }
            );
        }

        return new ImageRequestBuilder();
    }

    public class ImageRequestBuilder {
        private String mUri;
        private int mDefaultImageResId;

        public ImageRequestBuilder fetch(String url) {
            mUri = url;
            return this;
        }

        public ImageRequestBuilder setDefaultImageResId(int defaultImageResId) {
            mDefaultImageResId = defaultImageResId;
            return this;
        }

        public void into(NetworkImageView imageView) {

            /*
            if (mUri == null) {
                throw new IllegalArgumentException("Needs a uri to fetch");
            }
            */

            Log.d(TAG, String.format("Fetching image from %s", mUri));

            if (mDefaultImageResId > 0) {
                imageView.setDefaultImageResId(mDefaultImageResId);
            }

            imageView.setImageUrl(
                    mUri,
                    sImageLoader
            );
        }

    }

    public static ImageRequestBuilder using(Context context) {
        if (sInstance == null) {
            sInstance = new LobImage();
        }

        return sInstance.provide(context);
    }
}
