package com.edisonwang.ps.sample;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.edisonwang.ps.annotations.Field;
import com.edisonwang.ps.annotations.Event;
import com.edisonwang.ps.annotations.EventProducer;
import com.edisonwang.ps.annotations.Kind;
import com.edisonwang.ps.annotations.ParcelableField;
import com.edisonwang.ps.annotations.Action;
import com.edisonwang.ps.annotations.ActionHelper;
import com.edisonwang.ps.lib.ActionKey;
import com.edisonwang.ps.lib.ActionRequest;
import com.edisonwang.ps.lib.ActionRequestHelper;
import com.edisonwang.ps.lib.ActionResult;
import com.edisonwang.ps.lib.RequestEnv;
import com.edisonwang.ps.lib.parcelers.ParcelableParceler;
import com.edisonwang.ps.sample.events.ComplicatedActionSample;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author edi
 */
@EventProducer(events = {
        ComplicatedAction.SampleActionSuccess.class,
        ComplicatedAction.SampleActionFailed.class
}, generated = {
        @Event(postFix = "Sample", base = ActionResult.class,
                fields = {
                        @ParcelableField(
                                name = "sampleParam3",
                                kind = @Kind(clazz = String.class),
                                parceler = SampleStringParceler.class),
                        @ParcelableField(
                                name = "sampleParcelable",
                                kind = @Kind(clazz = ComplicatedAction.SampleParcelable.class),
                                parceler = ParcelableParceler.class,
                                required = false
                        ),
                        @ParcelableField(
                                name = "defaultParcelable",
                                kind = @Kind(clazz = double.class)
                        ),
                        @ParcelableField(
                                name = "sampleStringList",
                                kind = @Kind(clazz = List.class, parameter = String.class),
                                required = false
                        ),
                }),
})
@Action(
        base = ActionKey.class,
        valueType = com.edisonwang.ps.lib.Action.class,
        group = "Sample"
)
@ActionHelper(base = ActionRequestHelper.class, args = {
        @Field(name = "sampleParam", kind = @Kind(clazz = String.class)),
        @Field(name = "sampleParamTwo", kind =  @Kind(clazz = ComplicatedAction.SampleParcelable.class), required = true),
        @Field(name = "shouldFail", kind =  @Kind(clazz = boolean.class))
}
)
public class ComplicatedAction implements com.edisonwang.ps.lib.Action {

    private static final String TAG = "ComplicatedAction";

    private static final Random sRandom = new Random();

    @Override
    public ActionResult processRequest(Context context, ActionRequest request, RequestEnv env) {
        ComplicatedActionHelper helper = new ComplicatedActionHelper(request.getArguments(this));
        Log.i(TAG, "Processing requestAction " + helper.sampleParamTwo().mTestName);
        final ActionResult result;
        if (helper.shouldFail()) {
            result = new SampleActionFailed(helper.sampleParam(), helper.sampleParamTwo());
        } else {
            if (sRandom.nextInt() % 2 == 0) {
                result = new SampleActionSuccess(helper.sampleParam(), helper.sampleParamTwo());
            } else {
                ComplicatedActionSample event = new ComplicatedActionSample("sampleParam3", 0);
                ArrayList<String> someRandomList = new ArrayList<>();
                for (int i = 0; i < 5; i++) {
                    someRandomList.add(String.valueOf(sRandom.nextInt(59) + 1));
                }
                event.sampleStringList = someRandomList;
                result = event;
            }
        }
        Log.i(TAG, "Processed " + helper.sampleParamTwo().mTestName + " " + result);
        return result;
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

    public static class SampleActionSuccess extends SampleActionEvent {

        public static final Parcelable.Creator<SampleActionSuccess> CREATOR
                = new Parcelable.Creator<SampleActionSuccess>() {
            public SampleActionSuccess createFromParcel(Parcel in) {
                return new SampleActionSuccess(in);
            }

            public SampleActionSuccess[] newArray(int size) {
                return new SampleActionSuccess[size];
            }
        };

        public SampleActionSuccess(String s, SampleParcelable parcelable) {
            super(s, parcelable);
        }

        public SampleActionSuccess(Parcel in) {
            super(in);
        }

        @Override
        public int describeContents() {
            return 0;
        }
    }

    public static class SampleActionFailed extends SampleActionEvent {

        public static final Parcelable.Creator<SampleActionFailed> CREATOR
                = new Parcelable.Creator<SampleActionFailed>() {
            public SampleActionFailed createFromParcel(Parcel in) {
                return new SampleActionFailed(in);
            }

            public SampleActionFailed[] newArray(int size) {
                return new SampleActionFailed[size];
            }
        };

        public SampleActionFailed(String s, SampleParcelable parcelable) {
            super(s, parcelable);
        }

        public SampleActionFailed(Parcel in) {
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
