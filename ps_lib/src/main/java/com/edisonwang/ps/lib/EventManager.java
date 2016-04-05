package com.edisonwang.ps.lib;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import de.greenrobot.event.EventBus;

/**
 * @author edi
 */
public class EventManager {

    private final EventServiceConnection mServiceConnection;
    private final EventBus mBus;
    private final boolean mLogRequestStacks;

    EventManager(Context context, PennStation.PennStationOptions options) {
        mBus = new EventBus();
        mLogRequestStacks = options.logRequestStacks;
        mServiceConnection = new EventServiceConnection(context, new EventServiceResponseHandler(), options);
        context.bindService(new Intent(context, options.eventServiceClass), mServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    public void cancel(String reqId) {
        mServiceConnection.cancel(reqId);
    }

    public void postLocalStickyEvent(Object object) {
        mBus.postSticky(object);
    }

    public void postLocalEvent(Object object) {
        mBus.post(object);
    }

    public void registerListener(Object object) {
        mBus.register(object);
    }

    public void unRegisterListener(Object object) {
        mBus.unregister(object);
    }

    public EventBus getEventBus() {
        return mBus;
    }

    public <T> T getStickyEvent(Class<T> eventType) {
        return mBus.getStickyEvent(eventType);
    }

    public String requestAction(ActionRequestHelper helper) {
        return requestAction(helper.buildRequest());
    }

    public String requestAction(ActionRequest request) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(EventServiceImpl.EXTRA_SERVICE_REQUEST, request);
        if (mLogRequestStacks) {
            bundle.putString(EventServiceImpl.EXTRA_STACKTRACE_STRING,
                    Log.getStackTraceString(new Exception()));
        }
        return mServiceConnection.queueAndExecute(bundle);
    }

    private class EventServiceResponseHandler implements EventServiceImpl.EventServiceResponseHandler {
        @Override
        public void handleServiceResponse(Bundle b) {
            final String reqId = b.getString(EventServiceConnection.EXTRA_REQUEST_ID);

            if (reqId == null) {
                //The service requestAction was not made by an app controller.
                return;
            }

            mServiceConnection.remove(reqId);
            ActionResult result = b.getParcelable(EventServiceImpl.EXTRA_SERVICE_RESULT);

            if (result != null) {
                result.setResponseInfo(new ResponseInfo(b));
                if (result.postSticky()) {
                    postLocalStickyEvent(result);
                } else {
                    postLocalEvent(result);
                }
            }
        }
    }
}
