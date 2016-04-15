package com.edisonwang.ps.sample;

import android.content.Context;
import android.util.Log;

import com.edisonwang.ps.annotations.Action;
import com.edisonwang.ps.annotations.ActionHelper;
import com.edisonwang.ps.annotations.Field;
import com.edisonwang.ps.annotations.Kind;
import com.edisonwang.ps.lib.ActionRequest;
import com.edisonwang.ps.lib.ActionResult;
import com.edisonwang.ps.lib.FullAction;
import com.edisonwang.ps.lib.RequestEnv;
import com.edisonwang.ps.sample.events.CountActionEvent;

/**
 * @author edi
 */
@Action
@ActionHelper(args = {
        @Field(name = "cached_arg", kind = @Kind(clazz = int.class), required = true)
})
public class CachedAction extends FullAction {

    private int mCount = 0;

    @Override
    protected ActionResult process(Context context, ActionRequest request, RequestEnv env) throws Throwable {
        Thread.sleep(2_000);
        Log.i("CountAction", "Processing request on " + mCount);
        mCount++;
        return new CountActionEvent(mCount);
    }

    @Override
    protected ActionResult onError(Context context, ActionRequest request, RequestEnv env, Throwable e) {
        return null;
    }

    @Override
    protected CachePolicy getCachePolicy() {
        return new CachePolicy(true, 3);
    }
}
