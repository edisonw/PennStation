package com.edisonwang.ps.processors;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.io.Writer;

import javax.annotation.processing.Filer;
import javax.lang.model.element.TypeElement;

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

    public static void writeClass(String path,
                                  TypeSpec typeSpec,
                                  Filer filer) {
        writeClass(
                path,
                filer,
                JavaFile.builder(path.substring(0, path.lastIndexOf(".")), typeSpec).build());
    }

    private static void writeClass(String path, Filer filer, JavaFile jf) {
        try {
            Writer writer = filer.createSourceFile(path).openWriter();
            jf.writeTo(writer);
            writer.close();
            System.out.println("Generated " + path);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to generate class: " + path, e);
        }
    }

    public static void writeClass(String packageName,
                                  String className,
                                  TypeSpec typeSpec,
                                  Filer filer) {
        writeClass(
                packageName + "." + className,
                filer,
                JavaFile.builder(packageName, typeSpec).build());
    }
}
