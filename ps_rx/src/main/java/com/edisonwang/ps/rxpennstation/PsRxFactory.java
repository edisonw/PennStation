package com.edisonwang.ps.rxpennstation;

import android.os.Bundle;

import com.edisonwang.ps.lib.ActionRequest;
import com.edisonwang.ps.lib.ActionRequestHelper;
import com.edisonwang.ps.lib.ActionResult;
import com.edisonwang.ps.lib.PennStation;
import com.edisonwang.ps.lib.Requester;

import java.lang.ref.WeakReference;

import rx.Observable;
import rx.Subscriber;

/**
 * Right now there's no point to make this public
 * But when Annotation Support arrives for Jack and Jill.
 *
 * @author edi
 */
public class PsRxFactory<T extends ActionResult> {

    private static PsRxFactory<ActionResult> generic = new PsRxFactory<>(ActionResult.class);

    public static PsRxFactory<ActionResult> getGeneric() {
        return generic;
    }

    final Class<T> type;

    public PsRxFactory(Class<T> type) {
        this.type = type;
    }

    public Observable<T> from(final ActionRequestHelper helper) {
        return from(helper.buildRequest());
    }
    public Observable<T> from(final ActionRequest request) {
        return Observable.create(
                new Observable.OnSubscribe<T>() {
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
                                    } else if (!result.isSuccess()) {
                                        subscriber.onError(null);
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(String requestId) {

                            }
                        };
                        new Requester(request).request(PennStation.getManager(), new WeakReference<>(requestListener));
                    }
                }

        );
    }
}
