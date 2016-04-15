package com.edisonwang.ps.lib;

import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

/**
 * @author edi
 */
public class BundleKey {

    private final Bundle mBundle;
    private Integer mHashCode;

    public BundleKey(Bundle bundle) {
        mBundle = bundle;
    }

    @Override
    public boolean equals(Object b) {
        if (!(b instanceof BundleKey)) {
            return false;
        }
        final Bundle two = ((BundleKey) b).mBundle;
        return !(mBundle == null && two != null) && !(two == null && mBundle != null) && (two == null || equalBundles(mBundle, two));
    }

    @Override
    public int hashCode() {
        if (mHashCode == null) {
            mHashCode = bundleHashCode(mBundle, 0);
        }
        return mHashCode;
    }

    private int bundleHashCode(Bundle bundle, int start) {
        ArrayList<String> keys = new ArrayList<>();
        keys.addAll(bundle.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            Object val = bundle.get(key);
            if (val != null) {
                if (val instanceof Bundle) {
                    start = start * 31 + bundleHashCode((Bundle) val, start);
                } else {
                    start = start * 31 + val.hashCode();
                }
            }
        }
        return start;
    }

    public static boolean equalBundles(Bundle one, Bundle two) {
        if (one.size() != two.size()) {
            return false;
        }

        Set<String> setOne = one.keySet();
        Object valueOne;
        Object valueTwo;

        for (String key : setOne) {
            valueOne = one.get(key);
            valueTwo = two.get(key);
            if (valueOne instanceof Bundle && valueTwo instanceof Bundle &&
                    !equalBundles((Bundle) valueOne, (Bundle) valueTwo)) {
                return false;
            } else if (valueOne == null) {
                if (valueTwo != null || !two.containsKey(key))
                    return false;
            } else if (!valueOne.equals(valueTwo)) {
                Log.i("LOL", " " + valueOne + "  0  " + valueTwo);
                return false;
            }
        }

        return true;
    }
}
