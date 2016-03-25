package com.edisonwang.ps.processors;

import com.squareup.javapoet.TypeName;

/**
 * For ArrayList<String>
 *     type = ArrayList<String>
 *     name = ArrayList<String>
 *     base = ArrayList
 * @author edi
 */
final class ParsedKind {
    public final TypeName type;
    public final String name;
    public final String base;

    public ParsedKind(TypeName type, String name, String base) {
        this.type = type;
        this.name = name;
        this.base = base;
    }
}
