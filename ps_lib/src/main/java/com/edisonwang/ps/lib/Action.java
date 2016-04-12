package com.edisonwang.ps.lib;

import android.content.Context;

/**
 * @author edi
 */
public interface Action {

    ActionResult processRequest(Context context, ActionRequest request, RequestEnv env);

}
