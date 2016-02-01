package com.edisonwang.ps.sample;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.edisonwang.ps.annotations.EventListener;
import com.edisonwang.ps.lib.EventService;
import com.edisonwang.ps.lib.PennStation;
import com.edisonwang.ps.sample.ComplicatedAction_.PsSampleComplicatedAction;
import com.edisonwang.ps.sample.SimpleAction_.PsSimpleAction;

@EventListener(producers = {
        ComplicatedAction.class,
        SimpleAction.class
})
public class SampleActivity extends Activity {

    private Toast mToast;
    private SampleActivityEventListener mListener = new SampleActivityEventListener() {

        @Override
        public void onEventMainThread(SimpleActionEvent event) {
            onReceived("Got " + event.getClass().getSimpleName());
        }

        @Override
        public void onEventMainThread(ComplicatedAction.SampleActionFailedEvent event) {
            onReceived("Got " + event.getClass().getSimpleName() + " that was " +
                    event.mSampleParam + " " + event.mSampleParcelable.mTestName);
        }

        @Override
        public void onEventMainThread(ComplicatedAction.SampleActionSuccessEvent event) {
            onReceived("Got " + event.getClass().getSimpleName() + " that was " +
                    event.mSampleParam + " " + event.mSampleParcelable.mTestName);
        }

        @Override
        public void onEventMainThread(ComplicatedActionEventSample event) {
            onReceived("Got " + event.getClass().getSimpleName() + " that was " +
                    event.sampleParam3);
        }
    };

    private void onReceived(String text) {
        Log.i("PennStationTest", text);
        mToast.setText(text);
        mToast.show();
    }

    @SuppressLint("ShowToast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);
        PennStation.init(getApplication(), new PennStation.PennStationOptions(EventService.class));
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
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
        PennStation.requestAction(PsSimpleAction.helper());
        PennStation.requestAction(PsSampleComplicatedAction.helper().sampleParam("sampleParamOneToFail").sampleParamTwo(
                new ComplicatedAction.SampleParcelable("FailParcelable")).shouldFail(true)
                .buildRequest());
        PennStation.requestAction(new ComplicatedActionHelper().sampleParam("sampleParamOneToSucceed").sampleParamTwo(
                new ComplicatedAction.SampleParcelable("SuccessParcelable")).shouldFail(false)
                .buildRequest());
    }
}
