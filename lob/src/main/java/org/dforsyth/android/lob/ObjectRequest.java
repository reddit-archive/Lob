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

import android.net.Uri;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;

import org.dforsyth.android.lob.encoders.DecodeError;
import org.dforsyth.android.lob.encoders.Encoder;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Decodable {@link com.android.volley.Request }
 */
public class ObjectRequest<T> extends Request<LobResponse<T>> {
    private final Type mType;
    private final Map<String, String> mHeaders;
    private final Response.Listener<LobResponse<T>> mListener;
    private final Encoder mEncoder;
    private final Map<String, String> mPostParams;
    private final LobDynamic mDynamic;
    private final byte[] mBody;
    private String mBodyContentType;

    /**
     * Make a request and parse an object out of the response body
     *
     * @param method
     * @param url URL of the request to make
     * @param encoder {@link org.dforsyth.android.lob.encoders.Encoder } for this request
     * @param type Relevant type object, for {@link org.dforsyth.android.lob.encoders.Encoder}
     * @param headers Map of request headers
     * @param postParams Map of post parameters
     * @param listener
     * @param errorListener
     */
    public ObjectRequest(
            int method,
            String url,
            Encoder encoder,
            LobDynamic dynamic,
            Type type,
            Map<String, String> headers,
            Map<String, String> postParams,
            byte[] body,
            Response.Listener<LobResponse<T>> listener,
            Response.ErrorListener errorListener) {

        super(method, url, errorListener);

        // TODO: if clazz is null, then we actually dont want a decodable request, we just want a normal request
        mType = type;
        mHeaders = headers;
        mListener = listener;
        mPostParams = postParams;
        mBody = body;

        mDynamic = dynamic;

        mEncoder = encoder;
    }

    // TODO: don't do extra map allocation if there is no dynamic

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> headers = new HashMap<String, String>(mHeaders);
        if (mDynamic != null) {
            Map<String, String> dynamicHeaders = mDynamic.getDynamicHeaders();
            if (dynamicHeaders != null) {
                headers.putAll(dynamicHeaders);
            }

        }

        for (String k : headers.keySet()) {
            Log.d("headers", k + ": " + headers.get(k));
        }

        // here's a fun way to override something...
        if (headers.containsKey("Content-Type")) {
            mBodyContentType = headers.get("Content-Type");
        }

        return headers.size() > 0 ? headers : super.getHeaders();
    }

    @Override
    public Map<String, String> getParams() throws AuthFailureError {
        Map<String, String> params = new HashMap<String, String>(mPostParams);
        if (mDynamic != null) {
            Map<String, String> dynamicParams = mDynamic.getDynamicParams();
            if (dynamicParams != null) {
                params.putAll(dynamicParams);
            }
        }

        return params.size() > 0 ? params : super.getParams();
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        if (mBody != null) {
            return mBody;
        }

        return super.getBody();
    }

    @Override
    public String getUrl() {
        if (mDynamic != null) {
            Map<String, String> dynamicQueryParams = mDynamic.getDynamicQueryParams();
            if (dynamicQueryParams != null) {
                Uri.Builder builder = Uri.parse(super.getUrl()).buildUpon();
                for (String key : dynamicQueryParams.keySet()) {
                    builder.appendQueryParameter(key, dynamicQueryParams.get(key));
                }
                return builder.build().toString();
            }
        }

        return super.getUrl();
    }

    @Override
    public String getBodyContentType() {
        // return super.getBodyContentType();
        if (mBodyContentType != null) {
            return mBodyContentType;
        }

        return super.getBodyContentType();
    }

    public Response.Listener<LobResponse<T>> getListener() {
        return mListener;
    }

    @Override
    protected void deliverResponse(LobResponse<T> response) {
        mListener.onResponse(response);
    }

    @Override
    protected Response<LobResponse<T>> parseNetworkResponse(NetworkResponse response) {
        // TODO: check status code for errors and send loberrors

        String data;
        try {
            data = new String(
                    response.data,
                    HttpHeaderParser.parseCharset(response.headers));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        }

        Log.d("ObjectRequest", "url = " + getUrl() + " response = " + data);

        T decoded = null;
        if (mEncoder != null && mType != null) {
            try {
                decoded = mEncoder.decode(data, mType);
            } catch (DecodeError e) {
                return Response.error(new ParseError(e));
            }
        }

        LobResponse<T> lobResponse = new LobResponse<>(response, decoded);

        return Response.success(
                lobResponse,
                HttpHeaderParser.parseCacheHeaders(response)
        );
    }
}
