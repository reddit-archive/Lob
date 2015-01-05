package org.dforsyth.android.ravioli.test;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.HurlStack;

import org.apache.http.HttpResponse;

import java.io.IOException;
import java.util.Map;

/**
 * A HurlStack for faking endpoints
 */
public class TestStack extends HurlStack {


    TestEndpoint[] mEndpoints;

    public TestStack(TestEndpoint[] endpoints) {
        mEndpoints = endpoints;
    }

    @Override
    public HttpResponse performRequest(Request<?> request, Map<String, String> additionalHeaders) throws IOException, AuthFailureError {
        for (TestEndpoint endpoint : mEndpoints) {
            if (endpoint.matchRequest(request)) {
                return endpoint.prepareResponse(request, additionalHeaders);
            }
        }

        throw new IOException("MockAuthStack: Could not complete request.");
    }
}
