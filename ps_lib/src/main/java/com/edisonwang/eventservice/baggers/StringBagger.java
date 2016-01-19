package com.edisonwang.eventservice.baggers;

import android.os.Parcel;

/**
 * @author edi
 */
public class StringBagger {

    public static String readFromParcel(Parcel in, Class clazz) {
        return in.readString();
    }

    public static void writeToParcel(String value, Parcel dest, int flag) {
        dest.writeString(value);
    }
}
