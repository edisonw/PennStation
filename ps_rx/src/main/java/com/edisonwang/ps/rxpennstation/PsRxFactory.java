package com.edisonwang.ps.rxpennstation;

import android.os.Bundle;

import com.edisonwang.ps.lib.ActionRequestHelper;
import com.edisonwang.ps.lib.ActionResult;
import com.edisonwang.ps.lib.PennStation;
import com.edisonwang.ps.lib.Requester;

import java.lang.ref.WeakReference;

import rx.Observable;
import rx.Subscriber;

/**
 * @author edi
 */
public class PsRxFactory {

    /**
     * Right now there's no point to make this public
     * But when Annotation Support arrives for Jack and Jill:

     Observable
     .interval(1000, TimeUnit.MILLISECONDS)
     .take(6)
     .flatMap((Func1<? super Long, ? extends Observable<?>>) aLong -> PsRxFactory.from(PsSimpleAction.helper(), SimpleActionEvent.class))
     .subscribe(event -> Log.i("PennStationTest", event.toString()));

     Also annotation generators will be included so that type is not needed.

     Observable.interval(1000, TimeUnit.MILLISECONDS).take(6).flatMap(new Func1<Long, Observable<SimpleActionEvent>>() {
        @Override
        public Observable<SimpleActionEvent> call(Long aLong) {
            return PsRxFactory.from(PsSimpleAction.helper(), SimpleActionEvent.class);
        }
        }).subscribe(new Subscriber<SimpleActionEvent>() {
            //DO something.
        });
     */
    public static <T extends ActionResult> Observable<T> from(final ActionRequestHelper helper, final Class<T> type) {
        return Observable.create(new Observable.OnSubscribe<T>() {

            @Override
            public void call(final Subscriber<? super T> subscriber) {
                Requester.RequestListener requestListener = new Requester.RequestListener() {
                    @Override
                    public void onRequested(Bundle bundle, String requestId) {

                    }

                    @Override
                    public void onCompleted(String reqId, ActionResult result) {
                        if (!subscriber.isUnsubscribed()) {
                            if (type.isAssignableFrom(result.getClass())) {
                                subscriber.onNext((T) result);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(String requestId) {

                    }
                };
                new Requester(helper).request(
                        PennStation.getManager(), new WeakReference<>(requestListener));
            }
        });
    }
}
