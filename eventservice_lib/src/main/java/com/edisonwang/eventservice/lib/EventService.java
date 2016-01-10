package com.edisonwang.eventservice.lib;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * You can always extend this class and log the actions as needed.
 *
 * Make sure to register this Service.
 *
 * @author edi
 */
public class EventService extends Service {

    private EventServiceImpl<EventService> mImpl = new EventServiceImpl<>(this);

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return mImpl.onStartCommand(intent, flags, startId);
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
}
