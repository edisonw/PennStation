package com.edisonwang.ps.sample;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.edisonwang.ps.annotations.EventListener;
import com.edisonwang.ps.lib.ActionRequest;
import com.edisonwang.ps.lib.ActionRequestHelper;
import com.edisonwang.ps.lib.ActionResult;
import com.edisonwang.ps.lib.EventService;
import com.edisonwang.ps.lib.LimitedQueueInfo;
import com.edisonwang.ps.lib.PennStation;
import com.edisonwang.ps.lib.QueuePressureStateChangedEvent;
import com.edisonwang.ps.sample.CachedAction_.PsCachedAction;
import com.edisonwang.ps.sample.ComplicatedAction_.PsSampleComplicatedAction;
import com.edisonwang.ps.sample.CountAction_.PsCountAction;
import com.edisonwang.ps.sample.SimpleAction_.PsSimpleAction;
import com.edisonwang.ps.sample.events.ComplicatedActionSample;
import com.edisonwang.ps.sample.events.CountActionComplete;
import com.edisonwang.ps.sample.events.CountActionEvent;
import com.edisonwang.ps.sample.events.SimpleActionEvent;

import java.util.ArrayList;
import java.util.Arrays;

import rx.Subscription;
import rx.functions.Action1;

@EventListener(producers = {
        ComplicatedAction.class,
        SimpleAction.class,
        CountAction.class
})
public class SampleActivity extends Activity {

    private final ArrayList<String> mRequestIds = new ArrayList<>();

    private SampleActivityEventListener mListener = new SampleActivityEventListener() {

        @Override
        public void onEventMainThread(CountActionComplete event) {
            onReceived("Count complete!");
        }

        @Override
        public void onEventMainThread(SimpleActionEvent event) {
            onReceived("Got " + event.getClass().getSimpleName());
        }

        @Override
        public void onEventMainThread(ComplicatedAction.SampleActionFailed event) {
            onReceived("Got " + event.getClass().getSimpleName() + " that was " +
                    event.mSampleParam + " " + event.mSampleParcelable.mTestName);
        }

        @Override
        public void onEventMainThread(ComplicatedAction.SampleActionSuccess event) {
            onReceived("Got " + event.getClass().getSimpleName() + " that was " +
                    event.mSampleParam + " " + event.mSampleParcelable.mTestName);
        }

        @Override
        public void onEventMainThread(CountActionEvent event) {
            onReceived("Count was " + event.count);
        }

        @Override
        public void onEventMainThread(ComplicatedActionSample event) {
            onReceived("Got " + event.getClass().getSimpleName() + " that was " +
                    event.sampleParam3 + "\n" +
                    "Lucky Numbers were: " + Arrays.toString(event.sampleStringList.toArray()));
        }

        public void onEventMainThread(QueuePressureStateChangedEvent event) {
            onReceived("Got pressure changed event: " + event.state + " current size is " + event.size);
        }
    };

    private EditText mUpdates;
    private Subscription mSubscription;
    private Subscription mSubscriptionSingle;

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
        PennStation.PennStationOptions options =
                new PennStation.PennStationOptions(EventService.class);
        options.pendingWarningThreshold = 5;
        PennStation.init(getApplication(), options);
        mUpdates = (EditText) findViewById(R.id.updates);
        mUpdates.setKeyListener(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PennStation.registerListener(mListener);
        mSubscription = SimpleActionObserver.create().subscribe(new Action1<ActionResult>() {
            @Override
            public void call(ActionResult actionResult) {
                Log.i("PennStationTest", "There was an simple action to action observer.");
            }
        });
        mSubscriptionSingle = SimpleActionEvent.Rx.observable().subscribe(new Action1<SimpleActionEvent>() {
            @Override
            public void call(SimpleActionEvent event) {
                Log.i("PennStationTest", "There was an simple action to event observer.");
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        PennStation.unRegisterListener(mListener);
        mSubscription.unsubscribe();
        mSubscriptionSingle.unsubscribe();
    }

    public void requestAction(ActionRequest request) {
        synchronized (mRequestIds) {
            mRequestIds.add(PennStation.requestAction(request));
        }
    }

    private void requestAction(ActionRequest request, LimitedQueueInfo queue) {
        synchronized (mRequestIds) {
            mRequestIds.add(PennStation.requestAction(request, queue));
        }
    }

    private void requestAction(ActionRequestHelper helper, LimitedQueueInfo queue) {
        synchronized (mRequestIds) {
            mRequestIds.add(PennStation.requestAction(helper, queue));
        }
    }

    private void requestAction(ActionRequestHelper helper) {
        synchronized (mRequestIds) {
            mRequestIds.add(PennStation.requestAction(helper));
        }
    }

    public void testSerial(View button) {
        LimitedQueueInfo queue = new LimitedQueueInfo(1, 1, "SimpleActionSerialQueue");
        for (int i = 0; i < 10; i++) {
            requestAction(PsSimpleAction.helper(), queue);
        }
    }

    public void testParallel(View button) {
        mUpdates.setText("");
        for (int i = 0; i < 10; i++) {
            requestAction(PsSimpleAction.helper());
        }
    }

    public void testCounter(View button) {
        mUpdates.setText("");
        requestAction(PsCountAction.helper().dependsOn(
                PsSampleComplicatedAction.helper(
                        new ComplicatedAction.SampleParcelable("First count."))
                        .sampleParam("First count is ready.").shouldFail(true)
        ).then(PsCountAction.helper().dependsOn(
                PsSampleComplicatedAction.helper(
                        new ComplicatedAction.SampleParcelable("Second count."))
                        .sampleParam("Second count is ready.").shouldFail(true)
        )));
    }

    public void testCancelCurrent(View button) {
        synchronized (mRequestIds) {
            for (String id : mRequestIds) {
                PennStation.cancelAction(id);
            }
        }
    }

    public void testCache(View button) {
        LimitedQueueInfo queue = new LimitedQueueInfo(1, 1, "CacheActionQueue");
        requestAction(PsCachedAction.helper(2).actionCacheAllowed(true), queue);
        requestAction(PsCachedAction.helper(3).actionCacheAllowed(false), queue);
    }

    public void testEventRequest(View button) {
        mUpdates.setText("");
        //This will result in a success in simple action event.
        requestAction(PsSimpleAction.helper());
        //This will result in a failed complicated action event.
        requestAction(PsSampleComplicatedAction.helper(
                new ComplicatedAction.SampleParcelable("FailParcelable"))
                .shouldFail(true)
                .sampleParam("sampleParamOneToFail")
                .buildRequest());
        //This will randomly be either one of the two success events that the action emits.
        requestAction(new ComplicatedActionHelper(
                new ComplicatedAction.SampleParcelable("SuccessParcelable"))
                .sampleParam("sampleParamOneToSucceed")
                .shouldFail(false)
                .buildRequest());
    }
}
