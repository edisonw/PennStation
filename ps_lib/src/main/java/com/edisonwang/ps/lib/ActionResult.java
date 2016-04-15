package com.edisonwang.ps.lib;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author edi
 */
public abstract class ActionResult implements Parcelable {

    private boolean mPostSticky = false;

    /**
     * Not parceled since it is added on the requesting thread.
     */
    private ResponseInfo mResponseInfo = null;

    public ActionResult() {

    }

    protected ActionResult(Parcel in) {
        mPostSticky = in.readByte() != 0;
    }

    public boolean postSticky() {
        return mPostSticky;
    }

    public void setPostSticky(boolean postSticky) {
        mPostSticky = postSticky;
    }

    public ResponseInfo getResponseInfo() {
        return mResponseInfo;
    }

    void setResponseInfo(ResponseInfo responseInfo) {
        mResponseInfo = responseInfo;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (mPostSticky ? 1 : 0));
    }

    public boolean isSuccess() {
        return true;
    }
}
