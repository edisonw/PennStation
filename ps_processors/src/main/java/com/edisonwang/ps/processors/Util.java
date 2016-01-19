package com.edisonwang.ps.processors;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

/**
 * @author edi (@wew)
 */
public final class Util {

    private Util() {

    }

    public static TypeName guessTypeName(String classNameString) {
        if (double.class.getName().equals(classNameString)) {
            return TypeName.DOUBLE;
        } else if (int.class.getName().equals(classNameString)) {
            return TypeName.INT;
        } else if (boolean.class.getName().equals(classNameString)) {
            return TypeName.BOOLEAN;
        } else if (float.class.getName().equals(classNameString)) {
            return TypeName.FLOAT;
        } else if (byte.class.getName().equals(classNameString)) {
            return TypeName.BYTE;
        } else if (char.class.getName().equals(classNameString)) {
            return TypeName.CHAR;
        } else if (long.class.getName().equals(classNameString)) {
            return TypeName.LONG;
        } else if (short.class.getName().equals(classNameString)) {
            return TypeName.SHORT;
        }
        return ClassName.bestGuess(classNameString);
    }
}
