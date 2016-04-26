package com.edisonwang.ps.lib;

import android.os.Bundle;

/**
 * @author edi
 */
public interface RequestEnv {
    Bundle getServiceBundle();

    ActionCacheFactory getActionCacheFactory();
}
