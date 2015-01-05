package org.dforsyth.android.ravioli.test;

import android.net.Uri;

import com.android.volley.Request;

import org.apache.http.message.BasicStatusLine;

import java.util.Map;

/**
 * Match {@link org.dforsyth.android.ravioli.test.TestEndpoint} by path
 */
public class PathMatchEndpoint extends TestEndpoint {
    private String mPath;

    public PathMatchEndpoint(String path, BasicStatusLine statusLine, Map<String, String> headers, String body) {
        super(statusLine, headers, body);
        mPath = path;
    }

    @Override
    public boolean matchRequest(Request<?> request) {
        Uri uri = Uri.parse(request.getUrl());

        return uri.getPath().equals(mPath);
    }
}
