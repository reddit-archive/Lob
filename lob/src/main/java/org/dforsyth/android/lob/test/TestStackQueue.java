package org.dforsyth.android.lob.test;

import org.dforsyth.android.lob.queues.SimpleQueue;

/**
 * A queue that uses {@link TestStack}.
 */
public class TestStackQueue extends SimpleQueue {
    public TestStackQueue(TestEndpoint[] endpoint) {
        super(new TestStack(endpoint));
    }
}
