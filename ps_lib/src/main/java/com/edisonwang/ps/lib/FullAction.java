package com.edisonwang.ps.lib;

import android.content.Context;

/**
 * @author edi
 */
public abstract class FullAction implements Action {

    protected abstract ActionResult process(Context context, ActionRequest request, RequestEnv env) throws Throwable;
    protected abstract ActionResult onError(Context context, ActionRequest request, RequestEnv env, Throwable e);

    /**
     * Runs before process request.
     *
     * @param context the context for this action.
     * @param request the request for this action.
     * @param env     the request environment variables.
     * @return a result if the request should not continue. (e.g. login state), default returns null.
     */
    protected ActionResult preProcess(Context context, ActionRequest request, RequestEnv env) {
        return null;
    }

    /**
     * Runs when an error happens during process.
     *
     * @return the error that will be sent as a result.
     */

    @Override
    public final ActionResult processRequest(Context context, ActionRequest request, RequestEnv env) {
        ActionResult result;
        try {
            result = preProcess(context, request, env);
            if (result == null) {
                result = process(context, request, env);
            }
        } catch (Throwable e) {
            result = onError(context, request, env, e);
        }
        return result;
    }
}
