package com.edisonwang.eventservice.lib;

import java.util.HashSet;

/**
 * Sample implementation for if you want to make sure to
 * only process a request made by a certain object.
 *
 * @author edi
 */
public class RequestKeeper {
    private final HashSet<String> mRequestIds = new HashSet<>();

    /**
     * Make a request via PennStation and keep track of the requestId.
     */
    public void addRequest(ActionRequest request) {
        mRequestIds.add(PennStation.requestAction(request));
    }

    /**
     * @return true if this event was made via this request keeper.
     */
    public boolean onEvent(ActionResult result) {
        return mRequestIds.remove(result.getResponseInfo().mRequestId);
    }
}
