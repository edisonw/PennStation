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
        /**
         * The event service class that actions will be running on.
         */
        public final Class<? extends EventService> eventServiceClass;

        /**
         * If null, events will always run on new threads, if set, it will be used as default.
         */
        public LimitedQueueInfo defaultUseLimitedQueueInfo;

        /**
         * If true, requests stacks will be logged, and have a slight performance reduction.
         */
        public boolean logRequestStacks;

        /**
         * If true, if there are too many requests pending in the global queue, logs will be emitted.
         */
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
        return requestAction(request, null);
    }

    public static String requestAction(ActionRequestHelper request, LimitedQueueInfo queueInfo) {
        return getManager().requestAction(request.buildRequest(), queueInfo);
    }

    public static String requestAction(ActionRequest request, LimitedQueueInfo queueInfo) {
        return getManager().requestAction(request, queueInfo);
    }

    public static String requestAction(ActionRequest request) {
        return requestAction(request, null);
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
