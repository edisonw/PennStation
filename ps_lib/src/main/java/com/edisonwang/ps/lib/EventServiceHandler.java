package com.edisonwang.ps.lib;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * @author edi
 */
public class EventServiceHandler<T extends EventServiceImpl> extends Handler {

    private final WeakReference<T> mServiceImpl;

    EventServiceHandler(WeakReference<T> serviceImpl) {
        mServiceImpl = serviceImpl;
    }

    @Override
    public void handleMessage(Message msg) {
        T serviceImpl = mServiceImpl.get();
        if (serviceImpl != null) {
            switch (msg.what) {
                case EventServiceImpl.PERFORM_REQUEST:
                    serviceImpl.performRequest(msg);
                    break;
                case EventServiceImpl.CANCEL_REQUEST:
                    serviceImpl.cancelRequest(msg);
                    break;
            }
        } else {
            Log.e(EventServiceImpl.TAG, "ServiceImpl is already dead.");
        }
    }
}
