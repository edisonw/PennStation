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
import android.os.Process;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author edi
 */
public class DefaultServiceImpl<T extends EventService> implements EventServiceImpl {

    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final T mService;
    private final int[] mTaskLock = new int[0];
    private final Bundle mBundle = new Bundle();
    private final HashMap<String, ExecutionRunnable> mSubmittedTasks = new HashMap<>();
    private final Messenger mMessenger = new Messenger(new EventServiceHandler<>(new WeakReference<EventServiceImpl>(this)));
    private ActionExecutor mExecutor;
    private LinkedHashMap<Integer, Boolean> mStartIds;

    public DefaultServiceImpl(T service) {
        mService = service;
    }

    public void onCreate() {
        mExecutor = new ActionExecutor();
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
        final EventServiceResponder responder;
        if (cb != null) {
            responder = cb.getResponder();
        } else {
            responder = null;
        }
        performRequest(new ExecutionRunnable(startId, bundle, responder, null));
        mStartIds.put(startId, false);
        return Service.START_STICKY;
    }

    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    @Override
    public Bundle getState() {
        return mBundle;
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

    @Override
    public T getContext() {
        return mService;
    }

    public void cancelRequest(Message msg) {
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

    public void performRequest(Message msg) {
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

    private class ResponderRunnable implements Runnable {
        private final EventServiceResponder mResponder;
        private final Bundle mBundle;
        private final int mStartId;
        private final boolean mCompleteSignal;

        public ResponderRunnable(EventServiceResponder responder, Bundle bundle, int startId, boolean completeSignal) {
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
        private final EventServiceResponder mResponder;
        private final String mRequestId;
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
        private boolean mCanceled;

        // Optionally either responder or messenger will be used to send response back to ui
        public ExecutionRunnable(int startId, Bundle bundle,
                                 EventServiceResponder responder, Messenger messenger) {
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
                ActionRequestEnv env = new ActionRequestEnv(mBundle, mService.getActionCacheFactory(), DefaultServiceImpl.this);
                event.process(mResultDeliver, env, true);
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
}
