package com.edisonwang.ps.rxpennstation;

import android.os.Bundle;

import com.edisonwang.ps.lib.ActionRequest;
import com.edisonwang.ps.lib.ActionResult;
import com.edisonwang.ps.lib.PennStation;
import com.edisonwang.ps.lib.Requester;

import java.lang.ref.WeakReference;

import rx.Observable;
import rx.Subscriber;

/**
 * @author edi
 */
class ActionResultOnSubscribe<T> implements Observable.OnSubscribe<T> {
    private PsRxFactory t;
    private final ActionRequest request;

    public static class RequestError extends Throwable {
        public final ActionResult failedResult;

        public RequestError(ActionResult failedResult) {
            this.failedResult = failedResult;
        }
    }

    public ActionResultOnSubscribe(PsRxFactory T, ActionRequest request) {
        t = T;
        this.request = request;
    }

    @Override
    public void call(final Subscriber<? super T> subscriber) {
        Requester.RequestListener requestListener = new Requester.RequestListener() {
            @Override
            public void onRequested(Bundle bundle, String requestId) {

            }

            @Override
            public void onCompleted(String reqId, ActionResult result) {
                if (!subscriber.isUnsubscribed()) {
                    if (t.type.isAssignableFrom(result.getClass())) {
                        subscriber.onNext((T) result);
                    } else if (!result.isSuccess()) {
                        subscriber.onError(new RequestError(result));
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
