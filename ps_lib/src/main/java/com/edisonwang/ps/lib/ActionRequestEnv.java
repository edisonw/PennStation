package com.edisonwang.ps.lib;

import android.content.Context;
import android.os.Bundle;

/**
 * @author edi
 */
public class ActionRequestEnv implements RequestEnv {

    private final Bundle mBundle;
    private final ActionCacheFactory mActionCacheFactory;
    private final ActionResults mResults;
    private final EventServiceImpl mServiceImpl;

    public ActionRequestEnv(Bundle bundle, ActionCacheFactory actionCacheFactory, EventServiceImpl service) {
        mBundle = bundle;
        mActionCacheFactory = actionCacheFactory;
        mResults = new ActionResults();
        mServiceImpl = service;
    }

    @Override
    public Bundle getServiceState() {
        return mServiceImpl.getState();
    }

    @Override
    public Context getContext() {
        return mServiceImpl.getContext();
    }

    @Override
    public ActionResults getResults() {
        return mResults;
    }

    @Override
    public Bundle getServiceBundle() {
        return mBundle;
    }

    @Override
    public ActionCacheFactory getActionCacheFactory() {
        return mActionCacheFactory;
    }
}
