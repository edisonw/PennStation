package com.edisonwang.ps.lib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * @author edi
 */
public class ActionResults {
    private final HashMap<ActionRequest, ActionResult> results = new HashMap<>();
    private boolean hasFailed;

    public boolean hasFailed() {
        return hasFailed;
    }

    public void add(ActionRequest request, ActionResult result) {
        results.put(request, result);
        if (!result.isSuccess()) {
            hasFailed = true;
        }
    }

    public HashMap<ActionRequest, ActionResult> getResults() {
        return results;
    }

    public ActionResult getResult(ActionRequest request) {
        return results.get(request);
    }

    public ActionResult getFirstResult(Class<? extends Action> action) {
        Set<ActionRequest> keys = results.keySet();
        for (ActionRequest key : keys) {
            if (key.type() == action) {
                return results.get(key);
            }
        }
        return null;
    }

    public List<ActionResult> getResults(Class<? extends Action> action) {
        List<ActionResult> r = new ArrayList<>();
        Set<ActionRequest> keys = results.keySet();
        for (ActionRequest key : keys) {
            if (key.type() == action) {
                r.add(results.get(key));
            }
        }
        return r;
    }
}
