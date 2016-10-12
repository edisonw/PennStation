package com.edisonwang.ps.sample;


import android.util.Log;

import com.edisonwang.ps.lib.ActionResult;
import com.edisonwang.ps.sample.events.SimpleActionEvent;

import rx.Subscription;
import rx.functions.Action1;

public class RxSampleActivity extends SampleActivity {
    private Subscription mSubscription;
    private Subscription mSubscriptionSingle;

    @Override
    protected void onResume() {
        super.onResume();
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
        mSubscription.unsubscribe();
        mSubscriptionSingle.unsubscribe();
    }
}
