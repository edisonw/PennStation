package com.edisonwang.ps.rxpennstation;

import com.edisonwang.ps.lib.ActionRequest;
import com.edisonwang.ps.lib.ActionRequestHelper;

import rx.Observable;

/**
 * Right now there's no point to make this public
 * But when Annotation Support arrives for Jack and Jill.
 *
 * @author edi
 */
public class PsRxFactory<T> {

    final Class<T> type;

    public PsRxFactory(Class<T> type) {
        this.type = type;
    }

    public Observable<T> from(final ActionRequestHelper helper) {
        return from(helper.buildRequest());
    }

    public Observable<T> from(final ActionRequest request) {
        return Observable.create(new ActionResultOnSubscribe<T>(this, request));
    }

}
