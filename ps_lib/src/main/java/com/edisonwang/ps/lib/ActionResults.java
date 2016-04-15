package com.edisonwang.ps.lib;

import java.util.ArrayList;

/**
 * @author edi
 */
public class ActionResults {
    private final ArrayList<ActionResult> results = new ArrayList<>();
    private boolean hasFailed;

    public boolean hasFailed() {
        return hasFailed;
    }

    public void add(ActionResult result) {
        results.add(result);
        if (!result.isSuccess()) {
            hasFailed = true;
        }
    }

    public ArrayList<ActionResult> getResults() {
        return results;
    }
}
