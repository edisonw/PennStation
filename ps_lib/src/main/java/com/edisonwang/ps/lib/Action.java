package com.edisonwang.ps.lib;

/**
 * @author edi
 */
public interface Action {

    ActionResult processRequest(EventServiceImpl service, ActionRequest actionRequest);

}
