package com.edisonwang.ps.rxpennstation;

import android.os.Bundle;

import com.edisonwang.ps.lib.ActionRequest;
import com.edisonwang.ps.lib.ActionResult;
import com.edisonwang.ps.lib.PennStation;
import com.edisonwang.ps.lib.Requester;

import java.util.HashSet;

import rx.Observable;
import rx.Subscriber;

/**
 * @author edi
 */
class ActionResultOnSubscribe<T> implements Observable.OnSubscribe<T> {
    private PsRxFactory mFactory;
    private final ActionRequest mRequest;
    private final HashSet<Requester.RequestListener> mListeners = new HashSet<>(3);

    public ActionResultOnSubscribe(PsRxFactory T, ActionRequest request) {
        mFactory = T;
        mRequest = request;
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
                    if (mFactory.type.isAssignableFrom(result.getClass())) {
                        subscriber.onNext((T) result);
                    }
                }
                synchronized (mListeners) {
                    mListeners.remove(this);
                }
            }

            @Override
            public void onCancelled(String requestId) {

            }
        };

        synchronized (mListeners) {
            mListeners.add(requestListener);
        }

        Requester.singleFire(mRequest, PennStation.getManager(), requestListener);
    }
}
