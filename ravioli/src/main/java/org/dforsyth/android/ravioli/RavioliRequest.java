/*
 * Copyright (c) 2014, David Forsythe
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of Ravioli nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.dforsyth.android.ravioli;

import android.net.Uri;

import com.android.volley.Request.Method;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * A builder for decodable volley requests
 */
public class RavioliRequest<T> {
    public final static int DEFAULT_METHOD = Method.GET;

    private Ravioli mClient;

    private int mMethod;
    private Uri mUri;
    private Map<String, String> mHeaders;
    private Map<String, String> mParams;
    private byte[] mBody;
    private RetryPolicy mRetryPolicy;
    private RavioliDynamic mDynamic;

    private Type mType;

    private RavioliRequest(Builder builder) {
        mClient = builder.mClient;
        mMethod = builder.mMethod;
        mUri = builder.mUriBuilder.build();
        mHeaders = builder.mHeaders;
        mParams = builder.mPostParams;
        mBody = builder.mBody;
        mType = builder.mType;

        // TODO: make it possible to set dynamics on individual requests
        mDynamic = mClient.getDynamic();

        // if we don't set a retry policy in the builder, fall back to the client default (which might be null).
        mRetryPolicy = builder.mRetryPolicy != null ? builder.mRetryPolicy : mClient.getRetryPolicy();
    }

    public int getMethod() {
        return mMethod;
    }

    public Uri getUri() {
        return mUri;
    }

    public Map<String, String> getHeaders() {
        return mHeaders;
    }

    public Map<String, String> getParams() {
        return mParams;
    }

    public interface Callbacks<T> {
        public void onSuccess(RavioliResponse<T> response);
        public void onFailure(VolleyError error);
    }

    public RavioliResponse<T> request() throws ExecutionException, InterruptedException {
        return request(null);
    }

    public RavioliResponse<T> request(Object tag) throws ExecutionException, InterruptedException {
        RequestFuture<RavioliResponse<T>> future = RequestFuture.newFuture();

        ObjectRequest<T> request = createRequest(tag, future, future);

        mClient.submitRequest(request);

        return future.get();
    }

    public void requestAsync(Callbacks<T> callback) {
        requestAsync(null, callback);

    }

    public void requestAsync(Object tag, final Callbacks<T> callbacks) {
        Response.Listener<RavioliResponse<T>> listener = new Response.Listener<RavioliResponse<T>>() {
            @Override
            public void onResponse(RavioliResponse<T> response) {
                callbacks.onSuccess(response);
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                callbacks.onFailure(error);
            }
        };

        ObjectRequest<T> request = createRequest(tag, listener, errorListener);

        mClient.submitRequest(request);
    }

    public ObjectRequest<T> createRequest(Object tag, Response.Listener<RavioliResponse<T>> listener, Response.ErrorListener errorListener) {
        ObjectRequest<T> request = new ObjectRequest<T>(
                mMethod,
                mUri.toString(),
                mClient.mEncoder,
                mDynamic,
                mType,
                mHeaders,
                mParams,
                mBody,
                listener,
                errorListener
        );

        if (mRetryPolicy != null) {
            request.setRetryPolicy(mRetryPolicy);
        }

        if (tag != null) {
            request.setTag(tag);
        }

        return request;
    }

    /**
     * Builder for {@link RavioliRequest}
     *
     * @param <T> Response type
     */
    public static class Builder<T> {
        private Ravioli mClient;
        private Uri.Builder mUriBuilder;
        private Type mType;

        private int mMethod = DEFAULT_METHOD;

        private Map<String, String> mPostParams;
        private byte[] mBody;
        private Map<String, String> mHeaders;

        private RetryPolicy mRetryPolicy;

        public Builder(Ravioli client, Type type) {
            mClient = client;
            mType = type;

            mUriBuilder = mClient.getBaseUri().buildUpon();

            mPostParams = new HashMap<>();
            mHeaders = new HashMap<>(mClient.getHeaders());
        }

        public Builder(Ravioli client) {
            this(client, null);
        }

        public Builder<T> setMethod(int method) {
            if (method == Method.DEPRECATED_GET_OR_POST) {
                throw new IllegalArgumentException("DEPRECATED_GET_OR_POST");
            }

            mMethod = method;
            return this;
        }

        public Builder<T> addQueryParameter(String key, String value) {
            mUriBuilder.appendQueryParameter(key, value);
            return this;
        }

        public Builder<T> addParam(String key, String value) {
            mPostParams.put(key, value);
            return this;
        }

        public Builder<T> addHeader(String key, String value) {
            mHeaders.put(key, value);
            return this;
        }

        public Builder<T> addPath(String path) {
            mUriBuilder.appendEncodedPath(path);
            return this;
        }

        public Builder<T> setRetryPolicy(RetryPolicy retryPolicy) {
            mRetryPolicy = retryPolicy;
            return this;
        }

        public Builder<T> setBody(byte[] body) {
            mBody = body;
            return this;
        }

        private final static int[] PARAM_METHODS = {
                Method.POST,
                Method.PUT,
                Method.PATCH
        };
        private boolean isParamMethod(int method) {
            for (int pmethod : PARAM_METHODS) {
                if (pmethod == method) {
                    return true;
                }
            }
            return false;
        }

        public RavioliRequest<T> build() {
            if (!isParamMethod(mMethod) && (mPostParams.size() > 0 || mBody != null)) {
                throw new IllegalArgumentException("Parameters in non-param request");
            }

            if (mPostParams.size() > 0 && mBody != null) {
                throw new IllegalArgumentException("Cannot have post params and explicit body");
            }

            return new RavioliRequest<T>(this);
        }
    }
}
