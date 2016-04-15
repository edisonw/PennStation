package com.edisonwang.ps.lib;

import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;

/**
 * @author edi
 */
public abstract class ActionRequestHelper {

    protected Intent mVariableHolder = new Intent();
    private Bundle mValues;

    private final ArrayList<ActionRequestHelper> mDependencies = new ArrayList<>();
    private final ArrayList<ActionRequestHelper> mNext = new ArrayList<>();
    private boolean mCacheAllowed;

    protected void setVariableValues(Bundle values) {
        mValues = values;
    }

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

    protected abstract ActionKey getActionKey();

    public ActionRequestHelper actionCacheAllowed(boolean allowed) {
        mCacheAllowed = allowed;
        return this;
    }

    /**
     * This request will depend on this dependency's success.
     * @param dependency the depended request.
     * @return self
     */
    public ActionRequestHelper dependsOn(ActionRequestHelper dependency) {
        mDependencies.add(dependency);
        return this;
    }

    /**
     * This request will be executed if the current request succeed.
     * @param nextAction the action to execute next.
     * @return self
     */
    public ActionRequestHelper then(ActionRequestHelper nextAction) {
        mNext.add(nextAction);
        return this;
    }

    public ActionRequest buildRequest() {
        return new ActionRequest(
                getActionKey(),
                mVariableHolder.getExtras(),
                mDependencies,
                mNext,
                mCacheAllowed);
    }
}
