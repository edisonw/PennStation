package com.edisonwang.android.ipes;

import com.edisonwang.eventservice.annotations.EventProducer;
import com.edisonwang.eventservice.annotations.RequestFactory;
import com.edisonwang.eventservice.annotations.RequestFactoryWithVariables;
import com.edisonwang.eventservice.annotations.ResultClassWithVariables;
import com.edisonwang.eventservice.lib.Action;
import com.edisonwang.eventservice.lib.ActionKey;
import com.edisonwang.eventservice.lib.ActionRequest;
import com.edisonwang.eventservice.lib.ActionRequestBuilder;
import com.edisonwang.eventservice.lib.ActionResult;
import com.edisonwang.eventservice.lib.EventServiceImpl;

/**
 * @author edi
 */
@EventProducer(events = {}, generated = {
        @ResultClassWithVariables(classPostFix = "", baseClass = ActionResult.class, fields = {}),
})
@RequestFactory(
        baseClass = ActionKey.class,
        valueType = Action.class,
        group = "Samples"
)
@RequestFactoryWithVariables(baseClass = ActionRequestBuilder.class, variables = {})
public class SimpleAction implements Action {

    @Override
    public ActionResult processRequest(EventServiceImpl service, ActionRequest actionRequest) {
        return new SimpleActionEvent();
    }
}
