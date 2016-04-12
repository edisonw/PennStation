package com.edisonwang.ps.sample;

import android.content.Context;

import com.edisonwang.ps.annotations.EventProducer;
import com.edisonwang.ps.annotations.RequestAction;
import com.edisonwang.ps.annotations.RequestActionHelper;
import com.edisonwang.ps.annotations.EventClass;
import com.edisonwang.ps.lib.Action;
import com.edisonwang.ps.lib.ActionRequest;
import com.edisonwang.ps.lib.ActionResult;
import com.edisonwang.ps.lib.RequestEnv;

/**
 * @author edi
 */
@EventProducer(generated = {
        @EventClass
})
@RequestAction
@RequestActionHelper
public class SimpleAction implements Action {

    @Override
    public ActionResult processRequest(Context context, ActionRequest request, RequestEnv env) {
        try {
            //Pretend this takes 3s to execute.
            Thread.sleep(2 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new SimpleActionEvent();
    }
}
