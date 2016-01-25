package com.edisonwang.ps.lib;

import android.os.Bundle;

/**
 * @author edi
 */
public interface Action {

    ActionResult processRequest(EventServiceImpl service, ActionRequest actionRequest, Bundle bundle);

}
