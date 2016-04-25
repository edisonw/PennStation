package com.edisonwang.ps.lib;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Process;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author edi
 */
public class EventServiceImpl<T extends EventService> {

    public static final String TAG = "EventService";

    public static final String EXTRA_REQUEST_QUEUE_PRIORITY = "extra_request_priority";
    public static final String EXTRA_REQUEST_QUEUE_LIMIT = "extra_request_queue_limit";
    public static final String EXTRA_REQUEST_QUEUE_NEW_THREAD = "extra_request_queue_new_thread";
    public static final String EXTRA_REQUEST_QUEUE_TAG = "extra_request_queue_tag";

    public static final String EXTRA_SERVICE_REQUEST = "extra_service_request";
    public static final String EXTRA_SERVICE_RESULT = "extra_service_result";
    public static final String EXTRA_SERVICE_COMPLETE_SIGNAL = "extra_service_complete_signal";
    public static final String EXTRA_CALLBACK = "extra_callback";
    public static final String EXTRA_STACKTRACE_STRING = "extra_stack_trace_string";

    static final int PERFORM_REQUEST = 0;
    static final int CANCEL_REQUEST = 1;

    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final T mService;
    private ActionExecutor mExecutor;
    private final int[] mTaskLock = new int[0];
    private final HashMap<String, ExecutionRunnable> mSubmittedTasks = new HashMap<>();
    private final Messenger mMessenger = new Messenger(new EventServiceHandler(new WeakReference<EventServiceImpl>(this)));

    private LinkedHashMap<Integer, Boolean> mStartIds;

    public EventServiceImpl(T service) {
        mService = service;
    }

    void onCreate() {
        mExecutor = new ActionExecutor();
        mStartIds = new LinkedHashMap<>(50, 50);
    }

