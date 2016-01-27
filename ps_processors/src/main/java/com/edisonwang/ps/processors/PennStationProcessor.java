package com.edisonwang.ps.processors;

import com.edisonwang.ps.annotations.ClassField;
import com.edisonwang.ps.annotations.EventListener;
import com.edisonwang.ps.annotations.EventProducer;
import com.edisonwang.ps.annotations.ParcelableClassField;
import com.edisonwang.ps.annotations.RequestFactory;
import com.edisonwang.ps.annotations.RequestFactoryWithClass;
import com.edisonwang.ps.annotations.RequestFactoryWithVariables;
import com.edisonwang.ps.annotations.ResultClassWithVariables;
import com.google.auto.service.AutoService;
import com.google.common.base.Joiner;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

/**
 * @author edi
 */
@AutoService(Processor.class)
public class PennStationProcessor extends AbstractProcessor {

    private static final Set<String> NAMES;

    static {
        HashSet<String> set = new HashSet<>();
        set.add(RequestFactory.class.getCanonicalName());
        set.add(RequestFactoryWithClass.class.getCanonicalName());
        set.add(RequestFactoryWithVariables.class.getCanonicalName());
        set.add(EventListener.class.getCanonicalName());
        set.add(EventProducer.class.getCanonicalName());
        set.add(ClassField.class.getCanonicalName());
        NAMES = Collections.unmodifiableSet(set);
    }

