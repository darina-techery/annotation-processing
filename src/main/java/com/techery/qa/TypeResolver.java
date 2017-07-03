package com.techery.qa;

import com.squareup.javapoet.ClassName;

import javax.lang.model.type.TypeMirror;
import java.util.HashSet;
import java.util.Set;

public class TypeResolver {
	public static final Set<String> DEVICE_PREFIXES = new HashSet<>();

	static  {
		DEVICE_PREFIXES.add("IOS");
		DEVICE_PREFIXES.add("Droid");
	}

	public static ClassName getClassName(TypeMirror typeMirror) {
		String fullyQualifiedClassName = typeMirror.toString();
		String simpleClassName = getSimpleName(fullyQualifiedClassName);
		String packageName = getPackageName(fullyQualifiedClassName);
		return ClassName.get(packageName, simpleClassName);
	}

	public static ClassName getClassNameWithPrefix(TypeMirror typeMirror, String prefix) {
		String fullyQualifiedClassName = typeMirror.toString();
		String simpleClassName = prefix + getSimpleName(fullyQualifiedClassName);
		String packageName = getPackageName(fullyQualifiedClassName);
		return ClassName.get(packageName, simpleClassName);
	}

	public static String getPackageName(String fullyQualifiedClassName) {
		int dotIndex = fullyQualifiedClassName.lastIndexOf(".");
		return fullyQualifiedClassName.substring(0, dotIndex);
	}

	public static String getPackageName(TypeMirror typeMirror) {
		return getPackageName(typeMirror.toString());
	}

	public static String getSimpleName(String fullyQualifiedClassName) {
		int dotIndex = fullyQualifiedClassName.lastIndexOf(".");
		return fullyQualifiedClassName.substring(dotIndex + 1, fullyQualifiedClassName.length());
	}

	public static String getSimpleName(TypeMirror element) {
		return getSimpleName(element.toString());
	}
}
