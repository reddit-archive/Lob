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
 *  Neither the name of Lob nor the names of its
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

package org.dforsyth.android.lob;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RetryPolicy;

import org.dforsyth.android.lob.encoders.Encoder;
import org.dforsyth.android.lob.encoders.GsonEncoder;
import org.dforsyth.android.lob.queues.LobQueue;
import org.dforsyth.android.lob.queues.SimpleQueue;
import org.dforsyth.android.lob.util.Constants;

import java.util.HashMap;
import java.util.Map;

/**
 * A general http client that wraps Volley
 */
public class Lob {

    private final Context mContext;
    private final Map<String, String> mHeaders;
    private final RetryPolicy mRetryPolicy;
    private final LobDynamic mDynamic;

    protected final Encoder mEncoder;
    protected final LobQueue mQueue;

    private final Uri mBaseUri;

    public Uri getBaseUri() {
        return mBaseUri;
    }

    public Map<String, String> getHeaders() {
        return mHeaders;
    }

    public RetryPolicy getRetryPolicy() {
        return mRetryPolicy;
    }

    public LobDynamic getDynamic() {
        return mDynamic;
    }

    public LobQueue getQueue() {
        return mQueue;
    }

    private Lob(Builder builder) {
        mContext = builder.mContext;
        mHeaders = builder.mHeaders;
        mRetryPolicy = builder.mRetryPolicy;
        mDynamic = builder.mDynamic;

        mEncoder = builder.mEncoder == null ? new GsonEncoder() : builder.mEncoder;

        mBaseUri = builder.mBaseUri;

        mQueue = builder.mQueue == null ? new SimpleQueue() : builder.mQueue;

        mQueue.prepare(mContext);
    }

    protected void submitRequest(ObjectRequest request) {
        Log.d("Lob", "request submitted: " + request.getUrl());
        if (request.getMethod() == Request.Method.GET) {
            Log.d("Lob", "params: ");
            Map<String, String> p = null;
            try {
                p = request.getParams();
                if (p != null) {
                    for (String key : p.keySet()) {
                        Log.d("Lob", "k=" + key + " v=" + p.get(key));
                    }
                }
            } catch (AuthFailureError authFailureError) {
                authFailureError.printStackTrace();
            }
        }

        mQueue.submitRequest(request);
    }

    public void cancel(Object tag) {
        mQueue.cancel(tag);
    }

    /**
     * Builder class for {@link Lob}
     */
    public static class Builder {
        private Context mContext;
        private Encoder mEncoder;
        private LobQueue mQueue;
        private Uri mBaseUri;

        private Map<String, String> mHeaders;
        private RetryPolicy mRetryPolicy;
        private LobDynamic mDynamic;


        // TODO: null check on context

        /**
         * Builder constructor
         *
         * @param context
         */
        public Builder(Context context, Uri baseUri) {
            mContext = context;
            mBaseUri = baseUri;
            mHeaders = new HashMap<String, String>();
            mHeaders.put(Constants.HEADER_USER_AGENT, Constants.DEFAULT_USER_AGENT);
        }

        public Builder setHeader(String name, String value) {
            mHeaders.put(name, value);
            return this;
        }

        public Builder setEncoder(Encoder encoder) {
            mEncoder = encoder;
            return this;
        }

        public Builder setQueue(LobQueue queue) {
            mQueue = queue;
            return this;
        }

        public Builder setRetryPolicy(RetryPolicy retryPolicy) {
            mRetryPolicy = retryPolicy;
            return this;
        }

        public Builder setDynamic(LobDynamic dynamic) {
            mDynamic = dynamic;
            return this;
        }

        /**
         * Build a Lob client
         *
         * @return A {@link Lob}
         */
        public Lob build() {
            return new Lob(this);
        }
    }
}
