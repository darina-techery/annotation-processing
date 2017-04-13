package com.techery.qa;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static com.techery.qa.TypeResolver.DEVICE_PREFIXES;
import static javax.tools.Diagnostic.Kind.ERROR;

class Validator {
	private Elements elementUtils;
	private Types typeUtils;
	private Messager messager;
	private List<String> validationErrors;
	static final Collector<CharSequence, ?, String> JOIN_BY_COMMA = Collectors.joining(", ");
	static final String ERROR_MESSAGE_PREFIX = "[VALIDATION ERROR]:\n";
	Validator(ProcessingEnvironment processingEnvironment) {
		elementUtils = processingEnvironment.getElementUtils();
		messager = processingEnvironment.getMessager();
		typeUtils = processingEnvironment.getTypeUtils();
	}

	void validateActionsClasses(Set<TypeMirror> actionsClasses) {
		for (TypeMirror baseActionsClass : actionsClasses) {
			String baseClassName = TypeResolver.getSimpleName(baseActionsClass);
			for (String prefix : DEVICE_PREFIXES) {
				String deviceSpecificClassName = prefix + baseClassName;
				String fullDeviceSpecificClassName = TypeResolver.getPackageName(baseActionsClass)
						+ "." + deviceSpecificClassName;
				TypeElement deviceSpecificElement = elementUtils
						.getTypeElement(fullDeviceSpecificClassName);
				if (deviceSpecificElement == null) {
					messager.printMessage(ERROR, ERROR_MESSAGE_PREFIX +
							"Class [" + deviceSpecificClassName + "] was not found.\n\n");
				} else if (deviceSpecificElement.getSuperclass() == null
							|| !typeUtils.isAssignable(deviceSpecificElement.asType(), baseActionsClass)) {
					messager.printMessage(ERROR, ERROR_MESSAGE_PREFIX +
							"Class [" + deviceSpecificClassName
							+ "] should extend [" + baseActionsClass + "], but ["
							+ deviceSpecificElement.getSuperclass() + "] found instead.\n\n");
				}
			}
		}
	}

	private boolean checkArgumentTypes(ExecutableElement method, Set<TypeMirror> parameterTypes) {
		List<? extends VariableElement> arguments = method.getParameters();

		boolean hasProperArguments = arguments.stream()
				.map(Element::asType)
				.collect(Collectors.toList())
				.containsAll(parameterTypes);
		if (!hasProperArguments) {
			validationErrors.add("Method ["+method.getSimpleName()
					+ "] in [" + method.getEnclosingElement().getSimpleName()
					+ "] should have parameters {"
					+ parameterTypes.stream().map(TypeMirror::toString).collect(JOIN_BY_COMMA)
					+ "}, but found {" + arguments.stream().map(a->a.asType().toString()).collect(JOIN_BY_COMMA)
					+" }.");
			return false;
		}
		return true;
	}

	private boolean checkAnnotation(Element element, String annotationName) {
		List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
		TypeElement annotationType = elementUtils.getTypeElement(annotationName);
		boolean hasAnnotation = annotationMirrors.stream()
				.anyMatch(a -> a.getAnnotationType().asElement().equals(annotationType));
		if (!hasAnnotation) {
			validationErrors.add("Element ["+element.getSimpleName()
					+ "] in [" + element.getEnclosingElement().getSimpleName()
					+ "] should have annotation @" + annotationType.getSimpleName()
					+ ", but found {" + annotationMirrors.stream().map(am -> am.getAnnotationType().toString()).collect(JOIN_BY_COMMA)
					+ "}."
			);
			return false;
		}
		return true;
	}


}
