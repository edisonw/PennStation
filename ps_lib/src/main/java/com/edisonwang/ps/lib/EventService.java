package com.edisonwang.ps.lib;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

/**
 * You can always extend this class and log the actions as needed.
 *
 * Make sure to register this Service.
 *
 * @author edi
 */
public class EventService extends Service {

    private EventServiceImpl mImpl = createImpl();
    private ActionCacheFactory mActionCacheFactory = new LruMemCacheFactory();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return mImpl.onStartCommand(intent, flags, startId);
    }

    protected EventServiceImpl createImpl() {
        return new EventServiceImpl<>(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mImpl.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mImpl.onBind(intent);
    }

    public ActionCacheFactory getActionCacheFactory() {
        return mActionCacheFactory;
    }
}
