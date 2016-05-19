package com.edisonwang.ps.lib;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author edi
 */
public class EventServiceCallback implements Parcelable {

    public static final Creator<EventServiceCallback> CREATOR =
            new Creator<EventServiceCallback>() {
                public EventServiceCallback createFromParcel(Parcel source) {
                    return new EventServiceCallback(source);
                }

                public EventServiceCallback[] newArray(int size) {
                    return new EventServiceCallback[size];
                }
            };

    private final DefaultServiceImpl.EventServiceResponder mResponder;

    /**
     * Construct a callback object from an interface.
     * This constructor is used by the service consumer.
     *
     * @param responder The callback interface
     */
    public EventServiceCallback(DefaultServiceImpl.EventServiceResponder responder) {
        mResponder = responder;
    }

    /**
     * Constructs a callback from a parcel.
     *
     * @param parcel The parcel from which to buildRequest the callback
     */
    public EventServiceCallback(Parcel parcel) {
        mResponder = (DefaultServiceImpl.EventServiceResponder) parcel.readStrongBinder();
    }

    public DefaultServiceImpl.EventServiceResponder getResponder() {
        return mResponder;
    }

    /**
     * The operation complete callback
     *
     * @param bundle The output bundle
     */
    public void onServiceResponse(Bundle bundle) {
        mResponder.onServiceResponse(bundle);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStrongBinder(mResponder);
    }

}
