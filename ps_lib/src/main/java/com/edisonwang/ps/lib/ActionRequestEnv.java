package com.edisonwang.ps.lib;

import android.os.Bundle;

/**
 * @author edi
 */
public class ActionRequestEnv implements RequestEnv {

    private final Bundle mBundle;
    private final ActionCacheFactory mActionCacheFactory;

    public ActionRequestEnv(Bundle bundle, ActionCacheFactory actionCacheFactory) {
        mBundle = bundle;
        mActionCacheFactory = actionCacheFactory;
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
