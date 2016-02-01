package com.edisonwang.ps.lib;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import de.greenrobot.event.EventBus;

/**
 * @author edi
 */
public class PennStation {

    private static EventManager sManager;

    private PennStation() {

    }

    public static class PennStationOptions {
        public final Class<? extends EventService> eventServiceClass;
        public boolean logRequestStacks;

        public PennStationOptions(Class<? extends EventService> eventServiceClass) {
            this.eventServiceClass = eventServiceClass;
        }
    }

    /**
     * You can call this inside Application.onCreate();
     * <p/>
     * This only needs to called once, call getInstance() after.
     *
     * @return the instance that was created for this process.
     */
    public static synchronized EventManager init(Application application,
                                                 PennStationOptions options) {
        if (sManager == null) {
            sManager = new EventManager(application, options);
        }
        return sManager;
    }

    public static EventManager getInstance() {
        if (sManager == null) {
            throw new IllegalStateException("You must call init() before using the instance. ");
        }
        return sManager;
    }

    public static void postLocalStickyEvent(Object object) {
        getInstance().postLocalStickyEvent(object);
    }

    public static void postLocalEvent(Object object) {
        getInstance().postLocalEvent(object);
    }

    public static void registerListener(Object object) {
        getInstance().registerListener(object);
    }

    public static void unRegisterListener(Object object) {
        getInstance().unRegisterListener(object);
    }

    public static String requestAction(ActionRequestHelper request) {
        return getInstance().requestAction(request.build());
    }

    public static String requestAction(ActionRequest request) {
        return getInstance().requestAction(request);
    }

    public <T> T getStickyEvent(Class<T> eventType) {
        return getInstance().getStickyEvent(eventType);
    }

    public static class EventManager {

        private final EventServiceImpl.EventServiceConnection mServiceConnection;
        private final EventBus mBus;
        private final PennStationOptions mOptions;

        private EventManager(Context context, PennStationOptions options) {
            mBus = new EventBus();
            mOptions = options;
            EventServiceImpl.EventServiceResponseHandler mServiceResponseHandler =
                    new EventServiceImpl.EventServiceResponseHandler() {
                        @Override
                        public void handleServiceResponse(Bundle b) {
                            final String reqId = b.getString(EventServiceImpl.EventServiceConnection.EXTRA_REQUEST_ID);

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
                    };
            mServiceConnection = new EventServiceImpl.EventServiceConnection(context, mServiceResponseHandler);
            context.bindService(new Intent(context, options.eventServiceClass), mServiceConnection,
                    Context.BIND_AUTO_CREATE);
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

        public String requestAction(ActionRequest request) {
            Bundle bundle = new Bundle();
            bundle.putParcelable(EventServiceImpl.EXTRA_SERVICE_REQUEST, request);
            if (mOptions.logRequestStacks) {
                bundle.putString(EventServiceImpl.EXTRA_STACKTRACE_STRING,
                        Log.getStackTraceString(new Exception()));
            }
            return mServiceConnection.queueAndExecute(bundle);
        }
    }
}
