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

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import org.dforsyth.android.ravioli.encoders.DecodeError;
import org.dforsyth.android.ravioli.encoders.Encoder;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Decodable {@link com.android.volley.Request }
 */
public class ObjectRequest<T> extends Request<RavioliResponse<T>> {
    private final Type mType;
    private final Map<String, String> mHeaders;
    private final Response.Listener<RavioliResponse<T>> mListener;
    private final Encoder mEncoder;
    private final Map<String, String> mPostParams;
    private final RavioliDynamic mDynamic;
    private final byte[] mBody;

    /**
     * Make a GET request and return a parsed object from JSON.
     *
     * @param method
     * @param url URL of the request to make
     * @param encoder {@link org.dforsyth.android.ravioli.encoders.Encoder } for this request
     * @param type Relevant type object, for {@link org.dforsyth.android.ravioli.encoders.Encoder}
     * @param headers Map of request headers
     * @param postParams Map of post parameters
     * @param listener
     * @param errorListener
     */
    public ObjectRequest(
            int method,
            String url,
            Encoder encoder,
            RavioliDynamic dynamic,
            Type type,
            Map<String, String> headers,
            Map<String, String> postParams,
            byte[] body,
            Response.Listener<RavioliResponse<T>> listener,
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
    public byte[] getBody() {
        return mBody;
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

    public Response.Listener<RavioliResponse<T>> getListener() {
        return mListener;
    }

    @Override
    protected void deliverResponse(RavioliResponse<T> response) {
        mListener.onResponse(response);
    }

    @Override
    protected Response<RavioliResponse<T>> parseNetworkResponse(NetworkResponse response) {
        // TODO: check status code for errors and send raviolierrors

        try {
            String json = new String(
                    response.data,
                    HttpHeaderParser.parseCharset(response.headers));

            RavioliResponse<T> ravioliResponse = new RavioliResponse<T>(
                    response,
                    (T) mEncoder.decode(json, mType)
            );

            return Response.success(
                    ravioliResponse,
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (DecodeError e) {
            return Response.error(new ParseError(e));
        }
    }
}
