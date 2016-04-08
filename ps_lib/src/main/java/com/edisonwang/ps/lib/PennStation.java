package com.edisonwang.ps.lib;

import android.app.Application;

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
        public int pendingWarningThreshold;

        public PennStationOptions(Class<? extends EventService> eventServiceClass) {
            this.eventServiceClass = eventServiceClass;
        }
    }

    /**
     * You can call this inside Application.onCreate();
     *
     * This only needs to called once, call getManager() after.
     *
     * @param application Android Application Singleton.
     * @param options Options for PennStation.
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

    public static EventManager getManager() {
        if (sManager == null) {
            throw new IllegalStateException("You must call init() before using the instance. ");
        }
        return sManager;
    }

    public static void postLocalStickyEvent(Object object) {
        getManager().postLocalStickyEvent(object);
    }

    public static void postLocalEvent(Object object) {
        getManager().postLocalEvent(object);
    }

    public static void registerListener(Object object) {
        getManager().registerListener(object);
    }

    public static void unRegisterListener(Object object) {
        getManager().unRegisterListener(object);
    }

    public static String requestAction(ActionRequestHelper request) {
        return getManager().requestAction(request.buildRequest());
    }

    public static String requestAction(ActionRequest request) {
        return getManager().requestAction(request);
    }

    /**
     * Cancel a request that has not started running yet.
     * @param requestId the request id,
     */
    public static void cancelAction(String requestId) {
        getManager().cancel(requestId);
    }

    public <T> T getStickyEvent(Class<T> eventType) {
        return getManager().getStickyEvent(eventType);
    }

}
