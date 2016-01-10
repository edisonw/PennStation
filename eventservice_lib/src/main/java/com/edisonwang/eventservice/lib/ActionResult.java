package com.edisonwang.eventservice.lib;

import android.os.Parcelable;

/**
 * @author edi
 */
public abstract class ActionResult implements Parcelable {

    private ResponseInfo mResponseInfo = null;

    public void setResponseInfo(ResponseInfo responseInfo) {
        mResponseInfo = responseInfo;
    }

    public ResponseInfo getResponseInfo() {
        return mResponseInfo;
    }
}
