package com.edisonwang.ps.lib;

import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;

/**
 * @author edi
 */
public interface EventServiceImpl {
    int PERFORM_REQUEST = 0;
    int CANCEL_REQUEST = 1;
    String TAG = "EventServiceImpl";
    String EXTRA_REQUEST_QUEUE_PRIORITY = "extra_request_priority";
    String EXTRA_REQUEST_QUEUE_LIMIT = "extra_request_queue_limit";
    String EXTRA_REQUEST_QUEUE_NEW_THREAD = "extra_request_queue_new_thread";
    String EXTRA_REQUEST_QUEUE_TAG = "extra_request_queue_tag";
    String EXTRA_SERVICE_REQUEST = "extra_service_request";
    String EXTRA_SERVICE_RESULT = "extra_service_result";
    String EXTRA_SERVICE_COMPLETE_SIGNAL = "extra_service_complete_signal";
    String EXTRA_CALLBACK = "extra_callback";
    String EXTRA_STACKTRACE_STRING = "extra_stack_trace_string";

    int onStartCommand(Intent intent, int flags, int startId);

    void onCreate();

    IBinder onBind(Intent intent);

    void performRequest(Message msg);

    void cancelRequest(Message msg);

    interface EventServiceResponseHandler {
        void handleServiceResponse(Bundle b);
    }

    abstract class EventServiceResponder extends Binder {
        public abstract void onServiceResponse(Bundle bundle);
    }

    Context getContext();

    Bundle getState();
}
