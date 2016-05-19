package com.edisonwang.ps.lib;

import android.os.Parcelable;

/**
 * @author edi
 */
public interface Requirement extends Parcelable {
    Condition get();
}
