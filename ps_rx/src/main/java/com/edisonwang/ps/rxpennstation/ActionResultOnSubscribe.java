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
    private final PsRxFactory<T> mFactory;
    private final ActionRequest mRequest;
    private final HashSet<SingleRequestListener> mListeners = new HashSet<>(3);

    public ActionResultOnSubscribe(PsRxFactory<T> factory, ActionRequest request) {
        mFactory = factory;
        mRequest = request;
    }

    @Override
    public void call(final Subscriber<? super T> subscriber) {
        SingleRequestListener requestListener = new SingleRequestListener(subscriber);

        synchronized (mListeners) {
            mListeners.add(requestListener);
        }

        Requester.singleFire(mRequest, PennStation.getManager(), requestListener);
    }

    private class SingleRequestListener implements Requester.RequestListener {

        private final Subscriber<? super T> mSubscriber;

        public SingleRequestListener(Subscriber<? super T> subscriber) {
            mSubscriber = subscriber;
        }

        @Override
        public void onRequested(Bundle bundle, String requestId) {

        }

        @Override
        public void onCompleted(String reqId, ActionResult result) {
            if (!mSubscriber.isUnsubscribed()) {
                if (mFactory.type.isAssignableFrom(result.getClass())) {
                    mSubscriber.onNext((T) result);
                }
            }
            synchronized (mListeners) {
                if (mListeners.contains(this)) {
                    mListeners.remove(this);
                }
            }
        }

        @Override
        public void onCancelled(String requestId) {

        }
    }
}
