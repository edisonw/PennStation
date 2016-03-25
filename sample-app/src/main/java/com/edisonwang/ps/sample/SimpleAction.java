package com.edisonwang.ps.sample;

import android.os.Bundle;

import com.edisonwang.ps.annotations.EventProducer;
import com.edisonwang.ps.annotations.RequestFactory;
import com.edisonwang.ps.annotations.RequestFactoryHelper;
import com.edisonwang.ps.annotations.ResultClassWithVariables;
import com.edisonwang.ps.lib.Action;
import com.edisonwang.ps.lib.ActionRequest;
import com.edisonwang.ps.lib.ActionResult;
import com.edisonwang.ps.lib.EventServiceImpl;

/**
 * @author edi
 */
@EventProducer(generated = {
        @ResultClassWithVariables
})
@RequestFactory
@RequestFactoryHelper
public class SimpleAction implements Action {

    @Override
    public ActionResult processRequest(EventServiceImpl service, ActionRequest actionRequest, Bundle bundle) {
        return new SimpleActionEvent();
    }
}
