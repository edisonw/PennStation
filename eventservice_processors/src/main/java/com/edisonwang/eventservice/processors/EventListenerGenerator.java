package com.edisonwang.eventservice.processors;

import com.edisonwang.eventservice.annotations.EventListener;
import com.edisonwang.eventservice.annotations.EventProducer;
import com.edisonwang.eventservice.annotations.ParcelableClassField;
import com.edisonwang.eventservice.annotations.ResultClassWithVariables;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * Annotated Class -> List of Producers -> List of Events -> List of methods.
 *
 * @author edi
 */
@AutoService(Processor.class)
public class EventListenerGenerator extends AbstractProcessor {

    private static final Set<String> NAMES;

    static {
        HashSet<String> set = new HashSet<>(2);
        set.add(EventListener.class.getCanonicalName());
        set.add(EventProducer.class.getCanonicalName());
        NAMES = Collections.unmodifiableSet(set);
    }

    private Messager messager;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return NAMES;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        HashMap<String, HashSet<String>> producerEvents = new HashMap<>();
        for (Element element : roundEnv.getElementsAnnotatedWith(EventProducer.class)) {
            if (element.getKind() != ElementKind.CLASS) {
                error(element, "You cannot annotate " + element.getSimpleName() + " with " + EventProducer.class);
                return true;
            }
            TypeElement typed = (TypeElement) element;
            HashSet<String> events = getAnnotatedClassesVariable(typed, "events", EventProducer.class);

            EventProducer eventProducer = typed.getAnnotation(EventProducer.class);
            for (ResultClassWithVariables resultEvent : eventProducer.generated()) {
                events.add(generateResultClass(typed, resultEvent));
            }
            producerEvents.put(typed.getQualifiedName().toString(), events);
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(EventListener.class)) {
            TypeElement typed = (TypeElement) element;
            EventListener annotationElement = typed.getAnnotation(EventListener.class);
            HashSet<String> producers = getAnnotatedClassesVariable(typed, "producers", EventListener.class);
            HashSet<String> listenedToEvents = new HashSet<>();
            for (String producer : producers) {
                HashSet<String> events = producerEvents.get(producer);
                if (events == null) {
                    error(element, "Producer not registered, have you annotated it? ");
                    return true;
                }
                listenedToEvents.addAll(events);
            }

            String eventClassName = typed.getSimpleName().toString() + EventListener.class.getSimpleName();
            String originalClassName = typed.getQualifiedName().toString();
            String packageName = packageFromQualifiedName(originalClassName);

            TypeSpec.Builder typeBuilder = TypeSpec.interfaceBuilder(eventClassName).addModifiers(Modifier.PUBLIC);
            for (String event : listenedToEvents) {
                typeBuilder.addMethod(MethodSpec.methodBuilder(
                        (annotationElement.restrictMainThread() ? "onEventMainThread" : "onEvent"))
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT).addParameter(ClassName.bestGuess(event), "event").build());
            }
            try {
                Writer writer = filer.createSourceFile(packageName + "." + eventClassName).openWriter();
                JavaFile jf = JavaFile.builder(packageName, typeBuilder.build()).build();
                jf.writeTo(writer);
                writer.close();
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to write class.", e);
            }
        }

        return true;
    }

    private static class ParcelableClassFieldParsed {

        public final String name;
        public final String kindName;
        public final String parcelerName;
        public final boolean required;

        public ParcelableClassFieldParsed(String name, String kindName,
                                          String parcelerName, boolean required) {
            this.name = name;
            this.kindName = kindName;
            this.parcelerName = parcelerName;
            this.required = required;
        }
    }

    private String generateResultClass(TypeElement typed, ResultClassWithVariables resultEvent) {
        TypeMirror baseTypeMirror = null;
        try {
            resultEvent.baseClass();
        } catch (MirroredTypeException mte) {
            baseTypeMirror = mte.getTypeMirror();
        }

        List<ParcelableClassFieldParsed> parsed = new ArrayList<>();

        ParcelableClassField[] fields = resultEvent.fields();
        for (ParcelableClassField field : fields) {
            String kindName;
            try {
                kindName = field.kind().toString();
            } catch (MirroredTypeException mte) {
                kindName = mte.getTypeMirror().toString();
            }
            String parcelerName;
            try {
                parcelerName = field.parceler().getName();
            } catch (MirroredTypeException mte) {
                parcelerName = mte.getTypeMirror().toString();
            }

            parsed.add(new ParcelableClassFieldParsed(field.name(), kindName,
                    parcelerName, field.required()));
        }

        try {
            String eventClassName = typed.getSimpleName().toString() + "Event" +
                    resultEvent.classPostFix();
            String originalClassName = typed.getQualifiedName().toString();
            String packageName = packageFromQualifiedName(originalClassName);

            JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(packageName + "." + eventClassName);
            Writer writer = sourceFile.openWriter();
            writer.write("package " + packageName + ";\n");
            writer.write("import android.os.Parcel;\n");
            writer.write("public class " + eventClassName + " extends " + baseTypeMirror.toString() + "  {\n\n");
            int requiredSize = 0;
            for (ParcelableClassFieldParsed p : parsed) {
                writer.write("public " + p.kindName + " " + p.name + ";\n");
                if (p.required) {
                    requiredSize++;
                }
            }
            writer.write("\npublic " + eventClassName + "(");
            int i = 1;
            for (ParcelableClassFieldParsed p : parsed) {
                if (p.required) {
                    writer.write(p.kindName + " " + p.name);
                    if (i != requiredSize) {
                        writer.write(", ");
                    }
                }
                i++;
            }
            writer.write(") {\n");
            for (ParcelableClassFieldParsed p : parsed) {
                if (p.required) {
                    writer.write("\tthis." + p.name + " = " + p.name + ";\n");
                }
            }
            writer.write("}\n\n");

            writer.write("public " + eventClassName + "(Parcel in) {\n");
            for (ParcelableClassFieldParsed p : parsed) {
                writer.write("\tthis." + p.name + " = (" + p.kindName + ")" + p.parcelerName + ".readFromParcel(in, " + p.kindName + ".class );\n");
            }
            writer.write("}\n");
            writer.write("\n" +
                    "    @Override\n" +
                    "    public int describeContents() {\n" +
                    "        return 0;\n" +
                    "    }\n" +
                    "\n");
            writer.write("" +
                    "@Override\n" +
                    "    public void writeToParcel(Parcel dest, int flags) {\n");
            for (ParcelableClassFieldParsed p : parsed) {
                writer.write("\t" + p.parcelerName + ".writeToParcel(this." + p.name + ", dest, flags);\n");
            }
            writer.write("}\n");
            writer.write("\n" +
                    "\n" +
                    "    public static final Creator<" + eventClassName + "> CREATOR = " +
                    "new Creator<" + eventClassName + ">() {\n" +
                    "        @Override\n" +
                    "        public " + eventClassName + " createFromParcel(Parcel in) {\n" +
                    "            return new " + eventClassName + "(in);\n" +
                    "        }\n" +
                    "\n" +
                    "        @Override\n" +
                    "        public " + eventClassName + "[] newArray(int size) {\n" +
                    "            return new " + eventClassName + "[size];\n" +
                    "        }\n" +
                    "    };\n");

            writer.write("}\n");
            writer.close();
            return packageName + "." + eventClassName;
        } catch (Throwable e) {
            throw new IllegalArgumentException("Failed to write.", e);
        }
    }

    private String packageFromQualifiedName(String originalClassName) {
        return originalClassName.substring(0, originalClassName.lastIndexOf("."));
    }

    private HashSet<String> getAnnotatedClassesVariable(TypeElement element, String name, Class clazz) {
        HashSet<String> events = new HashSet<>();

        AnnotationMirror am = null;
        List<? extends AnnotationMirror> mirrors = element.getAnnotationMirrors();
        for (AnnotationMirror mirror : mirrors) {
            if (mirror.getAnnotationType().toString().equals(clazz.getCanonicalName())) {
                am = mirror;
                break;
            }
        }
        AnnotationValue annotationEventValue = null;

        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am.getElementValues().entrySet()) {
            if (name.equals(entry.getKey().getSimpleName().toString())) {
                annotationEventValue = entry.getValue();
                break;
            }
        }

        List eventClasses = (List) annotationEventValue.getValue();
        for (Object c : eventClasses) {
            String extraLongClassName = c.toString();
            String regularClassName = extraLongClassName.substring(0, extraLongClassName.length() - ".class".length());
            events.add(regularClassName);
        }
        return events;
    }

    private void error(Element e, String msg, Object... args) {
        messager.printMessage(
                Diagnostic.Kind.ERROR,
                String.format(msg, args),
                e);
        throw new IllegalStateException("Failed to process some annotation.");
    }

}
