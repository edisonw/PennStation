package com.edisonwang.ps.rxpennstation;

import com.edisonwang.ps.lib.ActionRequest;
import com.edisonwang.ps.lib.ActionRequestHelper;

import rx.Observable;
import rx.subjects.PublishSubject;

/**
 * @author edi
 */
public class PsRxFactory<T> {

    final Class<T> type;

    final PublishSubject<T> bus = PublishSubject.create();

    public PsRxFactory(Class<T> type) {
        this.type = type;
    }

    public Observable<T> from(final ActionRequestHelper helper) {
        return from(helper.buildRequest());
    }

    public Observable<T> from(final ActionRequest request) {
        return Observable.create(new ActionResultOnSubscribe<T>(this, request));
    }

    public Observable<T> observable() {
        return bus.asObservable();
    }

    public void send(T event) {
        if (bus.hasObservers()) {
            bus.onNext(event);
        }
    }
}
