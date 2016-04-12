package com.edisonwang.ps.lib;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * @author edi
 */
public class ActionRequest implements Parcelable {
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

    private static final String TAG = "ActionRequest";

    private ActionKey mActionKey;
    private final ArrayList<ActionRequest> mDependencies = new ArrayList<>();
    private final ArrayList<ActionRequest> mNext = new ArrayList<>();

    Bundle mArgs;

    //Transient variables
    private ArrayList<ActionResult> mResults;

    public ActionRequest(ActionKey actionKey, Bundle args,
                         ArrayList<ActionRequestHelper> dependencies,
                         ArrayList<ActionRequestHelper> chainedActions) {
        mActionKey = actionKey;
        mArgs = args;
        if (mArgs == null) {
            mArgs = new Bundle();
        }
        for (ActionRequestHelper dependency : dependencies) {
            mDependencies.add(dependency.buildRequest());
        }
        for (ActionRequestHelper next : chainedActions) {
            mNext.add(next.buildRequest());
        }
    }

    public ActionRequest(ActionKey actionKey) {
        mActionKey = actionKey;
        mArgs = new Bundle();
    }

    public void addArgs(Bundle bundle) {
        mArgs.putAll(bundle);
    }

    public ArrayList<ActionResult> getCurrentRequestResults() {
        return mResults;
    }

    public Bundle getArguments(ClassLoader loader) {
        mArgs.setClassLoader(loader);
        return mArgs;
    }

    public Bundle getArguments(Action action) {
        return getArguments(action.getClass().getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    protected ActionRequest(Parcel in) {
        mActionKey = (ActionKey) in.readSerializable();
        in.readList(mDependencies, getClassLoader());
        in.readList(mNext, getClassLoader());
        mArgs = in.readBundle(getClass().getClassLoader());
        if (mArgs == null) {
            mArgs = new Bundle();
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeSerializable(mActionKey);
        dest.writeList(mDependencies);
        dest.writeList(mNext);
        dest.writeBundle(mArgs != null ? mArgs : new Bundle());
    }

    public ArrayList<ActionResult> process(ResultDeliver resultDeliver,
                                           EventServiceImpl service,
                                           Bundle bundle,
                                           final ArrayList<ActionResult> results) {
        mResults = results;
        for (ActionRequest actionRequest : mDependencies) {
            results.addAll(actionRequest.process(resultDeliver, service, bundle, results));
        }
        final Action action = mActionKey.value();
        final ActionResult result = action.processRequest(service.getContext(), this, new ActionRequestEnv(bundle));
        if (result != null) {
            resultDeliver.deliverResult(result);
            results.add(result);
        }
        for (ActionRequest actionRequest : mNext) {
            results.addAll(actionRequest.process(resultDeliver, service, bundle, results));
        }
        return results;
    }

    @Override
    public String toString() {
        return mActionKey + " with args: " + (mArgs != null ? mArgs.toString() : "No arguments supplied.");
    }

    public Class<? extends Action> type() {
        return mActionKey.value().getClass();
    }

    /**
     * Override this if there is a need to use a specific class loader.
     *
     * @return classLoader to use during Parceling.
     */
    public ClassLoader getClassLoader() {
        return type().getClassLoader();
    }
}
