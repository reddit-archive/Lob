package org.dforsyth.android.lob.test;

import android.content.Context;
import android.content.res.Resources;

import com.android.volley.Request;

import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * Request handlers for {@link org.dforsyth.android.lob.test.TestStack}
 */
public abstract class TestEndpoint {
    BasicStatusLine statusLine;
    Map<String, String> headers;
    String body;

    public TestEndpoint(BasicStatusLine statusLine, Map<String, String> headers, String body) {
        this.statusLine = statusLine;
        this.headers = headers;
        this.body = body;
    }

    public abstract boolean matchRequest(Request<?> request);

    public HttpResponse prepareResponse(Request<?> request, Map<String, String> additionalHeaders) throws UnsupportedEncodingException {
        HttpResponse response = new BasicHttpResponse(statusLine);
        if (headers != null) {
            for (String headerKey : headers.keySet()) {
                response.addHeader(headerKey, headers.get(headerKey));
            }
        }
        response.setEntity(new StringEntity(body));
        return response;
    }

    public static String readRawResourceString(Context context, int resId) {
        Resources res = context.getResources();
        InputStream is = res.openRawResource(resId);

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = is.read(buf)) != -1) {
                os.write(buf, 0, len);
            }
            os.close();
            is.close();
        } catch (IOException e) {
            return null;
        }
        return os.toString();
    }
}