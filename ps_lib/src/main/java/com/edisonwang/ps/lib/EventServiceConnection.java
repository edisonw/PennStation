package com.edisonwang.ps.lib;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.HashMap;

/**
 * @author edi
 */
class EventServiceConnection implements ServiceConnection {

    public static final String EXTRA_REQUEST_ID = "connection_request_id";
    public static final String EXTRA_REQUEST_TIME_MS = "connection_request_time";
    private final HashMap<String, Bundle> mPendingQueue = new HashMap<>();
    private final HashMap<String, Bundle> mRequestQueue = new HashMap<>();
    private final HashMap<String, WeakReference<Requester.RequestListener>> mListeners;
    private final int[] mLock = {};
    private final Context mContext;
    private final EventServiceImpl.EventServiceResponseHandler mResponseHandler;
    private final int mPendingWarningThreshold;
    private final LimitedQueueInfo mDefaultQueueInfo;
    private Messenger mService;
    private Messenger mResponder;
    private boolean mPendingThresholdWarned;

    public EventServiceConnection(Context context,
                                  EventServiceImpl.EventServiceResponseHandler handler,
                                  PennStation.PennStationOptions options) {
        mContext = context;
        mResponseHandler = handler;
        mListeners = new HashMap<>();
        mPendingWarningThreshold = options.pendingWarningThreshold;
        mDefaultQueueInfo = options.defaultUseLimitedQueueInfo;
    }

    public String queueAndExecute(Bundle bundle) {
        return queueAndExecute(bundle, mDefaultQueueInfo, null);
    }

    public String queueAndExecute(Bundle bundle, LimitedQueueInfo queueInfo, WeakReference<Requester.RequestListener> lisRef) {
        if (queueInfo != null) {
            bundle.putBoolean(EventServiceImpl.EXTRA_REQUEST_QUEUE_NEW_THREAD, false);
            bundle.putInt(EventServiceImpl.EXTRA_REQUEST_QUEUE_PRIORITY, queueInfo.priority);
            bundle.putString(EventServiceImpl.EXTRA_REQUEST_QUEUE_TAG, queueInfo.tag);
            bundle.putInt(EventServiceImpl.EXTRA_REQUEST_QUEUE_LIMIT, queueInfo.limit);
        } else {
            bundle.putBoolean(EventServiceImpl.EXTRA_REQUEST_QUEUE_NEW_THREAD, true);
        }
        final String reqId = generateRequestId();
        bundle.putString(EXTRA_REQUEST_ID, reqId);
        bundle.putLong(EXTRA_REQUEST_TIME_MS, System.currentTimeMillis());
        final Requester.RequestListener listener;
        if (lisRef != null) {
            listener = lisRef.get();
            if (listener != null) {
                listener.onRequested(bundle, reqId);
            }
        }
        synchronized (mLock) {
            if (lisRef != null) {
                mListeners.put(reqId, lisRef);
            }
            Messenger service = mService;
            if (service != null) {
                mRequestQueue.put(reqId, bundle);
                final int size = mRequestQueue.size();
                if (mPendingWarningThreshold > 0 &&
                        size >= mPendingWarningThreshold) {
                    PennStation.getManager().getEventBus().getStickyEvent(QueuePressureStateChangedEvent.class);
                    PennStation.postLocalEvent(new QueuePressureStateChangedEvent(QueuePressureStateChangedEvent.STATE_ABOVE_THRESHOLD, size));
                    mPendingThresholdWarned = true;
                } else {
                    cancelWarningIfNeeded(size);
                }
                sendMessage(service, newPerformRequestMessage(bundle));
            } else {
                mPendingQueue.put(reqId, bundle);
            }
        }
        return reqId;
    }

    private void cancelWarningIfNeeded(int size) {
        if (mPendingThresholdWarned && size < mPendingWarningThreshold) {
            mPendingThresholdWarned = false;
            PennStation.postLocalEvent(new QueuePressureStateChangedEvent(QueuePressureStateChangedEvent.STATE_GOOD, size));
        }
    }

    public void cancelAllUnSubmitted() {
        synchronized (mLock) {
            mRequestQueue.clear();
            mPendingQueue.clear();
            cancelWarningIfNeeded(0);
        }
    }

    private Message newPerformRequestMessage(Bundle bundle) {
        final Message msg = Message.obtain();
        msg.what = EventServiceImpl.PERFORM_REQUEST;
        msg.setData(bundle);
        msg.replyTo = getServiceResponder();
        return msg;
    }

    private Message newCancelRequestMessage(String reqId) {
        final Message msg = Message.obtain();
        msg.what = EventServiceImpl.CANCEL_REQUEST;
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_REQUEST_ID, reqId);
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
            Log.e(EventServiceImpl.TAG, "Unable to send message to service.", e);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        synchronized (mLock) {
            Messenger service = new Messenger(binder);
            mService = service;
            mRequestQueue.putAll(mPendingQueue);
            for (Bundle action : mPendingQueue.values()) {
                sendMessage(service, newPerformRequestMessage(action));
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
            return mRequestQueue.containsKey(requestId) || mPendingQueue.containsKey(requestId);
        }
    }

    public Requester.RequestListener onComplete(String requestId) {
        synchronized (mLock) {
            mRequestQueue.remove(requestId);
            cancelWarningIfNeeded(mRequestQueue.size());
            WeakReference<Requester.RequestListener> listRef = mListeners.remove(requestId);
            if (listRef != null) {
                Requester.RequestListener listener = listRef.get();
                if (listener != null) {
                    return listener;
                }
            }
        }
        return null;
    }

    public void cancel(String requestId) {
        synchronized (mLock) {
            if (isPending(requestId)) {
                mRequestQueue.remove(requestId);
                mPendingQueue.remove(requestId);
            }
            Messenger service = mService;
            if (service != null) {
                sendMessage(service, newCancelRequestMessage(requestId));
            }
            cancelWarningIfNeeded(mRequestQueue.size());
            WeakReference<Requester.RequestListener> listRef = mListeners.remove(requestId);
            if (listRef != null) {
                Requester.RequestListener listener = listRef.get();
                if (listener != null) {
                    listener.onCancelled(requestId);
                }
            }
        }
    }

    public String generateRequestId() {
        return Long.toHexString(Double.doubleToLongBits(Math.random())) + Long.toHexString(System.currentTimeMillis());
    }
}
