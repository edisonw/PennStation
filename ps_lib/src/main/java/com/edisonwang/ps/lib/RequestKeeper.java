package com.edisonwang.ps.lib;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Sample implementation for if you want to make sure to
 * only process a request made by a certain object.
 *
 * @author edi
 */
public class RequestKeeper {
    private final HashMap<Class<? extends Action>, String> mLastRequestIdByType = new HashMap<>();
    private final HashSet<String> mRequestIds = new HashSet<>();

    /**
     * Make a request via PennStation and keep track of the requestId.
     */
    public void addRequest(ActionRequest request) {
        String requestId = PennStation.requestAction(request);
        synchronized (this) {
            mLastRequestIdByType.put(request.type(), requestId);
            mRequestIds.add(requestId);
        }
    }

    /**
     * Cancel the last request of this type made by this keeper.
     * @param type the type of action that will get cancelled.
     */
    public void cancelPrevious(Class<? extends Action> type) {
        String previousId;
        synchronized (this) {
            previousId = mLastRequestIdByType.remove(type);
        }
        if (previousId != null) {
            PennStation.cancelAction(previousId);
        }
    }

    /**
     * Cancel the last request of this type made by this keeper and then add it to the queue.
     */
    public void addRequestAsReplacement(ActionRequest request) {
        cancelPrevious(request.type());
        addRequest(request);
    }

    /**
     * Required method to be called if you are using RequestKeeper to funnel your requests.
     *
     * @return true if this event was made via this request keeper.
     */
    public boolean onEvent(ActionResult result) {
        synchronized (this) {
            String requestId = result.getResponseInfo().mRequestId;
            if (requestId == null) {
                return false;
            }
            boolean eventRemoved = mRequestIds.remove(requestId);
            if (eventRemoved) {
                Class key = null;
                for (Class type: mLastRequestIdByType.keySet()) {
                    if (requestId.equals(mLastRequestIdByType.get(type))) {
                        key = type;
                        break;
                    }
                }
                if (key != null) {
                    mLastRequestIdByType.remove(key);
                }
            }
            return eventRemoved;
        }
    }

    public void addRequest(ActionRequestHelper helper) {
        addRequest(helper.buildRequest());
    }
}
