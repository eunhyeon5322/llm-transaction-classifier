package com.wonnabe.classifier.classification;

import com.wonnabe.classifier.domain.Category;
import com.wonnabe.classifier.domain.ClassificationMethod;

public record ClassificationResult(Category category, ClassificationMethod method) {

	static final ClassificationResult UNCLASSIFIED = new ClassificationResult(null, ClassificationMethod.UNCLASSIFIED);
}
