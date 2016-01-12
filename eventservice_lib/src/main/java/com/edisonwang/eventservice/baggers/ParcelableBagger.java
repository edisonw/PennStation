package com.edisonwang.eventservice.baggers;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author edi
 */
public class ParcelableBagger {

    public static Parcelable readFromParcel(Parcel in, Class clazz) {
        return in.readParcelable(clazz.getClassLoader());
    }

    public static void writeToParcel(Parcelable value, Parcel dest, int flag) {
        dest.writeParcelable(value, flag);
    }
}
