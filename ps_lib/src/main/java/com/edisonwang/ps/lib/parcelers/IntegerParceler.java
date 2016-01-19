package com.edisonwang.ps.lib.parcelers;

import android.os.Parcel;

/**
 * @author edi
 */
public class IntegerParceler {

    public static int readFromParcel(Parcel in, Class clazz) {
        return in.readInt();
    }

    public static void writeToParcel(int value, Parcel dest, int flag) {
        dest.writeInt(value);
    }
}
