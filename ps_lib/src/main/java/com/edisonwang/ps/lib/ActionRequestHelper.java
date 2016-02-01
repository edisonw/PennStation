package com.edisonwang.ps.lib;

import android.content.Intent;
import android.os.Bundle;

import java.io.Serializable;

/**
 *
 * @author edi
 */
public abstract class ActionRequestHelper {

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
        if (extras != null) {
            return extras.get(name);
        }
        return null;
    }

    protected abstract ActionKey getActionKey();

    public ActionRequest build() {
        return new ActionRequest(getActionKey(), mVariableHolder.getExtras());
    }
}
