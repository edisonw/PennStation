package com.edisonwang.eventservice.processors;

import com.edisonwang.eventservice.annotations.EventListener;
import com.edisonwang.eventservice.annotations.EventProducer;
import com.google.auto.service.AutoService;

import java.io.IOException;
import java.io.Writer;
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
import javax.lang.model.element.TypeElement;
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

    private Filer filer;
    private Messager messager;

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

            try {
                JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(packageName + "." + eventClassName);
                Writer writer = sourceFile.openWriter();
                writer.write("package " + packageName + ";\n");
                for (String event : listenedToEvents) {
                    writer.write("import " + event +";\n");
                }
                writer.write("public interface " + eventClassName + " {\n\n");
                for (String event : listenedToEvents) {
                    writer.write("public void " + (annotationElement.restrictMainThread() ? "onEventMainThread" : "onEvent") +"(" +
                            event + " event"+ ");\n\n");
                }
                writer.write("}\n");
                writer.close();
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to write.", e);
            }
        }

        return true;
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
