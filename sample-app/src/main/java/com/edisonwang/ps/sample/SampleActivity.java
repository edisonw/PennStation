package com.edisonwang.ps.sample;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.edisonwang.ps.annotations.EventListener;
import com.edisonwang.ps.lib.ActionKey_.Samples;
import com.edisonwang.ps.lib.EventService;
import com.edisonwang.ps.lib.PennStation;

@EventListener(producers = {
        ComplicatedAction.class,
        SimpleAction.class
})
public class SampleActivity extends Activity {

    private static final String TAG = "SampleActivity";

    private SampleActivityEventListener mListener = new SampleActivityEventListener() {

        @Override
        public void onEventMainThread(SimpleActionEvent event) {
            Log.i(TAG, "Got " + event.getClass().getSimpleName());
        }

        @Override
        public void onEventMainThread(ComplicatedAction.SampleActionFailedEvent event) {
            Log.i(TAG, "Got " + event.getClass().getSimpleName() + " that was " +
                    event.mSampleParam + " " + event.mSampleParcelable.mTestName);
        }

        @Override
        public void onEventMainThread(ComplicatedAction.SampleActionSuccessEvent event) {
            Log.i(TAG, "Got " + event.getClass().getSimpleName() + " that was " +
                    event.mSampleParam + " " + event.mSampleParcelable.mTestName);
        }

        @Override
        public void onEventMainThread(ComplicatedActionEventSample event) {
            Log.i(TAG, "Got " + event.getClass().getSimpleName() + " that was " +
                    event.sampleParam3);
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
        PennStation.requestAction(Samples.simpleAction().build());
        PennStation.requestAction(Samples.complicatedAction().sampleParam("sampleParamOneToFail").sampleParamTwo(
                new ComplicatedAction.SampleParcelable("FailParcelable")).shouldFail(true)
                .build());
        PennStation.requestAction(Samples.complicatedAction().sampleParam("sampleParamOneToSucceed").sampleParamTwo(
                new ComplicatedAction.SampleParcelable("SuccessParcelable")).shouldFail(false)
                .build());
    }
}
