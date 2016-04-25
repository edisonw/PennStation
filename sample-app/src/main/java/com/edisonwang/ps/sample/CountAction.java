package com.edisonwang.ps.sample;

import android.content.Context;
import android.util.Log;

import com.edisonwang.ps.annotations.Event;
import com.edisonwang.ps.annotations.EventProducer;
import com.edisonwang.ps.annotations.Kind;
import com.edisonwang.ps.annotations.ParcelableField;
import com.edisonwang.ps.annotations.Action;
import com.edisonwang.ps.annotations.ActionHelper;
import com.edisonwang.ps.lib.ActionRequest;
import com.edisonwang.ps.lib.ActionResult;
import com.edisonwang.ps.lib.FullAction;
import com.edisonwang.ps.lib.RequestEnv;
import com.edisonwang.ps.lib.ResultDeliver;
import com.edisonwang.ps.sample.events.CountActionComplete;
import com.edisonwang.ps.sample.events.CountActionEvent;
/**
 * @author edi
 */
@EventProducer(generated = {
        @Event(fields = {
                @ParcelableField(name = "count", kind = @Kind(clazz = int.class), required = true)
        }),
        @Event(postFix = "Complete"),
})
@Action
@ActionHelper
public class CountAction extends FullAction {

    private int mCount = 0;

    @Override
    protected ActionResult process(Context context, ActionRequest request, RequestEnv env) throws Throwable {
        Log.i("CountAction", "Processing request on " + mCount);
        mCount++;
        return new CountActionEvent(mCount);
    }

    @Override
    protected ActionResult onError(Context context, ActionRequest request, RequestEnv env, Throwable e) {
        return null;
    }

    @Override
    public ActionResult onRequestComplete(ActionResult result) {
        return new CountActionComplete();
    }
}
