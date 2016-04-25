package com.edisonwang.ps.lib;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.lang.ref.WeakReference;

import de.greenrobot.event.EventBus;

/**
 * @author edi
 */
public class EventManager {

    private final EventServiceConnection mServiceConnection;
    private final EventBus mBus;
    private final boolean mLogRequestStacks;
    private final Handler mHandler;

    EventManager(Context context, PennStation.PennStationOptions options) {
        mBus = new EventBus();
        final HandlerThread thread = new HandlerThread("EventManager");
        thread.start();
        mHandler = new Handler(thread.getLooper());
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
        return mServiceConnection.queueAndExecute(createServiceBundle(request));
    }

    public String requestAction(ActionRequest request, LimitedQueueInfo queueInfo) {
        return mServiceConnection.queueAndExecute(createServiceBundle(request), queueInfo, null);
    }

    private Bundle createServiceBundle(ActionRequest request) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(EventServiceImpl.EXTRA_SERVICE_REQUEST, request);
        if (mLogRequestStacks) {
            bundle.putString(EventServiceImpl.EXTRA_STACKTRACE_STRING,
                    Log.getStackTraceString(new Exception()));
        }
        return bundle;
    }

    protected void requestAction(final ActionRequest request,
                              final LimitedQueueInfo queueInfo,
                              long delay,
                              final WeakReference<Requester.RequestListener> listener) {
        if (delay > 0) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    requestAction(request, queueInfo, 0, listener);
                }
            }, delay);
        }
        mServiceConnection.queueAndExecute(createServiceBundle(request), queueInfo, listener);
    }

    private class EventServiceResponseHandler implements EventServiceImpl.EventServiceResponseHandler {

        @Override
        public void handleServiceResponse(Bundle b) {
            final String reqId = b.getString(EventServiceConnection.EXTRA_REQUEST_ID);

            if (reqId == null) {
                //The service requestAction was not made by an app controller.
                return;
            }

            boolean completeSignal = b.getBoolean(EventServiceImpl.EXTRA_SERVICE_COMPLETE_SIGNAL, true);

            ActionResult result = b.getParcelable(EventServiceImpl.EXTRA_SERVICE_RESULT);

            if (result != null) {
                result.setResponseInfo(new ResponseInfo(b));
                if (completeSignal) {
                    Requester.RequestListener listener = mServiceConnection.onComplete(reqId);
                    if (listener != null) {
                        listener.onCompleted(reqId, result);
                    }
                } else {
                    if (result.postSticky()) {
                        postLocalStickyEvent(result);
                    } else {
                        postLocalEvent(result);
                    }
                }
            }
        }
    }
}
