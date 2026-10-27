package com.wonnabe.classifier.classification;

import java.io.Serializable;

import com.wonnabe.classifier.domain.Category;
import com.wonnabe.classifier.domain.ClassificationMethod;

public record ClassificationResult(Category category, ClassificationMethod method) implements Serializable {

	static final ClassificationResult UNCLASSIFIED = new ClassificationResult(null, ClassificationMethod.UNCLASSIFIED);
}
