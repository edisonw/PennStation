package com.edisonwang.ps.lib;

import android.os.Bundle;

/**
 * @author edi
 */
public class ActionRequestEnv implements RequestEnv {

    private final Bundle mBundle;

    public ActionRequestEnv(Bundle bundle) {
        mBundle = bundle;
    }

    @Override
    public Bundle getServiceBundle() {
        return mBundle;
    }
}
