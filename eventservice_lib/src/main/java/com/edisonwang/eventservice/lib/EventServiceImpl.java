package com.edisonwang.eventservice.lib;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author edi
 */
public class EventServiceImpl<T extends Service> {

    public static final String TAG = "EventService";

    public static final String EXTRA_SERVICE_REQUEST= "extra_service_request";
    public static final String EXTRA_SERVICE_RESULT = "extra_service_result";
    public static final String EXTRA_CALLBACK = "extra_callback";

    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final T mService;
    private ExecutorService mExecutor;
    private LinkedHashMap<Integer, Boolean> mStartIds;
    private final Messenger mMessenger = new Messenger(new Handler() {

        @Override
        public void handleMessage(Message msg) {
            msg.getData().setClassLoader(mService.getClassLoader());
            mExecutor.execute(new ExecutionRunnable(0, msg.getData(), null, msg.replyTo));
        }
    });

    public interface EventServiceResponseHandler {
        void handleServiceResponse(Bundle b);
    }

    public EventServiceImpl(T service) {
        mService = service;
    }

    public void onCreate() {
        mExecutor = Executors.newCachedThreadPool();
        mStartIds = new LinkedHashMap<>(50, 50);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
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
        mExecutor.execute(new ExecutionRunnable(startId, bundle, responder, null));
        mStartIds.put(startId, false);
        return Service.START_STICKY;
    }

    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    private class ResponderRunnable implements Runnable {
        private final SpiralServiceResponder mResponder;
        private final Bundle mBundle;
        private final int mStartId;

        public ResponderRunnable(SpiralServiceResponder responder, Bundle bundle, int startId) {
            mResponder = responder;
            mBundle = bundle;
            mStartId = startId;
        }

        public void run() {
            if (mResponder != null) {
                mResponder.onServiceResponse(mBundle);
            }
            if (mStartId > 0) {
                attemptStop(mStartId);
            }
        }
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

    private class MessengerResponderRunnable implements Runnable {
        private final Messenger mResponder;
        private final Bundle mBundle;
        private final int mStartId;

        public MessengerResponderRunnable(Messenger responder, Bundle bundle, int startId) {
            mResponder = responder;
            mBundle = bundle;
            mStartId = startId;
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
            if (mStartId > 0) {
                attemptStop(mStartId);
            }
        }
    }

    public T getService() {
        return mService;
    }

    public static abstract class SpiralServiceResponder extends Binder {
        public abstract void onServiceResponse(Bundle bundle);
    }

    private class ExecutionRunnable implements Runnable {
        private final int mStartId;
        private final Bundle mBundle;
        private final Messenger mMessenger;
        private final SpiralServiceResponder mResponder;

        // Optionally either responder or messenger will be used to send response back to ui
        public ExecutionRunnable(int startId, Bundle bundle,
                                 SpiralServiceResponder responder, Messenger messenger) {
            mStartId = startId;
            mBundle = bundle;
            mResponder = responder;
            mMessenger = messenger;
        }

        public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            ActionRequest event = mBundle.getParcelable(EXTRA_SERVICE_REQUEST);
            ActionResult result = null;
            if (event != null) {
                result = event.process(EventServiceImpl.this);
            }
            if (result != null) {
                mBundle.putParcelable(EXTRA_SERVICE_RESULT, result);
            }

            final Runnable responderRunnable;
            if (mResponder != null) {
                responderRunnable = new ResponderRunnable(mResponder, mBundle, mStartId);
            } else if (mMessenger != null) {
                responderRunnable = new MessengerResponderRunnable(mMessenger, mBundle, mStartId);
            } else {
                responderRunnable = null;
            }

            if (responderRunnable != null) {
                mMainHandler.post(responderRunnable);
            }
        }
    }

    public static class EventServiceCallback implements Parcelable {

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
         * @param parcel The parcel from which to build the callback
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

    public static class EventServiceConnection implements ServiceConnection {

        public static final String EXTRA_REQUEST_ID = "connection_request_id";
        public static final String EXTRA_REQUEST_TIME_MS = "connection_request_time";

        private Messenger mService;
        private final HashMap<String, Bundle> mPendingQueue = new HashMap<>();
        private final HashMap<String, Bundle> mRequestQueue = new HashMap<>();

        private final int[] mLock = {};
        private final Context mContext;
        private final EventServiceResponseHandler mResponseHandler;
        private Messenger mResponder;

        public EventServiceConnection(Context context, EventServiceResponseHandler handler) {
            mContext = context;
            mResponseHandler = handler;
        }

        public String queueAndExecute(Bundle bundle) {
            final String reqId = generateRequestId();
            bundle.putString(EXTRA_REQUEST_ID, reqId);
            bundle.putLong(EXTRA_REQUEST_TIME_MS, System.currentTimeMillis());
            synchronized (mLock) {
                Messenger service = mService;
                if (service != null) {
                    mRequestQueue.put(reqId, bundle);
                    sendMessage(service, createMessage(bundle));
                } else {
                    mPendingQueue.put(reqId, bundle);
                }
            }
            return reqId;
        }

        public void cancelAll() {
            synchronized (mLock) {
                mRequestQueue.clear();
                mPendingQueue.clear();
            }
        }

        private Message createMessage(Bundle bundle) {
            final Message msg = Message.obtain();
            msg.setData(bundle);
            msg.replyTo = getServiceResponder();
            return msg;
        }

        private synchronized Messenger getServiceResponder() {
            if (mResponder == null) {
                mResponder = new Messenger(new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        Bundle bundle = null;
                        try {
                            Bundle b = msg.getData();
                            b.setClassLoader(mContext.getClassLoader());
                            bundle = b;
                        } catch (Throwable e) {
                            //Bundling error during IPC. Ignore.
                        }
                        mResponseHandler.handleServiceResponse(bundle);
                    }
                });
            }
            return mResponder;
        }

        private void sendMessage(Messenger service, Message msg) {
            try {
                service.send(msg);
            } catch (Throwable e) {
                Log.e(TAG, "Unable to send message to service.", e);
            }
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            synchronized (mLock) {
                Messenger service = new Messenger(binder);
                mService = service;
                mRequestQueue.putAll(mPendingQueue);
                for (Bundle action : mPendingQueue.values()) {
                    sendMessage(service, createMessage(action));
                }
                mPendingQueue.clear();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            synchronized (mLock) {
                mService = null;
            }
        }

        public boolean isPending(String requestId) {
            synchronized (mLock) {
                return mRequestQueue.containsKey(requestId);
            }
        }

        public Bundle remove(String requestId) {
            synchronized (mLock) {
                Bundle action = mRequestQueue.get(requestId);
                mRequestQueue.remove(requestId);
                return action;
            }
        }

        public String generateRequestId() {
            //Generate a six letter requestAction ID.
            return Long.toHexString(Double.doubleToLongBits(Math.random()));
        }
    }
}
