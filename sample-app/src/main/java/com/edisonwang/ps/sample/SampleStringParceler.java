package com.edisonwang.ps.sample;

import android.os.Parcel;

/**
 * @author edi
 */
public class SampleStringParceler {

    public static String readFromParcel(Parcel in, Class clazz) {
        return in.readString();
    }

    public static void writeToParcel(String string, Parcel dest, int flags) {
        dest.writeString(string);
    }
}
