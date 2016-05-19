package com.edisonwang.ps.lib;

import android.content.Context;
import android.os.Bundle;

/**
 * @author edi
 */
public interface RequestEnv {

    Bundle getServiceState();

    Context getContext();

    ActionResults getResults();

    Bundle getServiceBundle();

    ActionCacheFactory getActionCacheFactory();
}
