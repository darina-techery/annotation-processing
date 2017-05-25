package com.techery.qa;

import com.squareup.javapoet.*;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.techery.qa.TypeResolver.DEVICE_PREFIXES;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.tools.Diagnostic.Kind.ERROR;

public class Generator {

	private static final String DAGGER_PACKAGE = "dagger";
	private static final String ACTIONS_MODULE_CLASS_NAME = "ActionsModule";
	private static final String STEPS_MODULE_CLASS_NAME = "StepsModule";
	private static final ClassName ACTION_DEFINITIONS_CLASS_NAME =
			ClassName.get("actions.definitions", "ActionsDefinition");
	private static final ClassName MODULE_ANNOTATION_NAME =
			ClassName.get(DAGGER_PACKAGE, "Module");
	private static final ClassName PROVIDES_ANNOTATION_NAME =
			ClassName.get(DAGGER_PACKAGE, "Provides");

	private final Filer filer;
	private final Messager messager;
	private StepsActionsStructure structure;

	Generator(ProcessingEnvironment processingEnv) {
		filer = processingEnv.getFiler();
		messager = processingEnv.getMessager();
	}

	void generateDaggerStructure(StepsActionsStructure structure) {
		this.structure = structure;
		generateActionsDefinition();
		generateActionsModule();
		generateStepsModule();
		generateStepsComponent();
	}

	void generateMissingActionsClass(TypeMirror parentActionsClass, String deviceSpecificClassName) {
		final String packageName = TypeResolver.getPackageName(parentActionsClass);
		final ClassName parentActionsClassName = TypeResolver.getClassName(parentActionsClass);
		TypeSpec.Builder typeBuilder = TypeSpec
				.classBuilder(deviceSpecificClassName)
				.addModifiers(PUBLIC)
				.superclass(parentActionsClassName);
		createJavaFile(packageName, typeBuilder.build());
	}

	private String decapitalize(String str) {
		return Character.toLowerCase(str.charAt(0)) + str.substring(1);
	}

	private void generateActionsDefinition() {
		final String actionsDefinitionInterfaceName = ACTION_DEFINITIONS_CLASS_NAME.simpleName();
		final String packageName = ACTION_DEFINITIONS_CLASS_NAME.packageName();
		TypeSpec.Builder typeBuilder = TypeSpec
				.interfaceBuilder(actionsDefinitionInterfaceName)
				.addModifiers(PUBLIC);
		for (TypeMirror actionsClass : structure.getAllActionsClasses()) {
			String methodName = decapitalize(TypeResolver.getSimpleName(actionsClass.toString()));
			ClassName actionsType = TypeResolver.getClassName(actionsClass);
			MethodSpec actionsMethod = MethodSpec
					.methodBuilder(methodName)
					.addModifiers(PUBLIC, Modifier.ABSTRACT)
					.returns(actionsType)
					.build();
			typeBuilder.addMethod(actionsMethod);
		}
		createJavaFile(packageName, typeBuilder.build());
		ClassName actionDefinitionsAsSuperInterface = ClassName.get(packageName, actionsDefinitionInterfaceName);
		for (String prefix : DEVICE_PREFIXES) {
			typeBuilder = TypeSpec
					.classBuilder(prefix + actionsDefinitionInterfaceName)
					.addModifiers(PUBLIC)
					.addSuperinterface(actionDefinitionsAsSuperInterface);
			for (TypeMirror actionsClass : structure.getAllActionsClasses()) {
				String methodName = decapitalize(TypeResolver.getSimpleName(actionsClass.toString()));
				ClassName baseActionsType = TypeResolver.getClassName(actionsClass);
				ClassName deviceSpecificActionsType = TypeResolver.getClassNameWithPrefix(actionsClass, prefix);
				MethodSpec actionsMethod = MethodSpec
						.methodBuilder(methodName)
						.addAnnotation(Override.class)
						.addModifiers(PUBLIC)
						.addStatement("return new $T()", deviceSpecificActionsType)
						.returns(baseActionsType)
						.build();
				typeBuilder.addMethod(actionsMethod);
			}
			createJavaFile(packageName, typeBuilder.build());
		}
	}

