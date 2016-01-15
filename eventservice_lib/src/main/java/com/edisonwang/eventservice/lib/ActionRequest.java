package com.edisonwang.eventservice.lib;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author edi
 */
public class ActionRequest implements Parcelable {

    ActionKey mActionKey;
    Bundle mArgs;

    public ActionRequest(ActionKey actionKey, Bundle args) {
        mActionKey = actionKey;
        mArgs = args;
        if (mArgs == null) {
            mArgs = new Bundle();
        }
    }


    public ActionRequest(ActionKey actionKey) {
        mActionKey = actionKey;
        mArgs = new Bundle();
    }

    public void addArgs(Bundle bundle) {
        mArgs.putAll(bundle);
    }

    public Bundle getArguments(ClassLoader loader) {
        mArgs.setClassLoader(loader);
        return mArgs;
    }

    protected ActionRequest(Parcel in) {
        mActionKey = (ActionKey) in.readSerializable();
        mArgs = in.readBundle();
        if (mArgs == null) {
            mArgs = new Bundle();
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(mActionKey);
        dest.writeBundle(mArgs != null ? mArgs : new Bundle());
    }

    public static final Creator<ActionRequest> CREATOR = new Creator<ActionRequest>() {
        @Override
        public ActionRequest createFromParcel(Parcel in) {
            return new ActionRequest(in);
        }

        @Override
        public ActionRequest[] newArray(int size) {
            return new ActionRequest[size];
        }
    };

    public ActionResult process(EventServiceImpl service) {
        return mActionKey.value().processRequest(service, this);
    }
}
