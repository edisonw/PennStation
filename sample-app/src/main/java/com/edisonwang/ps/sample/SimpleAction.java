package com.edisonwang.ps.sample;

import android.content.Context;

import com.edisonwang.ps.annotations.EventProducer;
import com.edisonwang.ps.annotations.Action;
import com.edisonwang.ps.annotations.ActionHelper;
import com.edisonwang.ps.annotations.Event;
import com.edisonwang.ps.lib.ActionRequest;
import com.edisonwang.ps.lib.ActionResult;
import com.edisonwang.ps.lib.RequestEnv;
import com.edisonwang.ps.sample.events.SimpleActionEvent;

/**
 * @author edi
 */
@EventProducer(generated = {
        @Event
})
@Action
@ActionHelper
public class SimpleAction implements com.edisonwang.ps.lib.Action {

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
