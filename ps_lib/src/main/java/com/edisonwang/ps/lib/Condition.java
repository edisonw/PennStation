package com.edisonwang.ps.lib;

/**
 *
 * Conditions that has to isSatisfied after the dependencies are processed.
 *
 * @author edi
 */
public interface Condition {

    /**
     * Satisfy the condition, return false if condition is not met and execution should terminate.
     */
    boolean isSatisfied(RequestEnv env, ActionRequest request);

}
