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

    private boolean mActionCacheAllowed = false;

    private ActionKey mActionKey;
    private final ArrayList<ActionRequest> mDependencies = new ArrayList<>();
    private final ArrayList<ActionRequest> mNext = new ArrayList<>();

    Bundle mArgs;

    //Transient args
    private ActionResults mResults;

    public ActionRequest(ActionKey actionKey, Bundle args,
                         ArrayList<ActionRequestHelper> dependencies,
                         ArrayList<ActionRequestHelper> chainedActions,
                         boolean cacheAllowed) {
        mActionKey = actionKey;
        mActionCacheAllowed = cacheAllowed;
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

    public ActionRequest actionCacheAllowed(boolean cacheAllowed) {
        mActionCacheAllowed = cacheAllowed;
        return this;
    }

    public boolean actionCacheAllowed() {
        return mActionCacheAllowed;
    }

    public void addArgs(Bundle bundle) {
        mArgs.putAll(bundle);
    }

    public ArrayList<ActionResult> getCurrentRequestResults() {
        return mResults.getResults();
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
        mActionCacheAllowed = in.readInt() == 1;
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
        dest.writeInt(mActionCacheAllowed ? 1 : 0);
        dest.writeSerializable(mActionKey);
        dest.writeList(mDependencies);
        dest.writeList(mNext);
        dest.writeBundle(mArgs != null ? mArgs : new Bundle());
    }

    public void process(ResultDeliver resultDeliver,
                                           EventServiceImpl service,
                                           Bundle bundle,
                                           final ActionResults results) {
        mResults = results;
        for (ActionRequest actionRequest : mDependencies) {
            actionRequest.process(resultDeliver, service, bundle, results);
            if (mResults.hasFailed()) {
                onCompletion(resultDeliver, null);
                return;
            }
        }
        final Action action = mActionKey.value();
        final ActionResult result = action.processRequest(service.getContext(), this, new ActionRequestEnv(bundle));
        if (result != null) {
            resultDeliver.deliverResult(result);
            results.add(result);
            if (mResults.hasFailed()) {
                onCompletion(resultDeliver, result);
                return;
            }
        }
        for (ActionRequest actionRequest : mNext) {
            actionRequest.process(resultDeliver, service, bundle, results);
        }
        onCompletion(resultDeliver, result);
    }

    private void onCompletion(ResultDeliver resultDeliver, ActionResult result) {
        final Action action = mActionKey.value();
        if (action instanceof FullAction) {
            ((FullAction) action).onRequestComplete(resultDeliver, result);
        }
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
