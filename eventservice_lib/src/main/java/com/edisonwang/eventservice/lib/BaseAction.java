package com.edisonwang.eventservice.lib;

/**
 * @author edi
 */
public interface BaseAction {

    ActionResult processRequest(EventService service, ActionRequest actionRequest);

}
