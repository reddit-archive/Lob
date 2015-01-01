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
import android.test.AndroidTestCase;

import com.android.volley.AuthFailureError;
import com.android.volley.Request.Method;

import org.dforsyth.android.ravioli.util.Constants;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;

/**
 * Ravioli Request tests
 */
public class RavioliRequestTest extends AndroidTestCase {

    private Ravioli client;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        client = new Ravioli.Builder(
                getContext(),
                Uri.parse("https://www.google.com")
        ).build();
    }

    public void testUri() {
        RavioliRequest<Object> request = new RavioliRequest.Builder<Object>(
                client,
                Object.class
        )
        .addPath("extra/path")
        .build();

        // we trust uri, we could probably just test toString
        Uri uri = request.getUri();

        assertEquals(uri.getScheme(), "https");
        assertEquals(uri.getAuthority(), "www.google.com");
        assertEquals(uri.getPath(), "/extra/path");
    }

    public void testDefaultMethod() {
        RavioliRequest<Object> request = new RavioliRequest.Builder<Object>(
                client,
                Object.class
        ).build();

        assertEquals(request.getMethod(), RavioliRequest.DEFAULT_METHOD);
    }

    public void testOverrideMethod() {
        int TEST_METHOD = RavioliRequest.DEFAULT_METHOD + 1;

        RavioliRequest<Object> RavioliRequest = new RavioliRequest.Builder<Object>(
                client,
                Object.class
        )
        .setMethod(TEST_METHOD)
        .build();

        assertEquals(RavioliRequest.getMethod(), TEST_METHOD);
    }

    public void
    testPostParameters() {
        RavioliRequest<Object> request = new RavioliRequest.Builder<Object>(
                client,
                Object.class
        )
        .setMethod(Method.POST)
        .addParam("key", "value")
        .build();

        Map<String, String> params = request.getParams();

        assertEquals(params.size(), 1);
        assertEquals(params.get("key"), "value");
    }

    public void testQueryParameters() {
        RavioliRequest<Object> request = new RavioliRequest.Builder<Object>(
                client,
                Object.class
        )
        .setMethod(Method.GET)
        .addQueryParameter("key", "value")
        .addQueryParameter("needs_escape", "<>")
        .build();

        Uri uri = request.getUri();

        assertEquals(uri.getQueryParameter("key"), "value");
        assertEquals(uri.getQueryParameter("needs_escape"), "<>");

        String asString = uri.toString();

        assertEquals(asString, "https://www.google.com?key=value&needs_escape=%3C%3E");
    }

    public void testPostParameterGetRequest() {
        RavioliRequest.Builder<Object> builder = new RavioliRequest.Builder<Object>(
                client,
                Object.class
        )
        .addParam("key", "value")
        .setMethod(Method.GET);

        try {
            builder.build();
            assertTrue(false);
        } catch (IllegalArgumentException e) {
            assertEquals(
                    e.getMessage(),
                    "Parameters in non-param request"
            );
        }
    }

    public void testHeaders() {
        RavioliRequest<Object> request = new RavioliRequest.Builder<Object>(
                client,
                Object.class
        )
        .addHeader("key", "value")
        .build();

        Map<String, String> headers = request.getHeaders();

        assertEquals(headers.size(), 2);
        assertEquals(headers.get("key"), "value");
        assertEquals(headers.get(Constants.HEADER_USER_AGENT), Constants.DEFAULT_USER_AGENT);
    }

    public void testExplicitBody() {
        byte[] TEST_BYTES = "something".getBytes();

        RavioliRequest<Object> request = new RavioliRequest.Builder<Object>(
                client,
                Object.class
        )
        .setMethod(Method.POST)
        .setBody(TEST_BYTES)
        .build();

        ObjectRequest<Object> objectRequest = request.createRequest(null, null, null);

        try {
            assertEquals(TEST_BYTES, objectRequest.getBody());
        } catch (AuthFailureError authFailureError) {
            authFailureError.printStackTrace();
            assertTrue(false);
        }
    }

    public void testParamBody() {

        RavioliRequest<Object> request = new RavioliRequest.Builder<Object>(
                client,
                Object.class
        )
        .setMethod(Method.POST)
        .addParam("test", "param")
        .build();

        ObjectRequest<Object> objectRequest = request.createRequest(null, null, null);

        try {
            assertTrue(
                    Arrays.equals(
                            "test=param&".getBytes(Charset.forName("UTF-8")),
                            objectRequest.getBody()
                    )
            );
        } catch (AuthFailureError authFailureError) {
            authFailureError.printStackTrace();
            assertTrue(false);
        }
    }
}
