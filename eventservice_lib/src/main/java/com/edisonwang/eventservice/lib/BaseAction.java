package com.edisonwang.eventservice.lib;

/**
 * @author edi
 */
public interface BaseAction {

    ActionResult processRequest(EventServiceImpl service, ActionRequest actionRequest);

}
