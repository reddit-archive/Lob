package org.dforsyth.android.lob.images;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;

/**
 * Created by dforsyth on 1/9/15.
 */
public class LobImage {

    private static LobImage sInstance;

    private static ImageLoader mImageLoader;

    private ImageRequestBuilder provide(Context context) {
        if (mImageLoader == null) {
            mImageLoader = new ImageLoader(
                    Volley.newRequestQueue(context),
                    new ImageLoader.ImageCache() {
                        private final LruCache<String, Bitmap> mCache = new LruCache<String, Bitmap>(4000);
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

        public ImageRequestBuilder fetch(String url) {
            mUri = url;
            return this;
        }

        public void into(NetworkImageView imageView) {
            if (mUri == null) {
                throw new IllegalArgumentException("Needs a uri to fetch");
            }

            imageView.setImageUrl(
                    mUri,
                    mImageLoader
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
