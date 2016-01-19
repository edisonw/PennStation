package com.edisonwang.ps.lib;

import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import de.greenrobot.event.EventBus;

/**
 * @author edi
 */
public class PennStation {

    private PennStation() {

    }

    private static EventManager sManager;

    /**
     * You can call this inside Application.onCreate();
     * <p/>
     * This only needs to called once, call getInstance() after.
     *
     * @return the instance that was created for this process.
     */
    public static synchronized EventManager init(Application application,
                                                 Class<? extends EventService> eventServiceClass) {
        if (sManager == null) {
            sManager = new EventManager(application, eventServiceClass);
        }
        return sManager;
    }

    public static EventManager getInstance() {
        if (sManager == null) {
            throw new IllegalStateException("You must call init() before using the instance. ");
        }
        return sManager;
    }

    public static void postStickyEvent(Object object) {
        getInstance().postStickyEvent(object);
    }

    public static void postEvent(Object object) {
        getInstance().postEvent(object);
    }

    public static void registerListener(Object object) {
        getInstance().registerListener(object);
    }

    public static void unRegisterListener(Object object) {
        getInstance().unRegisterListener(object);
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

        private EventManager(Context context, Class<? extends Service> eventServiceClass) {
            mBus = new EventBus();
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
                                    postStickyEvent(result);
                                } else {
                                    postEvent(result);
                                }
                            }
                        }
                    };
            mServiceConnection = new EventServiceImpl.EventServiceConnection(context, mServiceResponseHandler);
            context.bindService(new Intent(context, eventServiceClass), mServiceConnection,
                    Context.BIND_AUTO_CREATE);
        }

        public void postStickyEvent(Object object) {
            mBus.postSticky(object);
        }

        public void postEvent(Object object) {
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
            return mServiceConnection.queueAndExecute(bundle);
        }
    }
}
