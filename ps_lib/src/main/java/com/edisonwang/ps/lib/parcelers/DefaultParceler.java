package com.edisonwang.ps.lib.parcelers;

import android.os.Parcel;

/**
 * @author edi
 */
public class DefaultParceler {

    public static Object readFromParcel(Parcel in, Class clazz) {
        return in.readValue(clazz.getClassLoader());
    }

    public static void writeToParcel(Object value, Parcel dest, int flag) {
        dest.writeValue(value);
    }
}
