package com.edisonwang.ps.processors;

import com.edisonwang.ps.annotations.EventListener;
import com.edisonwang.ps.annotations.EventProducer;
import com.edisonwang.ps.annotations.ParcelableClassField;
import com.edisonwang.ps.annotations.ResultClassWithVariables;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

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

            String listenerClassName = typed.getSimpleName().toString() + EventListener.class.getSimpleName();
            String originalClassName = typed.getQualifiedName().toString();
            String packageName = packageFromQualifiedName(originalClassName);

            TypeSpec.Builder typeBuilder = TypeSpec.interfaceBuilder(listenerClassName).addModifiers(Modifier.PUBLIC);
            for (String event : listenedToEvents) {
                typeBuilder.addMethod(MethodSpec.methodBuilder(
                        (annotationElement.restrictMainThread() ? "onEventMainThread" : "onEvent"))
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT).addParameter(Util.guessTypeName(event), "event").build());
            }
            Util.writeClass(packageName, listenerClassName, typeBuilder.build(), filer);
        }

        return true;
    }

    private String generateResultClass(TypeElement typed, ResultClassWithVariables resultEvent) {
        String baseClassString;
        try {
            baseClassString = resultEvent.baseClass().getCanonicalName();
        } catch (MirroredTypeException mte) {
            baseClassString = mte.getTypeMirror().toString();
        }

        if (Object.class.getCanonicalName().equals(baseClassString)) {
            baseClassString = "com.edisonwang.ps.lib.ActionResult";
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
                parcelerName = field.parceler().getCanonicalName();
            } catch (MirroredTypeException mte) {
                parcelerName = mte.getTypeMirror().toString();
            }

            if (parcelerName.equals(Object.class.getCanonicalName())) {
                parcelerName = "com.edisonwang.ps.lib.parcelers.DefaultParceler";
            }

            parsed.add(new ParcelableClassFieldParsed(field.name(), kindName,
                    parcelerName, field.required()));
        }

        try {
            String eventClassName = typed.getSimpleName().toString() + "Event" +
                    resultEvent.classPostFix();
            String originalClassName = typed.getQualifiedName().toString();
            String packageName = packageFromQualifiedName(originalClassName);
            TypeName self = Util.guessTypeName(eventClassName);

            if (baseClassString == null) {
                throw new IllegalStateException("Base type not found.");
            }

            TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(eventClassName)
                    .addModifiers(Modifier.PUBLIC)
                    .superclass(Util.guessTypeName(baseClassString));

            for (ParcelableClassFieldParsed p : parsed) {
                typeBuilder.addField(Util.guessTypeName(p.kindName), p.name, Modifier.PUBLIC);
            }

            MethodSpec.Builder ctr = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);

            for (ParcelableClassFieldParsed p : parsed) {
                if (p.required) {
                    ctr.addParameter(Util.guessTypeName(p.kindName), p.name);
                    ctr.addStatement("this.$L = $L", p.name, p.name);
                }
            }
            typeBuilder.addMethod(ctr.build());

            ctr = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
            ctr.addParameter(Util.guessTypeName("android.os.Parcel"), "in");
            for (ParcelableClassFieldParsed p : parsed) {
                ctr.addStatement("\tthis." + p.name + " = (" + p.kindName + ")" + p.parcelerName + ".readFromParcel(in, " + p.kindName + ".class)");
            }
            typeBuilder.addMethod(ctr.build());
            typeBuilder.addMethod(MethodSpec.methodBuilder("describeContents")
                    .returns(int.class)
                    .addStatement("return 0")
                    .addModifiers(Modifier.PUBLIC).build());

            MethodSpec.Builder writeToParcel = MethodSpec.methodBuilder("writeToParcel")
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(Util.guessTypeName("android.os.Parcel"), "dest")
                    .addParameter(TypeName.INT, "flags");
            for (ParcelableClassFieldParsed p : parsed) {
                writeToParcel.addStatement("$L.writeToParcel(this.$L, dest, flags)", p.parcelerName, p.name);
            }

            typeBuilder.addMethod(writeToParcel.build());

            ClassName creatorClassName = ClassName.bestGuess("android.os.Parcelable.Creator");

            TypeSpec creator = TypeSpec.anonymousClassBuilder("")
                    .addSuperinterface(ParameterizedTypeName.get(creatorClassName, self))
                    .addMethod(MethodSpec.methodBuilder("createFromParcel")
                            .addModifiers(Modifier.PUBLIC)
                            .returns(self)
                            .addParameter(Util.guessTypeName("android.os.Parcel"), "in")
                            .addStatement("return new $L(in)", eventClassName)
                            .build())
                    .addMethod(MethodSpec.methodBuilder("newArray")
                            .addModifiers(Modifier.PUBLIC)
                            .returns(ArrayTypeName.of(self))
                            .addParameter(TypeName.INT, "size")
                            .addStatement("return new $L[size]", eventClassName)
                            .build()
                    ).build();

            typeBuilder.addField(FieldSpec.builder(ParameterizedTypeName.get(creatorClassName, self),
                    "CREATOR", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$L", creator).build());

            Util.writeClass(packageName, eventClassName, typeBuilder.build(), filer);

            return packageName + "." + eventClassName;
        } catch (Throwable e) {
            throw new IllegalArgumentException("Failed to write.", e);
        }
    }

    private String packageFromQualifiedName(String originalClassName) {
        return originalClassName.substring(0, originalClassName.lastIndexOf("."));
    }

    private HashSet<String> getAnnotatedClassesVariable(TypeElement element, String name, Class clazz) {
        HashSet<String> classes = new HashSet<>();

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

        if (annotationEventValue != null) {
            List eventClasses = (List) annotationEventValue.getValue();
            for (Object c : eventClasses) {
                String extraLongClassName = c.toString();
                String regularClassName = extraLongClassName.substring(0, extraLongClassName.length() - ".class".length());
                classes.add(regularClassName);
            }
        }
        return classes;
    }

    private void error(Element e, String msg, Object... args) {
        messager.printMessage(
                Diagnostic.Kind.ERROR,
                String.format(msg, args),
                e);
        throw new IllegalStateException("Failed to process some annotation.");
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

}
