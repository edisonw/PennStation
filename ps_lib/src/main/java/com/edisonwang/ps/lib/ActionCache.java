package com.edisonwang.ps.lib;

/**
 * @author edi
 */
public interface ActionCache {
    ActionResult get(ActionRequest request);

    void put(ActionRequest request, ActionResult result);
}
