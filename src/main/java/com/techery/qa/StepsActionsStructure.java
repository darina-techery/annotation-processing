package com.techery.qa;

import javax.lang.model.type.TypeMirror;
import java.util.*;

import static com.techery.qa.Validator.JOIN_BY_COMMA;

public class StepsActionsStructure {
	private HashMap<TypeMirror, List<TypeMirror>> map = new HashMap<>();
	public void add(TypeMirror stepsClass, TypeMirror actionsClass) {
		if (!map.containsKey(stepsClass)) {
			map.put(stepsClass, new ArrayList<>());
		}
		map.get(stepsClass).add(actionsClass);
	}

	public List<TypeMirror> getActionsClasses(TypeMirror stepsClass) {
		return map.get(stepsClass);
	}

	public Set<TypeMirror> getAllActionsClasses() {
		Set<TypeMirror> actionsClasses = new HashSet<>();
		map.forEach((k,v)-> actionsClasses.addAll(v));
		return actionsClasses;
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public String toString() {
		String verboseContent = "";
		for (TypeMirror key : map.keySet()) {
			verboseContent += String.format("%s: [%s]\n",
					key, map.get(key).stream().map(TypeMirror::toString).collect(JOIN_BY_COMMA));
		}
		return verboseContent;
	}

	public Set<TypeMirror> getAllStepsClasses() {
		return map.keySet();
	}
}
