package com.edisonwang.ps.lib;

import android.os.Build;
import android.util.Log;
import android.util.LruCache;

/**
 * @author edi
 */
public class LruMemCacheFactory implements ActionCacheFactory {

    private static final String TAG = "ActionLruMemCache";

    @Override
    public ActionCache getCache(FullAction action, FullAction.CachePolicy policy) {
        return new LruMemCache(action, policy);
    }

    public static class LruMemCache implements ActionCache {
        private final LruCache<BundleKey, ActionResult> mCache;
        private final FullAction mAction;

        public LruMemCache(FullAction action, FullAction.CachePolicy policy) {
            if (policy != null && policy.allowCache
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                mCache = new LruCache<>(policy.maxSize);
            } else {
                mCache = null;
            }
            mAction = action;
        }

        public boolean isReady() {
            return mCache != null;
        }

        @Override
        public ActionResult get(ActionRequest request) {
            if (mCache == null) {
                Log.i(TAG, "Cache was not allowed.");
                return null;
            }
            BundleKey key = new BundleKey(mAction.args(request));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                ActionResult result = mCache.get(key);
                if (result != null) {
                    Log.i(TAG, "Cache hit.");
                    return result;
                } else {
                    Log.i(TAG, "Cache miss.");
                }
            } else {
                Log.i(TAG, "Cache was not supported.");
            }
            return null;
        }

        @Override
        public void put(ActionRequest request, ActionResult result) {
            if (mCache != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
                mCache.put(new BundleKey(mAction.args(request)), result);
            }
        }
    }

}