    int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.e(TAG, "Invalid intent sent to service. (null intent)");
            return Service.START_NOT_STICKY;
        }
        final Bundle bundle = intent.getExtras();
        if (bundle == null) {
            Log.e(TAG, "Invalid intent sent to service. (null bundle)");
            return Service.START_NOT_STICKY;
        }
        final EventServiceCallback cb = intent.getParcelableExtra(EXTRA_CALLBACK);
        final SpiralServiceResponder responder;
        if (cb != null) {
            responder = cb.getResponder();
        } else {
            responder = null;
        }
        performRequest(new ExecutionRunnable(startId, bundle, responder, null));
        mStartIds.put(startId, false);
        return Service.START_STICKY;
    }

    IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    void attemptStop(int startId) {
        mStartIds.put(startId, true);

        // This is an ordered list sorted by insertion.
        for (Map.Entry<Integer, Boolean> entry : mStartIds.entrySet()) {
            if (!entry.getValue()) {
                return;
            }
        }

        // Call stop self for each startId, then clear.
        for (Integer key : mStartIds.keySet()) {
            mService.stopSelf(key);
        }
        mStartIds.clear();
    }

    public T getContext() {
        return mService;
    }

    public interface EventServiceResponseHandler {
        void handleServiceResponse(Bundle b);
    }

    public static abstract class SpiralServiceResponder extends Binder {
        public abstract void onServiceResponse(Bundle bundle);
    }

    static class EventServiceCallback implements Parcelable {

        public static final Creator<EventServiceCallback> CREATOR =
                new Creator<EventServiceCallback>() {
                    public EventServiceCallback createFromParcel(Parcel source) {
                        return new EventServiceCallback(source);
                    }

                    public EventServiceCallback[] newArray(int size) {
                        return new EventServiceCallback[size];
                    }
                };

        private final SpiralServiceResponder mResponder;

        /**
         * Construct a callback object from an interface.
         * This constructor is used by the service consumer.
         *
         * @param responder The callback interface
         */
        public EventServiceCallback(SpiralServiceResponder responder) {
            mResponder = responder;
        }

        /**
         * Constructs a callback from a parcel.
         *
         * @param parcel The parcel from which to buildRequest the callback
         */
        public EventServiceCallback(Parcel parcel) {
            mResponder = (SpiralServiceResponder) parcel.readStrongBinder();
        }

        public SpiralServiceResponder getResponder() {
            return mResponder;
        }

        /**
         * The operation complete callback
         *
         * @param bundle The output bundle
         */
        public void onServiceResponse(Bundle bundle) {
            mResponder.onServiceResponse(bundle);
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeStrongBinder(mResponder);
        }

    }

    private class ResponderRunnable implements Runnable {
        private final SpiralServiceResponder mResponder;
        private final Bundle mBundle;
        private final int mStartId;
        private final boolean mCompleteSignal;

        public ResponderRunnable(SpiralServiceResponder responder, Bundle bundle, int startId, boolean completeSignal) {
            mResponder = responder;
            mBundle = bundle;
            mStartId = startId;
            mCompleteSignal = completeSignal;
        }

        public void run() {
            if (mResponder != null) {
                mResponder.onServiceResponse(mBundle);
            }
            if (mStartId > 0 && mCompleteSignal) {
                attemptStop(mStartId);
            }
        }
    }

    private class MessengerResponderRunnable implements Runnable {
        private final Messenger mResponder;
        private final Bundle mBundle;
        private final int mStartId;
        private final boolean mCompleteSignal;

        public MessengerResponderRunnable(Messenger responder, Bundle bundle, int startId, boolean completeSignal) {
            mResponder = responder;
            mBundle = bundle;
            mStartId = startId;
            mCompleteSignal = completeSignal;
        }

        public void run() {
            if (mResponder != null) {
                Message msg = new Message();
                Bundle b = msg.getData();
                b.putAll(mBundle);
                try {
                    mResponder.send(msg);
                } catch (Exception e) {
                    Log.e(TAG, "Error sending service response.", e);
                }
            }
            if (mStartId > 0 && mCompleteSignal) {
                attemptStop(mStartId);
            }
        }
    }

    private class ExecutionRunnable implements Runnable {
        private final int mStartId;
        private final Bundle mBundle;
        private final Messenger mMessenger;
        private final SpiralServiceResponder mResponder;
        private final String mRequestId;

        private boolean mCanceled;
        private final ResultDeliver mResultDeliver = new ResultDeliver() {
            @Override
            public void deliverResult(ActionResult result, boolean completeSignal) {
                final Bundle bundle = new Bundle(mBundle);
                if (result != null) {
                    bundle.putParcelable(EXTRA_SERVICE_RESULT, result);
                }

                bundle.putBoolean(EXTRA_SERVICE_COMPLETE_SIGNAL, completeSignal);

                final Runnable responderRunnable;
                if (mResponder != null) {
                    responderRunnable = new ResponderRunnable(mResponder, bundle, mStartId, completeSignal);
                } else if (mMessenger != null) {
                    responderRunnable = new MessengerResponderRunnable(mMessenger, bundle, mStartId, completeSignal);
                } else {
                    responderRunnable = null;
                }

                if (responderRunnable != null) {
                    mMainHandler.post(responderRunnable);
                }
            }
        };
        // Optionally either responder or messenger will be used to send response back to ui
        public ExecutionRunnable(int startId, Bundle bundle,
                                 SpiralServiceResponder responder, Messenger messenger) {
            mStartId = startId;
            bundle.setClassLoader(ActionRequest.class.getClassLoader());
            mRequestId = bundle.getString(EventServiceConnection.EXTRA_REQUEST_ID);
            mBundle = bundle;
            mResponder = responder;
            mMessenger = messenger;
        }

        public void run() {
            if (canceled()) {
                Log.d(TAG, "Task " + mRequestId + " was not executed.");
                return;
            }
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            ActionRequest event = mBundle.getParcelable(EXTRA_SERVICE_REQUEST);
            if (event != null) {
                event.process(mResultDeliver, EventServiceImpl.this, new ActionRequestEnv(mBundle, mService.getActionCacheFactory()), null);
            } else {
                Log.w(TAG, "Nothing was done in " + mRequestId);
            }
            //TODO
            if (mRequestId != null) {
                synchronized (mTaskLock) {
                    Log.d(TAG, "Task " + mRequestId + " was completed.");
                    mSubmittedTasks.remove(mRequestId);
                }
            }
        }

        public boolean canceled() {
            synchronized (mTaskLock) {
                return mCanceled;
            }
        }

        public void setCanceled(boolean canceled) {
            mCanceled = canceled;
        }

        public Bundle getBundle() {
            return mBundle;
        }
    }

    private void cancelRequest(Message msg) {
        Bundle data = msg.getData();
        String reqId = data.getString(EventServiceConnection.EXTRA_REQUEST_ID);
        synchronized (mTaskLock) {
            ExecutionRunnable runningTask = mSubmittedTasks.remove(reqId);
            if (runningTask != null) {
                Log.i(TAG, "Request cancelled." + reqId);
                runningTask.setCanceled(true);
            }
        }
    }

    private void performRequest(Message msg) {
        performRequest(new ExecutionRunnable(0, msg.getData(), null, msg.replyTo));
    }

    private void performRequest(ExecutionRunnable task) {
        final Bundle data = task.getBundle();
        data.setClassLoader(mService.getClassLoader());
        if (task.mRequestId != null) {
            synchronized (mTaskLock) {
                mSubmittedTasks.put(task.mRequestId, task);
            }
        }
        if (data.getBoolean(EventServiceImpl.EXTRA_REQUEST_QUEUE_NEW_THREAD, true)) {
            mExecutor.executeOnNewThread(task);
        } else {
            final int queueLimit = data.getInt(EventServiceImpl.EXTRA_REQUEST_QUEUE_LIMIT, 2);
            final String tag = data.getString(EventServiceImpl.EXTRA_REQUEST_QUEUE_TAG);
            final String queueTag = tag != null ? tag : ActionExecutor.DEFAULT;
            final int queuePriority = data.getInt(EventServiceImpl.EXTRA_REQUEST_QUEUE_PRIORITY, 0);
            mExecutor.execute(task, queueLimit, queueTag, queuePriority);
        }
    }

    private static class EventServiceHandler extends Handler {

        private final WeakReference<EventServiceImpl> mServiceImpl;

        private EventServiceHandler(WeakReference<EventServiceImpl> serviceImpl) {
            mServiceImpl = serviceImpl;
        }

        @Override
        public void handleMessage(Message msg) {
            EventServiceImpl serviceImpl = mServiceImpl.get();
            if (serviceImpl != null) {
                switch (msg.what) {
                    case PERFORM_REQUEST:
                        serviceImpl.performRequest(msg);
                        break;
                    case CANCEL_REQUEST:
                        serviceImpl.cancelRequest(msg);
                        break;
                }
            } else {
                Log.e(TAG, "ServiceImpl is already dead.");
            }
        }
    }
}
