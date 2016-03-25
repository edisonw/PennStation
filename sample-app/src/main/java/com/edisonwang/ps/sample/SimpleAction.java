package com.edisonwang.ps.sample;

import android.os.Bundle;

import com.edisonwang.ps.annotations.EventProducer;
import com.edisonwang.ps.annotations.RequestAction;
import com.edisonwang.ps.annotations.RequestActionHelper;
import com.edisonwang.ps.annotations.EventClass;
import com.edisonwang.ps.lib.Action;
import com.edisonwang.ps.lib.ActionRequest;
import com.edisonwang.ps.lib.ActionResult;
import com.edisonwang.ps.lib.EventServiceImpl;

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
    public ActionResult processRequest(EventServiceImpl service, ActionRequest actionRequest, Bundle bundle) {
        return new SimpleActionEvent();
    }
}
