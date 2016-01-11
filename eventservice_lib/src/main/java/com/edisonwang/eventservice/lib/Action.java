package com.edisonwang.eventservice.lib;

/**
 * @author edi
 */
public interface Action {

    ActionResult processRequest(EventServiceImpl service, ActionRequest actionRequest);

}
