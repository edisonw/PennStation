package com.edisonwang.ps.lib;

import android.app.Service;
import android.os.Bundle;

/**
 * @author edi
 */
public interface Action<T extends Service> {

    ActionResult processRequest(EventServiceImpl<T> service, ActionRequest actionRequest, Bundle bundle);

}