	private void generateActionsModule() {
		TypeSpec.Builder classBuilder = TypeSpec
				.classBuilder(ACTIONS_MODULE_CLASS_NAME)
				.addModifiers(PUBLIC)
				.addAnnotation(MODULE_ANNOTATION_NAME)
				.addSuperinterface(ACTION_DEFINITIONS_CLASS_NAME);
		String actionsDefinitionParam = decapitalize(ACTION_DEFINITIONS_CLASS_NAME.simpleName());
		FieldSpec actionsDefinition = FieldSpec
				.builder(ACTION_DEFINITIONS_CLASS_NAME, actionsDefinitionParam, Modifier.PRIVATE, Modifier.FINAL)
				.build();
		classBuilder.addField(actionsDefinition);

		MethodSpec constructor = MethodSpec
				.constructorBuilder()
				.addParameter(ACTION_DEFINITIONS_CLASS_NAME, actionsDefinitionParam)
				.addStatement("this.$N=$N", actionsDefinitionParam, actionsDefinitionParam)
				.addModifiers(PUBLIC)
				.build();
		classBuilder.addMethod(constructor);

		for (TypeMirror actionsClass : structure.getAllActionsClasses()) {
			ClassName returnType = TypeResolver.getClassName(actionsClass);
			String methodName = decapitalize(returnType.simpleName());
			MethodSpec providingMethod = MethodSpec.methodBuilder(methodName)
					.addAnnotation(Override.class)
					.addAnnotation(PROVIDES_ANNOTATION_NAME)
					.addModifiers(PUBLIC)
					.addStatement("return $N.$N()", actionsDefinitionParam, methodName)
					.returns(returnType)
					.build();
			classBuilder.addMethod(providingMethod);
		}
		createJavaFile(DAGGER_PACKAGE, classBuilder.build());
	}

	private void generateStepsModule() {
		TypeSpec.Builder classBuilder = TypeSpec
				.classBuilder(STEPS_MODULE_CLASS_NAME)
				.addModifiers(PUBLIC)
				.addAnnotation(MODULE_ANNOTATION_NAME);
		for (TypeMirror stepsClass : structure.getAllStepsClasses()) {
			ClassName returnType = TypeResolver.getClassName(stepsClass);
			String methodName = decapitalize(returnType.simpleName());
			List<ParameterSpec> paramsList = new ArrayList<>();
			List<String> constructorArguments = new ArrayList<>();
			for (TypeMirror actionsParamClass : structure.getActionsClasses(stepsClass)) {
				ClassName actionsParamType = TypeResolver.getClassName(actionsParamClass);
				String actionsParamName = decapitalize(actionsParamType.simpleName());
				constructorArguments.add(actionsParamName);

				ParameterSpec parameterSpec = ParameterSpec.builder(actionsParamType, actionsParamName).build();
				paramsList.add(parameterSpec);
			}
			String constructorArgumentsStr = String.join(", ", constructorArguments);
			MethodSpec method = MethodSpec.methodBuilder(methodName)
					.addAnnotation(PROVIDES_ANNOTATION_NAME)
					.addModifiers(PUBLIC)
					.addParameters(paramsList)
					.addStatement("return new $N($N)", returnType.simpleName(), constructorArgumentsStr)
					.returns(returnType)
					.build();
			classBuilder.addMethod(method);
		}
		createJavaFile(DAGGER_PACKAGE, classBuilder.build());
	}

	private void generateStepsComponent() {
		final String className = "StepsComponent";
		TypeSpec.Builder classBuilder = TypeSpec
				.interfaceBuilder(className)
				.addModifiers(PUBLIC);
		ClassName componentAnnotationName = ClassName.get(DAGGER_PACKAGE, "Component");
		CodeBlock annotationValue = CodeBlock.builder().add("{$N.class, $N.class}",
				ACTIONS_MODULE_CLASS_NAME, STEPS_MODULE_CLASS_NAME).build();
		AnnotationSpec componentAnnotation = AnnotationSpec.builder(componentAnnotationName)
				.addMember("modules", annotationValue).build();
		classBuilder.addAnnotation(componentAnnotation);
		for (TypeMirror stepsClass : structure.getAllStepsClasses()) {
			ClassName returnType = TypeResolver.getClassName(stepsClass);
			String methodName = decapitalize(returnType.simpleName());
			MethodSpec method = MethodSpec.methodBuilder(methodName)
					.addModifiers(PUBLIC, ABSTRACT)
					.returns(returnType).build();
			classBuilder.addMethod(method);
		}
		createJavaFile(DAGGER_PACKAGE, classBuilder.build());
	}

	private void createJavaFile(String packageName, TypeSpec typeSpec) {
		final JavaFile javaFile = JavaFile.builder(packageName, typeSpec)
				.build();
		try {
			javaFile.writeTo(filer);
		} catch (IOException e) {
			messager.printMessage(ERROR,
					"Failed to create source file [" + typeSpec.name + "]:\n"
							+ e.getMessage());
		}

	}

}
