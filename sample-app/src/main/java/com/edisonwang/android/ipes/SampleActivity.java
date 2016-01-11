package com.edisonwang.android.ipes;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.edisonwang.eventservice.annotations.EventListener;
import com.edisonwang.eventservice.lib.ActionKey_.Samples;
import com.edisonwang.eventservice.lib.EventService;
import com.edisonwang.eventservice.lib.PennStation;

@EventListener(producers = {
        SampleAction.class
})
public class SampleActivity extends Activity {

    private static final String TAG = "SampleActivity";

    private SampleActivityEventListener mListener = new SampleActivityEventListener() {

        @Override
        public void onEventMainThread(SampleAction.SampleActionFailedEvent event) {
            Log.i(TAG, "Got " + event.getClass().getSimpleName() + " that was " +
                    event.mSampleParam + " " + event.mSampleParcelable.mTestName);
        }

        @Override
        public void onEventMainThread(SampleAction.SampleActionSuccessEvent event) {
            Log.i(TAG, "Got " + event.getClass().getSimpleName() + " that was " +
                    event.mSampleParam + " " + event.mSampleParcelable.mTestName);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);
        PennStation.init(getApplication(), EventService.class);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PennStation.registerListener(mListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PennStation.unRegisterListener(mListener);
    }

    public void testEventRequest(View button) {
        PennStation.requestAction(Samples.sampleAction().sampleParam("sampleParamOneToFail").sampleParamTwo(
                new SampleAction.SampleParcelable("FailParcelable")).shouldFail(true)
                .build());
        PennStation.requestAction(Samples.sampleAction().sampleParam("sampleParamOneToSucceed").sampleParamTwo(
                new SampleAction.SampleParcelable("SuccessParcelable")).shouldFail(false)
                .build());
    }
}
