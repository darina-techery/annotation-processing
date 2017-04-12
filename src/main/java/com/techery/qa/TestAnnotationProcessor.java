package com.techery.qa;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.Set;

@SupportedAnnotationTypes({
		"utils.annotations.UseActions"
})
public class TestAnnotationProcessor extends AbstractProcessor {

	private Validator validator;
	private Generator generator;
	private StepsActionsStructure structure;


	public TestAnnotationProcessor() {
	}

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		validator = new Validator(processingEnv);
		generator = new Generator(processingEnv);

	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		structure = new StepsActionsStructure();
		for (TypeElement annotationType : annotations) {
			for (Element elem : roundEnv.getElementsAnnotatedWith(annotationType)) {
				ExecutableElement constructor = ((ExecutableElement) elem);
				TypeMirror stepsClass = constructor.getEnclosingElement().asType();
				constructor.getParameters().forEach(actionsParam -> structure.add(stepsClass, actionsParam.asType()));
			}
		}
		if (!structure.isEmpty()) {
			validator.validateActionsClasses(structure.getAllActionsClasses());
			generator.generate(structure);
		}
		return false;
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latestSupported();
	}

}

