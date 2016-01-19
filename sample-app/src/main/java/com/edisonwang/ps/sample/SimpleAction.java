package com.edisonwang.ps.sample;

import com.edisonwang.ps.annotations.EventProducer;
import com.edisonwang.ps.annotations.RequestFactory;
import com.edisonwang.ps.annotations.RequestFactoryWithVariables;
import com.edisonwang.ps.annotations.ResultClassWithVariables;
import com.edisonwang.ps.lib.Action;
import com.edisonwang.ps.lib.ActionKey;
import com.edisonwang.ps.lib.ActionRequest;
import com.edisonwang.ps.lib.ActionRequestBuilder;
import com.edisonwang.ps.lib.ActionResult;
import com.edisonwang.ps.lib.EventServiceImpl;

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
@RequestFactoryWithVariables(variables = {})
public class SimpleAction implements Action {

    @Override
    public ActionResult processRequest(EventServiceImpl service, ActionRequest actionRequest) {
        return new SimpleActionEvent();
    }
}