    private Filer filer;
    private Messager messager;
    private Elements elementUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();
        elementUtils = processingEnv.getElementUtils();
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
        System.out.println("Processing aggregations:");
        System.out.println(annotations + " \n");
        boolean r = processEventProducersAndListeners(roundEnv);
        boolean r2 = processRequestFactory(roundEnv);
        return r && r2;
    }

    private boolean processRequestFactory(RoundEnvironment roundEnv) {
        Map<String, TypeSpec.Builder> builderMap = new LinkedHashMap<>();
        Map<String, HashSet<String>> groupToPackage = new HashMap<>();
        // Iterate over all @Factory annotated elements
        for (Element element : roundEnv.getElementsAnnotatedWith(RequestFactory.class)) {
            // Check if a class has been annotated with @Factory
            if (element.getKind() != ElementKind.CLASS) {
                error(element, "You cannot annotate " + element.getSimpleName() + " with " + RequestFactory.class);
                return true;
            }
            TypeElement classElement = (TypeElement) element;
            RequestFactory annotationElement = classElement.getAnnotation(RequestFactory.class);

            //Groups of Objects, named.

            String group = annotationElement.group();
            if (group == null || group.length() == 0) {
                throw new IllegalArgumentException(
                        String.format("group() in @%s for class %s is null or empty! that's not allowed",
                                RequestFactory.class.getSimpleName(), classElement.getQualifiedName().toString()));
            }

            String baseClassString;

            try {
                baseClassString = annotationElement.baseClass().getCanonicalName();
            } catch (MirroredTypeException mte) {
                baseClassString = mte.getTypeMirror().toString();
            }

            if (Object.class.getCanonicalName().equals(baseClassString)) {
                baseClassString = "com.edisonwang.ps.lib.ActionKey";
            }

            if (baseClassString == null) {
                throw new IllegalArgumentException(
                        String.format("valueType() in @%s for class %s is null or empty! that's not allowed",
                                RequestFactory.class.getSimpleName(), classElement.getQualifiedName().toString()));
            }

            String valueClassString;

            try {
                valueClassString = annotationElement.valueType().getCanonicalName();
            } catch (MirroredTypeException mte) {
                valueClassString = mte.getTypeMirror().toString();
            }

            if (valueClassString == null) {
                throw new IllegalArgumentException(
                        String.format("valueType() in @%s for class %s is null or empty! that's not allowed",
                                RequestFactory.class.getSimpleName(), classElement.getQualifiedName().toString()));
            }

            if (Object.class.getCanonicalName().equals(valueClassString)) {
                valueClassString = "com.edisonwang.ps.lib.Action";
            }

            TypeName baseClassType = ClassName.bestGuess(baseClassString);
            TypeName valueClassType = ClassName.bestGuess(valueClassString);

            if (valueClassType.toString().equals(classElement.getQualifiedName().toString())) {
                continue;
            }

            String groupId = baseClassString + "_." + group;

            HashSet<String> packageGroups = groupToPackage.get(baseClassString);

            if (packageGroups == null) {
                packageGroups = new HashSet<>();
                groupToPackage.put(baseClassString, packageGroups);
            }

            packageGroups.add(groupId);

            String enumName = classElement.getSimpleName().toString();

            TypeSpec.Builder groupSpec = builderMap.get(groupId);
            if (groupSpec == null) {
                groupSpec = TypeSpec.enumBuilder(group);
                groupSpec.addModifiers(Modifier.PUBLIC);
                groupSpec.addSuperinterface(baseClassType);
                groupSpec.addField(valueClassType,
                        "value", Modifier.PRIVATE, Modifier.FINAL)
                        .addMethod(MethodSpec.constructorBuilder()
                                .addParameter(valueClassType, "value")
                                .addStatement("this.$N = $N", "value", "value")
                                .build());
                groupSpec.addMethod(MethodSpec.methodBuilder("value").addModifiers(Modifier.PUBLIC)
                        .returns(valueClassType).addStatement("return this.value").build());
                builderMap.put(groupId, groupSpec);
            }

            groupSpec.addEnumConstant(enumName,
                    TypeSpec.anonymousClassBuilder("new $L()", classElement) //Empty Constructor required.
                            .build());

            RequestFactoryWithClass factoryAnnotation = classElement.getAnnotation(RequestFactoryWithClass.class);
            if (factoryAnnotation != null) {
                addFactoryMethodToGroupSpec(factoryAnnotation, classElement, enumName, groupSpec);
            }

            RequestFactoryWithVariables variables = classElement.getAnnotation(RequestFactoryWithVariables.class);

            if (variables != null) {
                addFactoryAndFactoryMethod(variables, classElement, enumName, groupSpec, groupId);
            }

        }

        for (String baseClass : groupToPackage.keySet()) {
            HashSet<String> groups = groupToPackage.get(baseClass);
            for (String groupId : groups) {
                writeClass(groupId, builderMap.get(groupId).build(), filer);
            }
        }

        return true;
    }

    private void addFactoryAndFactoryMethod(RequestFactoryWithVariables anno, TypeElement classElement,
                                            String enumName, TypeSpec.Builder groupSpec, String groupId) {

        String baseClassString;

        try {
            baseClassString = anno.baseClass().getCanonicalName();
        } catch (MirroredTypeException mte) {
            baseClassString = mte.getTypeMirror().toString();
        }

        if (Object.class.getCanonicalName().equals(baseClassString)) {
            baseClassString = "com.edisonwang.ps.lib.ActionRequestBuilder";
        }

        if (baseClassString == null) {
            throw new IllegalArgumentException(
                    String.format("baseClass() in @%s for class %s is null or empty! that's not allowed",
                            RequestFactoryWithVariables.class.getSimpleName(), classElement.getQualifiedName().toString()));
        }

        //Only primitives are supported.
        ClassField[] variables = anno.variables();

        String packageName = classElement.getQualifiedName().toString().substring(0, classElement.getQualifiedName().toString().lastIndexOf("."));
        String className = enumName + "Helper";
        String qualifiedName = packageName + "." + className;

        TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .superclass(ClassName.bestGuess(baseClassString));

        MethodSpec.Builder ctr = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);

        ctr.addStatement("mTarget = $L.$L", groupId, enumName).build();

        char c[] = enumName.toCharArray();
        c[0] = Character.toLowerCase(c[0]);
        String methodName = new String(c);

        if (variables.length != 0) {
            ParameterSpec valuesParam = ParameterSpec.builder(
                    ClassName.bestGuess("android.os.Bundle"), "values").build();
            typeBuilder.addMethod(
                    MethodSpec.constructorBuilder().
                            addModifiers(Modifier.PUBLIC).addParameter(valuesParam)
                            .addStatement("mTarget = $L.$L", groupId, enumName)
                            .addStatement("setVariableValues(values)")
                            .build());
            groupSpec.addMethod(MethodSpec.methodBuilder(methodName)
                    .addModifiers(Modifier.FINAL, Modifier.PUBLIC, Modifier.STATIC)
                    .addParameter(valuesParam)
                    .returns(ClassName.bestGuess(qualifiedName)).
                            addStatement("return new " + qualifiedName + "(values)").build());
        }

        ArrayList<String> requiredNames = new ArrayList<>();

        MethodSpec.Builder factoryMethod = MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.FINAL, Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.bestGuess(qualifiedName));

        for (ClassField variable : variables) {
            String name = variable.name();
            String kindName;
            try {
                kindName = variable.kind().getCanonicalName();
            } catch (MirroredTypeException mte) {
                kindName = mte.getTypeMirror().toString();
            }
            TypeName kindClassName = guessTypeName(kindName);
            if (variable.required()) {
                requiredNames.add(name);
                ctr.addParameter(ParameterSpec.builder(kindClassName, name).build());
                ctr.addStatement("mVariableHolder.putExtra(\"$L\", $L)", name, name);
                factoryMethod.addParameter(ParameterSpec.builder(kindClassName, name).build());
            }
            typeBuilder.addMethod(MethodSpec.methodBuilder(name).addStatement(
                    "Object r = get(\"" + name + "\")"
            ).returns(kindClassName).addStatement(
                    "return r == null ? null : (" + kindName + ") r"
            ).addModifiers(Modifier.PUBLIC).build());
            typeBuilder.addMethod(MethodSpec.methodBuilder(name).addParameter(
                    kindClassName, "value"
            ).returns(ClassName.bestGuess(className)).addStatement(
                    "mVariableHolder.putExtra(\"" + name + "\", value)"
            ).addStatement(
                    "return this"
            ).addModifiers(Modifier.PUBLIC).build());
        }

        typeBuilder.addMethod(ctr.build());

        writeClass(packageName, className, typeBuilder.build(), filer);

        factoryMethod.addStatement("return new " + qualifiedName + "(" + Joiner.on(",").join(requiredNames) + ")");

        groupSpec.addMethod(factoryMethod.build());
    }

    private void addFactoryMethodToGroupSpec(RequestFactoryWithClass factoryAnnotation,
                                             TypeElement classElement, String enumName,
                                             TypeSpec.Builder groupSpec) {
        TypeMirror factoryClassMirror = null;
        try {
            factoryAnnotation.factoryClass();
        } catch (MirroredTypeException mte) {
            factoryClassMirror = mte.getTypeMirror();
        }

        if (factoryClassMirror == null) {
            throw new IllegalArgumentException(
                    String.format("factoryClass() in @%s for class %s is null or empty! that's not allowed",
                            RequestFactoryWithClass.class.getSimpleName(),
                            classElement.getQualifiedName().toString()));
        }

        char c[] = enumName.toCharArray();
        c[0] = Character.toLowerCase(c[0]);
        String methodName = new String(c);

        groupSpec.addMethod(MethodSpec.methodBuilder(methodName)
                .addModifiers(Modifier.FINAL, Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeName.get(factoryClassMirror)).addStatement(
                        "return new " + factoryClassMirror.toString() + "()").build());
    }

    private void error(Element e, String msg, Object... args) {
        messager.printMessage(
                Diagnostic.Kind.ERROR,
                String.format(msg, args),
                e);
    }

    private boolean processEventProducersAndListeners(RoundEnvironment roundEnv) {
        HashMap<String, HashSet<String>> producerEvents = new HashMap<>();
        for (Element element : roundEnv.getElementsAnnotatedWith(EventProducer.class)) {
            if (element.getKind() != ElementKind.CLASS) {
                error(element, "You cannot annotate " + element.getSimpleName() + " with " + EventProducer.class);
                return true;
            }
            getEventsFromProducer(producerEvents, (TypeElement) element);
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(EventListener.class)) {
            TypeElement typed = (TypeElement) element;
            EventListener annotationElement = typed.getAnnotation(EventListener.class);
            HashSet<String> producers = getAnnotatedClassesVariable(typed, "producers", EventListener.class);
            HashSet<String> listenedToEvents = new HashSet<>();
            for (String producer : producers) {
                HashSet<String> events = producerEvents.get(producer);
                if (events == null) {
                    events = getEventsFromProducer(producerEvents, elementUtils.getTypeElement(producer));
                    if (events == null) {
                        error(element, "Producer " + producer + " not registered, have you annotated it? ");
                    }
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
                        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT).addParameter(guessTypeName(event), "event").build());
            }
            writeClass(packageName, listenerClassName, typeBuilder.build(), filer);
        }

        return false;
    }

    private HashSet<String> getEventsFromProducer(HashMap<String, HashSet<String>> producerEvents, TypeElement typed) {
        String typedName = typed.getQualifiedName().toString();
        HashSet<String> events = producerEvents.get(typedName);
        if (events == null) {
            events = getAnnotatedClassesVariable(typed, "events", EventProducer.class);

            EventProducer eventProducer = typed.getAnnotation(EventProducer.class);
            for (ResultClassWithVariables resultEvent : eventProducer.generated()) {
                events.add(generateResultClass(typed, resultEvent));
            }

            producerEvents.put(typed.getQualifiedName().toString(), events);
        }

        return events;
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
            TypeName self = guessTypeName(eventClassName);

            if (baseClassString == null) {
                throw new IllegalStateException("Base type not found.");
            }

            TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(eventClassName)
                    .addModifiers(Modifier.PUBLIC)
                    .superclass(guessTypeName(baseClassString));

            for (ParcelableClassFieldParsed p : parsed) {
                typeBuilder.addField(guessTypeName(p.kindName), p.name, Modifier.PUBLIC);
            }

            MethodSpec.Builder ctr = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);

            for (ParcelableClassFieldParsed p : parsed) {
                if (p.required) {
                    ctr.addParameter(guessTypeName(p.kindName), p.name);
                    ctr.addStatement("this.$L = $L", p.name, p.name);
                }
            }
            typeBuilder.addMethod(ctr.build());

            ctr = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
            ctr.addParameter(guessTypeName("android.os.Parcel"), "in");
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
                    .addParameter(guessTypeName("android.os.Parcel"), "dest")
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
                            .addParameter(guessTypeName("android.os.Parcel"), "in")
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

            writeClass(packageName, eventClassName, typeBuilder.build(), filer);

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

        if (am == null) {
            return classes;
        }

        Map<? extends ExecutableElement, ? extends AnnotationValue> v = am.getElementValues();

        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : v.entrySet()) {
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

    public static TypeName guessTypeName(String classNameString) {
        if (classNameString.endsWith("[]")) {
            TypeName typeName = guessTypeName(classNameString.substring(0, classNameString.length() - 2));
            return ArrayTypeName.of(typeName);
        }
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
