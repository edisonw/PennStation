package com.edisonwang.ps.sample;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.edisonwang.ps.annotations.EventListener;
import com.edisonwang.ps.lib.EventService;
import com.edisonwang.ps.lib.PennStation;
import com.edisonwang.ps.lib.QueuePressureStateChangedEvent;
import com.edisonwang.ps.sample.ComplicatedAction_.PsSampleComplicatedAction;
import com.edisonwang.ps.sample.SimpleAction_.PsSimpleAction;

import java.util.Arrays;

@EventListener(producers = {
        ComplicatedAction.class,
        SimpleAction.class
})
public class SampleActivity extends Activity {

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
                    event.sampleParam3 + "\n" +
                    "Lucky Numbers were: " + Arrays.toString(event.sampleStringList.toArray()));
        }

        public void onEventMainThread(QueuePressureStateChangedEvent event) {
            onReceived("Got pressure changed event: " + event.state + " current size is " + event.size);
        }
    };

    private EditText mUpdates;

    @SuppressLint("SetTextI18n")
    private void onReceived(String text) {
        Log.i("PennStationTest", text);
        mUpdates.setText(text + " \n\n" + mUpdates.getText());
    }

    @SuppressLint("ShowToast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample);
        PennStation.PennStationOptions options = new PennStation.PennStationOptions(EventService.class);
        options.pendingWarningThreshold = 5;
        PennStation.init(getApplication(), options);
        mUpdates = (EditText) findViewById(R.id.updates);
        mUpdates.setKeyListener(null);
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
        mUpdates.setText("");
        //This will result in a success in simple action event.
        PennStation.requestAction(PsSimpleAction.helper());
        //This will result in a failed complicated action event.
        PennStation.requestAction(PsSampleComplicatedAction.helper().sampleParam("sampleParamOneToFail").sampleParamTwo(
                new ComplicatedAction.SampleParcelable("FailParcelable")).shouldFail(true)
                .buildRequest());
        //This will randomly be either one of the two success events that the action emits.
        PennStation.requestAction(new ComplicatedActionHelper().sampleParam("sampleParamOneToSucceed").sampleParamTwo(
                new ComplicatedAction.SampleParcelable("SuccessParcelable")).shouldFail(false)
                .buildRequest());
    }
}
