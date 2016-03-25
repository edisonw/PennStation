package com.edisonwang.ps.sample;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.edisonwang.ps.annotations.ClassField;
import com.edisonwang.ps.annotations.EventProducer;
import com.edisonwang.ps.annotations.Kind;
import com.edisonwang.ps.annotations.ParcelableClassField;
import com.edisonwang.ps.annotations.RequestAction;
import com.edisonwang.ps.annotations.RequestActionHelper;
import com.edisonwang.ps.annotations.EventClass;
import com.edisonwang.ps.lib.Action;
import com.edisonwang.ps.lib.ActionKey;
import com.edisonwang.ps.lib.ActionRequest;
import com.edisonwang.ps.lib.ActionRequestHelper;
import com.edisonwang.ps.lib.ActionResult;
import com.edisonwang.ps.lib.EventServiceImpl;
import com.edisonwang.ps.lib.parcelers.ParcelableParceler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author edi
 */
@EventProducer(events = {
        ComplicatedAction.SampleActionSuccessEvent.class,
        ComplicatedAction.SampleActionFailedEvent.class
}, generated = {
        @EventClass(classPostFix = "Sample", baseClass = ActionResult.class,
                fields = {
                        @ParcelableClassField(
                                name = "sampleParam3",
                                kind = @Kind(clazz = String.class),
                                parceler = SampleStringParceler.class),
                        @ParcelableClassField(
                                name = "sampleParcelable",
                                kind = @Kind(clazz = ComplicatedAction.SampleParcelable.class),
                                parceler = ParcelableParceler.class,
                                required = false
                        ),
                        @ParcelableClassField(
                                name = "defaultParcelable",
                                kind = @Kind(clazz = double.class)
                        ),
                        @ParcelableClassField(
                                name = "sampleStringList",
                                kind = @Kind(clazz = List.class, parameter = String.class),
                                required = false
                        ),
                }),
})
@RequestAction(
        baseClass = ActionKey.class,
        valueType = Action.class,
        group = "Sample"
)
@RequestActionHelper(baseClass = ActionRequestHelper.class, variables = {
        @ClassField(name = "sampleParam", kind = @Kind(clazz = String.class)),
        @ClassField(name = "sampleParamTwo", kind =  @Kind(clazz = ComplicatedAction.SampleParcelable.class)),
        @ClassField(name = "shouldFail", kind =  @Kind(clazz = boolean.class))
}
)
public class ComplicatedAction implements Action {

    private static final String TAG = "ComplicatedAction";

    private static final Random sRandom = new Random();

    @Override
    public ActionResult processRequest(EventServiceImpl service, ActionRequest actionRequest, Bundle bundle) {
        ComplicatedActionHelper helper = new ComplicatedActionHelper(actionRequest.getArguments(getCurrentClassLoader()));
        Log.i(TAG, "Processing requestAction " + helper.sampleParamTwo().mTestName);
        if (helper.shouldFail()) {
            return new SampleActionFailedEvent(helper.sampleParam(), helper.sampleParamTwo());
        } else {
            if (sRandom.nextInt() % 2 == 0) {
                return new SampleActionSuccessEvent(helper.sampleParam(), helper.sampleParamTwo());
            } else {
                ComplicatedActionEventSample event = new ComplicatedActionEventSample("sampleParam3", 0);
                ArrayList<String> someRandomList = new ArrayList<>();
                for (int i = 0; i < 5; i++) {
                    someRandomList.add(String.valueOf(sRandom.nextInt(59) + 1));
                }
                event.sampleStringList = someRandomList;
                return event;
            }
        }
    }

    /**
     * @return the class loader associated with the current module,
     * you may want to use a different class loader.
     */
    private ClassLoader getCurrentClassLoader() {
        return getClass().getClassLoader();
    }

    @SuppressLint("ParcelCreator")
    public static abstract class SampleActionEvent extends ActionResult {

        public SampleParcelable mSampleParcelable;
        public String mSampleParam;

        public SampleActionEvent(String sampleParam, SampleParcelable parcelable) {
            mSampleParam = sampleParam;
            mSampleParcelable = parcelable;
        }

        public SampleActionEvent(Parcel in) {
            super(in);
            mSampleParam = in.readString();
            mSampleParcelable = in.readParcelable(SampleParcelable.class.getClassLoader());
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(mSampleParam);
            dest.writeParcelable(mSampleParcelable, flags);
        }
    }

    public static class SampleActionSuccessEvent extends SampleActionEvent {

        public static final Parcelable.Creator<SampleActionSuccessEvent> CREATOR
                = new Parcelable.Creator<SampleActionSuccessEvent>() {
            public SampleActionSuccessEvent createFromParcel(Parcel in) {
                return new SampleActionSuccessEvent(in);
            }

            public SampleActionSuccessEvent[] newArray(int size) {
                return new SampleActionSuccessEvent[size];
            }
        };

        public SampleActionSuccessEvent(String s, SampleParcelable parcelable) {
            super(s, parcelable);
        }

        public SampleActionSuccessEvent(Parcel in) {
            super(in);
        }

        @Override
        public int describeContents() {
            return 0;
        }
    }

    public static class SampleActionFailedEvent extends SampleActionEvent {

        public static final Parcelable.Creator<SampleActionFailedEvent> CREATOR
                = new Parcelable.Creator<SampleActionFailedEvent>() {
            public SampleActionFailedEvent createFromParcel(Parcel in) {
                return new SampleActionFailedEvent(in);
            }

            public SampleActionFailedEvent[] newArray(int size) {
                return new SampleActionFailedEvent[size];
            }
        };

        public SampleActionFailedEvent(String s, SampleParcelable parcelable) {
            super(s, parcelable);
        }

        public SampleActionFailedEvent(Parcel in) {
            super(in);
        }

        @Override
        public int describeContents() {
            return 0;
        }
    }

    public static class SampleParcelable implements Parcelable {

        public static final Creator<SampleParcelable> CREATOR = new Creator<SampleParcelable>() {
            @Override
            public SampleParcelable createFromParcel(Parcel in) {
                return new SampleParcelable(in);
            }

            @Override
            public SampleParcelable[] newArray(int size) {
                return new SampleParcelable[size];
            }
        };
        public String mTestName;

        public SampleParcelable(String testName) {
            mTestName = testName;
        }

        protected SampleParcelable(Parcel in) {
            mTestName = in.readString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mTestName);
        }
    }
}
