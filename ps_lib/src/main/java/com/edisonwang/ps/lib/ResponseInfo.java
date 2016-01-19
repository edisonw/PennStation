package com.edisonwang.ps.lib;

import android.os.Bundle;

/**
 * Info to use in the same process.
 *
 * @author edi
 */
public class ResponseInfo {

    public final String mRequestId;
    public final ActionRequest mRequest;
    public final Bundle mOriginalResultBundle;
    public final long mRequestTime;
    public final long mResultTime;

    public ResponseInfo(Bundle b) {
        mOriginalResultBundle = b;
        mRequestTime = b.getLong(EventServiceImpl.EventServiceConnection.EXTRA_REQUEST_TIME_MS, 0);
        mResultTime = System.currentTimeMillis();
        mRequestId = b.getString(EventServiceImpl.EventServiceConnection.EXTRA_REQUEST_ID);
        mRequest = b.getParcelable(EventServiceImpl.EXTRA_SERVICE_REQUEST);
    }
}
