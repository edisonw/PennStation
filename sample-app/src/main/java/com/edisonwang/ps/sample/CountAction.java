package com.edisonwang.ps.sample;

import android.content.Context;
import android.util.Log;

import com.edisonwang.ps.annotations.EventClass;
import com.edisonwang.ps.annotations.EventProducer;
import com.edisonwang.ps.annotations.Kind;
import com.edisonwang.ps.annotations.ParcelableClassField;
import com.edisonwang.ps.annotations.RequestAction;
import com.edisonwang.ps.annotations.RequestActionHelper;
import com.edisonwang.ps.lib.Action;
import com.edisonwang.ps.lib.ActionRequest;
import com.edisonwang.ps.lib.ActionResult;
import com.edisonwang.ps.lib.RequestEnv;

/**
 * @author edi
 */
@EventProducer(generated = {
        @EventClass(fields = {
                @ParcelableClassField(name = "count", kind = @Kind(clazz = int.class), required = true)
        })
})
@RequestAction
@RequestActionHelper
public class CountAction implements Action {

    private int mCount = 0;

    @Override
    public ActionResult processRequest(Context context, ActionRequest request, RequestEnv env) {
        Log.i("CountAction", "Processing request on " + mCount);
        mCount++;
        return new CountActionEvent(mCount);
    }
}
