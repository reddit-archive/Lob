package org.dforsyth.android.ravioli.test;

import org.dforsyth.android.ravioli.queues.SimpleQueue;

/**
 * A queue that uses {@link TestStack}.
 */
public class TestStackQueue extends SimpleQueue {
    public TestStackQueue(TestEndpoint[] endpoint) {
        super(new TestStack(endpoint));
    }
}
