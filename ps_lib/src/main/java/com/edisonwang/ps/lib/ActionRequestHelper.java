package com.edisonwang.ps.lib;

import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;

/**
 * Helper that can build ActionRequest and extract arguments from bundles.
 *
 * @author edi
 */
public abstract class ActionRequestHelper {

    private final ArrayList<ActionRequestHelper> mDependencies = new ArrayList<>();
    private final ArrayList<ActionRequestHelper> mNext = new ArrayList<>();
    protected Intent mVariableHolder = new Intent();
    private Bundle mValues;
    private boolean mCacheAllowed;
    private boolean mTerminateOnFailure = true;

    /**
     * @param values the arguments for this request.
     */
    protected void setVariableValues(Bundle values) {
        mValues = values;
    }

    /**
     * Returns a variable previously set.
     *
     * @param name name of the variable.
     * @return the value of the variable.
     */
    protected Object get(String name) {
        if (mValues != null) {
            return mValues.get(name);
        }
        //Expensive.
        Bundle extras = mVariableHolder.getExtras();
        if (extras != null) {
            return extras.get(name);
        }
        return null;
    }

    /**
     * @return the ActionKey this request represents.
     */
    protected abstract ActionKey getActionKey();

    /**
     * If allowed, results can be returned from Action's cache implementation.
     *
     * @param allowed if cached results can be used
     * @return self
     */
    public ActionRequestHelper actionCacheAllowed(boolean allowed) {
        mCacheAllowed = allowed;
        return this;
    }

    /**
     * This request will execute after the dependency completes.
     *
     * @param dependency the depended request.
     * @return self
     */
    public ActionRequestHelper dependsOn(ActionRequestHelper dependency) {
        mDependencies.add(dependency);
        return this;
    }

    /**
     * Used in chaining requests. If set to true, dependents of this will Not continue to execute.
     *
     * @param dependency the depended request.
     * @param terminateOnFailure if true, and this dependency emits an error, current will not run.
     * @return self
     */
    public ActionRequestHelper dependsOn(ActionRequestHelper dependency, boolean terminateOnFailure) {
        dependency.terminateOnFailure(terminateOnFailure);
        mDependencies.add(dependency);
        return this;
    }

    /**
     * Used in chaining requests. If set to true, dependents of this will Not continue to execute.
     *
     * @return self
     */
    public ActionRequestHelper terminateOnFailure(boolean terminateOnFailure) {
        mTerminateOnFailure = terminateOnFailure;
        return this;
    }

    /**
     * This request will be executed if the current request succeed.
     *
     * @param nextAction the action to execute next.
     * @return self
     */
    public ActionRequestHelper then(ActionRequestHelper nextAction) {
        mNext.add(nextAction);
        return this;
    }

    /**
     * This request will be executed if the current request succeed.
     *
     * @param nextAction the action to execute next.
     * @return self
     */
    public ActionRequestHelper then(ActionRequestHelper nextAction, boolean terminateOnFailure) {
        mNext.add(nextAction);
        nextAction.terminateOnFailure(terminateOnFailure);
        return this;
    }

    /**
     * @return a brand new request using arguments and settings from this builder.
     */
    public ActionRequest buildRequest() {
        return new ActionRequest(
                getActionKey(),
                mVariableHolder.getExtras(),
                mDependencies,
                mNext,
                mCacheAllowed,
                mTerminateOnFailure);
    }
}
