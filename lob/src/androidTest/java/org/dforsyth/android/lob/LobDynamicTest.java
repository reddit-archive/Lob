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
import android.test.AndroidTestCase;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;

import org.dforsyth.android.lob.queues.LobQueue;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Tests for {@link LobDynamic}
 */
public class LobDynamicTest extends AndroidTestCase {
    private class TestQueue implements LobQueue {
        private LinkedList<Request<?>> ll;

        @Override
        public void prepare(Context context) {
            ll = new LinkedList<>();
        }

        @Override
        public void submitRequest(Request<?> request) {
            ll.add(request);
        }

        @Override
        public void cancel(Object tag) {
        }

        @Override
        public RequestQueue getRequestQueue() {
            return null;
        }

        public Request<?> pop() {
            return ll.removeFirst();
        }
    }

    Lob client;
    TestQueue q;

    @Override
    protected void setUp() throws Exception {
        client = new Lob.Builder(
                getContext(),
                Uri.parse("https://www.google.com")
        )
        .setDynamic(new LobDynamic() {
            @Override
            public Map<String, String> getDynamicHeaders() {
                Map<String, String> map = new HashMap<String, String>();
                map.put("extraHeader", "headerExtra");
                return map;
            }

            @Override
            public Map<String, String> getDynamicParams() {
                Map<String, String> map = new HashMap<String, String>();
                map.put("extraParam", "paramExtra");
                return map;
            }

            @Override
            public Map<String, String> getDynamicQueryParams() {
                Map<String, String> map = new HashMap<String, String>();
                map.put("queryParamExtra", "extraParamQuery");
                return map;
            }
        })
        .setQueue(q = new TestQueue())
        .build();
    }

    public void testDynamicHeaders() {
        LobRequest<Object> request = new LobRequest.Builder<Object>(
                client,
                Object.class
        )
        .addHeader("normal", "normal")
        .build();

        Object tag = new Object();

        request.requestAsync(tag, null);

        ObjectRequest or = (ObjectRequest) q.pop();
        assertEquals(tag, or.getTag());

        Map<String, String> headers = null;
        try {
            headers = or.getHeaders();
        } catch (AuthFailureError authFailureError) {
            assertFalse(true);
        }

        assertEquals("normal", headers.get("normal"));
        assertEquals("headerExtra", headers.get("extraHeader"));
    }

    public void testDynamicPostParams() {
        LobRequest<Object> request = new LobRequest.Builder<Object>(
                client,
                Object.class
        )
        .setMethod(Request.Method.POST)
        .addParam("normal", "normal")
        .build();

        Object tag = new Object();

        request.requestAsync(tag, null);

        ObjectRequest or = (ObjectRequest) q.pop();
        assertEquals(tag, or.getTag());

        Map<String, String> params = null;
        try {
            params = or.getParams();
        } catch (AuthFailureError authFailureError) {
            authFailureError.printStackTrace();
            assertFalse(true);
        }

        assertEquals("normal", params.get("normal"));
        assertEquals("paramExtra", params.get("extraParam"));
    }

    public void testDynamicQueryParams() {
        LobRequest<Object> request = new LobRequest.Builder<Object>(
                client,
                Object.class
        )
        .addQueryParameter("normal", "normal")
        .build();

        Object tag = new Object();

        request.requestAsync(tag, null);

        ObjectRequest or = (ObjectRequest) q.pop();
        assertEquals(tag, or.getTag());

        String url = or.getUrl();
        assertTrue(url.contains("normal=normal"));
        assertTrue(url.contains("queryParamExtra=extraParamQuery"));
    }
}
