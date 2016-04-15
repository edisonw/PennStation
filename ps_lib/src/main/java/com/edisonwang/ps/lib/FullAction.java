package com.edisonwang.ps.lib;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.LruCache;

/**
 * @author edi
 */
public abstract class FullAction implements Action {

    private static final String TAG = "FullAction";

    private final LruCache<BundleKey, ActionResult> mCache;

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public FullAction() {
        CachePolicy policy = getCachePolicy();
        if (policy.allowCache) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                mCache = new LruCache<>(policy.maxSize);
            } else {
                Log.e(EventServiceImpl.TAG, "Cache is not supported pre ICS for PennStation.");
                mCache = null;
            }
        } else {
            mCache = null;
        }
    }

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
            BundleKey key = new BundleKey(args(request));
            if (mCache != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                result = mCache.get(key);
                if (result != null) {
                    Log.i(TAG, "Cache hit.");
                    return result;
                } else {
                    Log.i(TAG, "Cache miss.");
                }
            } else {
                Log.i(TAG, "Cache was not allowed.");
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

        if (mCache != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            Log.i(TAG, "Filling the cache..");
            mCache.put(new BundleKey(args(request)), result);
        }
        return result;
    }
}
