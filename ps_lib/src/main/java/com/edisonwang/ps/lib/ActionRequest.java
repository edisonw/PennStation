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
    private final ArrayList<ActionRequest> mDependencies = new ArrayList<>();
    private final ArrayList<ActionRequest> mNext = new ArrayList<>();
    private final ArrayList<Requirement> mRequirementFactories = new ArrayList<>();
    private final boolean mIsFullAction;
    private boolean mActionCacheAllowed = false;
    private boolean mTerminateOnFailure = true;
    private final ActionKey mActionKey;

    Bundle mArgs;

    public ActionRequest(ActionKey actionKey, Bundle args,
                         ArrayList<ActionRequestHelper> dependencies,
                         ArrayList<ActionRequestHelper> chainedActions,
                         ArrayList<Requirement> requirementFactories,
                         boolean cacheAllowed,
                         boolean terminateOnFailure) {
        mActionKey = actionKey;
        mIsFullAction = mActionKey.value() instanceof FullAction;
        mActionCacheAllowed = cacheAllowed;
        mTerminateOnFailure = terminateOnFailure;
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
        for (Requirement factory : requirementFactories) {
            mRequirementFactories.add(factory);
        }

    }

    public ActionRequest(ActionKey actionKey) {
        mActionKey = actionKey;
        mIsFullAction = mActionKey.value() instanceof FullAction;
        mArgs = new Bundle();
    }

    protected ActionRequest(Parcel in) {
        mActionCacheAllowed = in.readInt() == 1;
        mTerminateOnFailure = in.readInt() == 1;
        mActionKey = (ActionKey) in.readSerializable();
        mIsFullAction = mActionKey.value() instanceof FullAction;
        in.readList(mDependencies, getClassLoader());
        in.readList(mNext, getClassLoader());
        in.readList(mRequirementFactories, getClassLoader());
        mArgs = in.readBundle(getClass().getClassLoader());
        if (mArgs == null) {
            mArgs = new Bundle();
        }
    }

    public boolean isFullAction() {
        return mIsFullAction;
    }

    public ActionRequest actionCacheAllowed(boolean cacheAllowed) {
        mActionCacheAllowed = cacheAllowed;
        return this;
    }

    public ActionRequest terminateOnFailure(boolean terminateOnFailure) {
        mTerminateOnFailure = terminateOnFailure;
        return this;
    }

    public boolean terminateOnFailure() {
        return mTerminateOnFailure;
    }

    public boolean actionCacheAllowed() {
        return mActionCacheAllowed;
    }

    public void addArgs(Bundle bundle) {
        mArgs.putAll(bundle);
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

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mActionCacheAllowed ? 1 : 0);
        dest.writeInt(mTerminateOnFailure ? 1 : 0);
        dest.writeSerializable(mActionKey);
        dest.writeList(mDependencies);
        dest.writeList(mNext);
        dest.writeList(mRequirementFactories);
        dest.writeBundle(mArgs != null ? mArgs : new Bundle());
    }

    public void process(final ResultDeliver resultDeliver,
                        final RequestEnv env,
                        final boolean isOriginalRequest) {
        //Handle dependencies.
        for (ActionRequest actionRequest : mDependencies) {
            actionRequest.process(resultDeliver, env, false);
            if (env.getResults().hasFailed() && actionRequest.terminateOnFailure()) {
                onCompletion(resultDeliver, null, isOriginalRequest);
                return;
            }
        }
        //Handle requirements.
        for (Requirement requirement : mRequirementFactories) {
            if (!requirement.get().isSatisfied(env, this)) {
                onCompletion(resultDeliver, null, isOriginalRequest);
                return;
            }
        }
        //Process current request.
        final Action action = mActionKey.value();
        final ActionResult result = action.processRequest(env.getContext(), this, env);
        if (result != null) {
            resultDeliver.deliverResult(result, false);
            env.getResults().add(this, result);
            if (env.getResults().hasFailed() && terminateOnFailure()) {
                onCompletion(resultDeliver, result, isOriginalRequest);
                return;
            }
        }
        //Handle chained events.
        for (ActionRequest actionRequest : mNext) {
            actionRequest.process(resultDeliver, env, false);
            if (env.getResults().hasFailed() && actionRequest.terminateOnFailure()) {
                onCompletion(resultDeliver, result, isOriginalRequest);
                return;
            }
        }
        onCompletion(resultDeliver, result, isOriginalRequest);
    }

    private void onCompletion(ResultDeliver resultDeliver, final ActionResult result, boolean isOriginalRequest) {
        final Action action = mActionKey.value();
        if (action instanceof FullAction) {
            ActionResult completeResult = ((FullAction) action).onRequestComplete(result);
            if (completeResult != null) {
                resultDeliver.deliverResult(completeResult, false);
            }
        }
        if (isOriginalRequest) {
            resultDeliver.deliverResult(result, true);
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
