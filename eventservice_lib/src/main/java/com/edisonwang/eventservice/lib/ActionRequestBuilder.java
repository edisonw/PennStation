package com.edisonwang.eventservice.lib;

import android.content.Intent;
import android.os.Bundle;

/**
 * @author edi
 */
public class ActionRequestBuilder {

    protected ActionKey mTarget;
    protected Intent mVariableHolder = new Intent();
    private Bundle mValues;

    public void setVariableValues(Bundle values) {
        mValues = values;
    }

    protected Object get(String name) {
        if (mValues != null) {
            return mValues.get(name);
        }
        //Expensive.
        Bundle extras = mVariableHolder.getExtras();
        return extras.get(name);
    }

    public ActionRequest build() {
        return new ActionRequest(mTarget, mVariableHolder.getExtras());
    }
}
