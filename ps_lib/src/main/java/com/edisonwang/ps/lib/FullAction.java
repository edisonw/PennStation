package com.edisonwang.ps.lib;

import android.content.Context;
import android.os.Bundle;

/**
 * @author edi
 */
public abstract class FullAction implements Action {

    private ActionCache mCache;

    /**
     * Called at the end of a request's completion.
     * @param resultDeliver the result deliver if any more events need to be delivered.
     * @param result the result of this request.
     */
    public void onRequestComplete(ResultDeliver resultDeliver, ActionResult result) {
    }

    public static class CachePolicy {

        public static final CachePolicy NotAllowed = new CachePolicy(false, 0);

        public final boolean allowCache;
        public final int maxSize;

        public CachePolicy(boolean allowCache, int maxCacheSize) {
            this.allowCache = allowCache;
            this.maxSize = maxCacheSize;
        }
    }

    protected abstract ActionResult process(Context context, ActionRequest request, RequestEnv env) throws Throwable;

    protected abstract ActionResult onError(Context context, ActionRequest request, RequestEnv env, Throwable e);

    /**
     * Runs before process request.
     *
     * @param context the context for this action.
     * @param request the request for this action.
     * @param env     the request environment args.
     * @return a result if the request should not continue. (e.g. login state), default returns null.
     */
    protected ActionResult preProcess(Context context, ActionRequest request, RequestEnv env) {
        return null;
    }

    protected CachePolicy getCachePolicy() {
        return CachePolicy.NotAllowed;
    }

    protected Bundle args(ActionRequest request) {
        return request.getArguments(getClass().getClassLoader());
    }

    /**
     * Runs when an error happens during process.
     *
     * @return the error that will be sent as a result.
     */
    @Override
    public final ActionResult processRequest(Context context, ActionRequest request, RequestEnv env) {
        ActionResult result;
        if (request.actionCacheAllowed()) {
            result = getCache(env).get(request);
            if (result != null) {
                return result;
            }
        }
        try {
            result = preProcess(context, request, env);
            if (result == null) {
                result = process(context, request, env);
            }
        } catch (Throwable e) {
            result = onError(context, request, env, e);
        }
        getCache(env).put(request, result);
        return result;
    }

    private synchronized ActionCache getCache(RequestEnv env) {
        if (mCache == null) {
            mCache = env.getActionCacheFactory().getCache(this, getCachePolicy());
        }
        return mCache;
    }
}
