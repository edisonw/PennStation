package com.edisonwang.ps.lib;

import android.os.Bundle;

import java.lang.ref.WeakReference;

/**
 * @author edi
 */
public class Requester {
    public long delay;
    private ActionRequest request;
    private ActionRequestHelper helper;
    private LimitedQueueInfo queue;

    public Requester(ActionRequest request) {
        this.request = request;
        this.helper = null;
    }

    public Requester(ActionRequestHelper request) {
        this.request = null;
        this.helper = request;
    }

    public Requester queue(LimitedQueueInfo queue) {
        this.queue = queue;
        return this;
    }

    public Requester queue(int limit, int priority, String tag) {
        this.queue = new LimitedQueueInfo(limit, priority, tag);
        return this;
    }

    public Requester delay(long delay) {
        this.delay = delay;
        return this;
    }

    public void request(EventManager eventManager, WeakReference<RequestListener> listener) {
        if (request == null) {
            request = helper.buildRequest();
        }
        eventManager.requestAction(request, queue, delay, listener);
    }

    public interface RequestListener {
        /**
         * Called right before the request is made.
         */
        void onRequested(Bundle bundle, String requestId);

        /**
         * Called right after the request results are posted.
         */
        void onCompleted(String reqId, ActionResult result);

        /**
         * Called when request was cancelled.
         */
        void onCancelled(String requestId);
    }
}
